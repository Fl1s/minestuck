package com.mraof.minestuck.entity;

import com.mraof.minestuck.entity.carapacian.CarapacianEntity;
import com.mraof.minestuck.player.PlayerData;
import com.mraof.minestuck.player.dreamself.DreamselfData;
import com.mraof.minestuck.player.dreamself.DreamselfHandler;
import com.mraof.minestuck.util.MSAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents the self of a player that is currently asleep.
 */
public class SleepingSelfEntity extends Mob
{
	private static final EntityDataAccessor<Optional<UUID>> OWNER_ID = SynchedEntityData.defineId(SleepingSelfEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	private static final EntityDataAccessor<Boolean> DREAM_SELF = SynchedEntityData.defineId(SleepingSelfEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Direction> LIE_DIRECTION = SynchedEntityData.defineId(SleepingSelfEntity.class, EntityDataSerializers.DIRECTION);
	private static final EntityDimensions LYING_DIMENSIONS = EntityDimensions.scalable(0.9F, 0.4F);
	private static final EntityDimensions STANDING_DIMENSIONS = EntityType.PLAYER.getDimensions();
	private static final double SUPPORT_DEPTH = 0.2;
	private static final double MOVE_LIMIT_SQR = 0.5 * 0.5;
	private static final int UNSUPPORTED_TICKS_TO_WAKE = 2;
	private static final int FAILED_WAKE_COOLDOWN = 40;
	private static final int AGGRO_INTERVAL = 10;
	private static final double AGGRO_MAX_RANGE = 32;
	private static final double DEFAULT_FOLLOW_RANGE = 16;
	
	private boolean settled;
	@Nullable
	private Vec3 origin;
	private int unsupportedTicks;
	private int wakeCooldown;
	
	public SleepingSelfEntity(EntityType<? extends SleepingSelfEntity> type, Level level)
	{
		super(type, level);
		this.setNoAi(true);
		this.setPersistenceRequired();
		this.setPose(Pose.SLEEPING);
	}
	
	public static SleepingSelfEntity create(ServerLevel level, ServerPlayer owner, boolean dreamSelf, double x, double y, double z, float yRot, @Nullable BlockPos bedPos)
	{
		return create(level, owner.getUUID(), owner.getGameProfile().getName(), dreamSelf, x, y, z, yRot, bedPos);
	}
	
	public static SleepingSelfEntity create(ServerLevel level, UUID ownerId, String ownerName, boolean dreamSelf, double x, double y, double z, float yRot, @Nullable BlockPos bedPos)
	{
		SleepingSelfEntity entity = new SleepingSelfEntity(MSEntityTypes.SLEEPING_SELF.get(), level);
		entity.entityData.set(OWNER_ID, Optional.of(ownerId));
		entity.entityData.set(DREAM_SELF, dreamSelf);
		entity.setCustomName(Component.literal(ownerName));
		entity.setCustomNameVisible(true);
		entity.moveTo(x, y, z, yRot, 0);
		entity.setYHeadRot(yRot);
		entity.yBodyRot = yRot;
		entity.yBodyRotO = yRot;
		
		Direction lieDirection = Direction.fromYRot(yRot);
		if(bedPos != null)
		{
			BlockState bedState = level.getBlockState(bedPos);
			if(bedState.getBlock() instanceof BedBlock) lieDirection = bedState.getValue(BedBlock.FACING);
		}
		entity.entityData.set(LIE_DIRECTION, lieDirection);
		return entity;
	}
	
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		super.defineSynchedData(builder);
		builder.define(OWNER_ID, Optional.empty());
		builder.define(DREAM_SELF, false);
		builder.define(LIE_DIRECTION, Direction.NORTH);
	}
	
	@Nullable
	public UUID getOwnerId()
	{
		return this.entityData.get(OWNER_ID).orElse(null);
	}
	
	@Nullable
	private ServerPlayer getOwner()
	{
		UUID ownerId = getOwnerId();
		if(ownerId == null || this.level().getServer() == null) return null;
		return this.level().getServer().getPlayerList().getPlayer(ownerId);
	}
	
	public boolean isDreamSelf()
	{
		return this.entityData.get(DREAM_SELF);
	}
	
	@Nullable
	@Override
	public Direction getBedOrientation()
	{
		return this.entityData.get(LIE_DIRECTION);
	}
	
	@Override
	protected AABB makeBoundingBox()
	{
		return LYING_DIMENSIONS.makeBoundingBox(this.position());
	}
	
	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose)
	{
		return pose == Pose.SLEEPING ? LYING_DIMENSIONS : STANDING_DIMENSIONS;
	}
	
	@Override
	public float getPickRadius()
	{
		return 0.6F;
	}
	
	@Override
	public AABB getBoundingBoxForCulling()
	{
		return this.getBoundingBox().inflate(2.0, 0.6, 2.0);
	}
	
	@Override
	public boolean shouldRenderAtSqrDistance(double distance)
	{
		return distance < 128.0 * 128.0;
	}
	
	@Override
	public void tick()
	{
		super.tick();
		if(this.getPose() != Pose.SLEEPING) this.setPose(Pose.SLEEPING);
		
		if(this.level().isClientSide || this.isRemoved()) return;
		
		if(this.tickCount % 20 == 0 && !this.isStillRepresentingOwner())
		{
			this.discard();
			return;
		}
		
		this.tickPhysics();
	}
	
	private void tickPhysics()
	{
		if(this.wakeCooldown > 0) this.wakeCooldown--;
		
		boolean supported = this.isSupported();
		if(!this.settled)
		{
			if(!supported)
			{
				this.move(MoverType.SELF, new Vec3(0, -0.5, 0));
			} else
			{
				this.settled = true;
				this.origin = this.position();
				
				ServerPlayer owner = this.getOwner();
				if(owner != null) DreamselfHandler.syncStoredPosition(owner, this);
			}
			return;
		}
		
		ServerPlayer owner = this.getOwner();
		if(owner == null)
		{
			this.origin = this.position();
			this.unsupportedTicks = 0;
			return;
		}
		
		if(this.origin == null) this.origin = this.position();
		
		if(this.tickCount % AGGRO_INTERVAL == 0) this.attractHostileMobs(owner);
		
		this.unsupportedTicks = supported ? 0 : this.unsupportedTicks + 1;
		boolean moved = this.position().distanceToSqr(this.origin) > MOVE_LIMIT_SQR;
		
		if((moved || this.unsupportedTicks >= UNSUPPORTED_TICKS_TO_WAKE) && this.wakeCooldown == 0)
		{
			if(!DreamselfHandler.wakeUpFromDisturbance(owner, this)) this.wakeCooldown = FAILED_WAKE_COOLDOWN;
		}
	}
	
	private void attractHostileMobs(ServerPlayer owner)
	{
		if(owner.isCreative() || owner.isSpectator()) return;
		
		AABB searchArea = this.getBoundingBox().inflate(AGGRO_MAX_RANGE);
		for(Mob mob : this.level().getEntitiesOfClass(Mob.class, searchArea, mob -> isPotentialAttacker(mob)))
		{
			AttributeInstance followRange = mob.getAttribute(Attributes.FOLLOW_RANGE);
			double range = Math.min(AGGRO_MAX_RANGE, followRange != null ? followRange.getValue() : DEFAULT_FOLLOW_RANGE);
			
			if(mob.distanceToSqr(this) <= range * range && mob.canAttack(this) && mob.hasLineOfSight(this))
				mob.setTarget(this);
		}
	}
	
	private boolean isPotentialAttacker(Mob mob)
	{
		if(mob == this || !(mob instanceof Enemy) || mob instanceof CarapacianEntity || !mob.isAlive()) return false;
		
		LivingEntity currentTarget = mob.getTarget();
		return currentTarget == null || !currentTarget.isAlive();
	}
	
	private boolean isSupported()
	{
		AABB box = this.getBoundingBox();
		if(!this.level().hasChunksAt(BlockPos.containing(box.minX, box.minY - 1, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ)))
			return true;
		return !this.level().noCollision(this, box.move(0, -SUPPORT_DEPTH, 0));
	}
	
	private boolean isStillRepresentingOwner()
	{
		ServerPlayer owner = getOwner();
		if(owner == null) return true;
		
		DreamselfData data = PlayerData.get(owner).map(playerData -> playerData.getData(MSAttachments.DREAMSELF)).orElse(null);
		return data != null && this.getUUID().equals(data.sleepingEntity());
	}
	
	@Override
	public boolean hurt(DamageSource source, float amount)
	{
		if(this.level().isClientSide) return true;
		
		ServerPlayer owner = getOwner();
		if(owner == null) return false;
		
		boolean harmless = source.getEntity() == owner || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
		return DreamselfHandler.wakeUpFromAttack(owner, this, source, harmless ? 0 : amount);
	}
	
	@Override
	public boolean isPushable()
	{
		return false;
	}
	
	@Override
	protected void doPush(Entity entity)
	{
	}
	
	@Override
	public void push(double x, double y, double z)
	{
	}
	
	@Override
	public boolean isPushedByFluid()
	{
		return false;
	}
	
	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer)
	{
		return false;
	}
	
	@Override
	public boolean canBeLeashed()
	{
		return false;
	}
	
	@Override
	public boolean shouldShowName()
	{
		return this.isCustomNameVisible();
	}
	
	@Override
	public void addAdditionalSaveData(CompoundTag compound)
	{
		super.addAdditionalSaveData(compound);
		UUID ownerId = getOwnerId();
		if(ownerId != null) compound.putUUID("owner", ownerId);
		compound.putBoolean("dream_self", isDreamSelf());
		compound.putString("lie_direction", this.entityData.get(LIE_DIRECTION).getSerializedName());
		compound.putBoolean("settled", this.settled);
	}
	
	@Override
	public void readAdditionalSaveData(CompoundTag compound)
	{
		super.readAdditionalSaveData(compound);
		if(compound.hasUUID("owner")) this.entityData.set(OWNER_ID, Optional.of(compound.getUUID("owner")));
		this.entityData.set(DREAM_SELF, compound.getBoolean("dream_self"));
		Direction lieDirection = Direction.byName(compound.getString("lie_direction"));
		this.entityData.set(LIE_DIRECTION, lieDirection != null && lieDirection.getAxis().isHorizontal() ? lieDirection : Direction.NORTH);
		this.settled = compound.getBoolean("settled");
		this.setPose(Pose.SLEEPING);
	}
}