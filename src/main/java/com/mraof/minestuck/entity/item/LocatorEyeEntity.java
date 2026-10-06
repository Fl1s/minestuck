package com.mraof.minestuck.entity.item;

import com.mraof.minestuck.item.MSItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

public class LocatorEyeEntity extends net.minecraft.world.entity.Entity
{
	private static final EntityDataAccessor<Boolean> SHATTER = SynchedEntityData.defineId(LocatorEyeEntity.class, EntityDataSerializers.BOOLEAN);
	
	private BlockPos target;
	private int despawnTimer;
	private boolean shatterOrDrop;
	
	public LocatorEyeEntity(EntityType<? extends LocatorEyeEntity> type, Level level)
	{
		super(type, level);
	}
	
	public LocatorEyeEntity(Level level, double x, double y, double z)
	{
		this(com.mraof.minestuck.entity.MSEntityTypes.LOCATOR_EYE.get(), level);
		this.setPos(x, y, z);
	}
	
	public void moveTowards(BlockPos pos, float dropChance)
	{
		this.target = pos;
		this.shatterOrDrop = random.nextFloat() > dropChance;
		double dx = pos.getX() - getX();
		double dz = pos.getZ() - getZ();
		double distance = Math.sqrt(dx * dx + dz * dz);
		
		if(distance > 12)
		{
			setDeltaMovement(dx / distance * 0.6, 0.3, dz / distance * 0.6);
		} else
		{
			setDeltaMovement(dx * 0.1, 0.15, dz * 0.1);
		}
	}
	
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(SHATTER, false);
	}
	
	@Override
	public void tick()
	{
		super.tick();
		
		if(level().isClientSide())
		{
			level().addParticle(ParticleTypes.PORTAL,
					getX() - getDeltaMovement().x * 0.25 + random.nextDouble() * 0.6 - 0.3,
					getY() - getDeltaMovement().y * 0.25 - 0.5,
					getZ() - getDeltaMovement().z * 0.25 + random.nextDouble() * 0.6 - 0.3,
					getDeltaMovement().x, getDeltaMovement().y, getDeltaMovement().z);
			return;
		}
		
		ServerLevel serverLevel = (ServerLevel) level();
		despawnTimer++;
		
		if(target != null)
		{
			double dx = target.getX() - getX();
			double dy = target.getY() - getY();
			double dz = target.getZ() - getZ();
			double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
			
			double speed = 0.05;
			setDeltaMovement(dx / distance * speed, dy / distance * speed, dz / distance * speed);
		}
		
		move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
		
		if(despawnTimer > 80)
		{
			if(shatterOrDrop)
			{
				serverLevel.sendParticles(ParticleTypes.ENCHANT, getX(), getY(), getZ(), 10, 0.3, 0.3, 0.3, 0.1);
			} else
			{
				ItemStack stack = new ItemStack(MSItems.DENIZEN_EYE.get());
				ItemEntity itemEntity = new ItemEntity(level(), getX(), getY(), getZ(), stack);
				serverLevel.addFreshEntity(itemEntity);
			}
			playSound(SoundEvents.ENDER_EYE_DEATH);
			discard();
		}
	}
	
	@Override
	protected void readAdditionalSaveData(CompoundTag tag)
	{
		despawnTimer = tag.getInt("DespawnTimer");
		shatterOrDrop = tag.getBoolean("Shatter");
		if(tag.contains("TargetX"))
			target = new BlockPos(tag.getInt("TargetX"), tag.getInt("TargetY"), tag.getInt("TargetZ"));
	}
	
	@Override
	protected void addAdditionalSaveData(CompoundTag tag)
	{
		tag.putInt("DespawnTimer", despawnTimer);
		tag.putBoolean("Shatter", shatterOrDrop);
		if(target != null)
		{
			tag.putInt("TargetX", target.getX());
			tag.putInt("TargetY", target.getY());
			tag.putInt("TargetZ", target.getZ());
		}
	}
	
	@Override
	public boolean isPickable()
	{
		return false;
	}
	
	@Override
	public boolean isPushable()
	{
		return false;
	}
}
