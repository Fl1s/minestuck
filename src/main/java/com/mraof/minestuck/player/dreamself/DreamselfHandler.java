package com.mraof.minestuck.player.dreamself;

import com.mraof.minestuck.Minestuck;
import com.mraof.minestuck.computer.editmode.ServerEditHandler;
import com.mraof.minestuck.entity.SleepingSelfEntity;
import com.mraof.minestuck.event.OnEntryEvent;
import com.mraof.minestuck.player.IdentifierHandler;
import com.mraof.minestuck.player.PlayerData;
import com.mraof.minestuck.network.DreamselfFadePacket;
import com.mraof.minestuck.player.PlayerIdentifier;
import com.mraof.minestuck.skaianet.SburbPlayerData;
import com.mraof.minestuck.util.MSAttachments;
import com.mraof.minestuck.util.MSSoundEvents;
import com.mraof.minestuck.util.Teleport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the dreamself system.
 * <p>
 * Going to sleep in a bed swaps which of the two the player controls.
 * The state of the sleeping self is kept in {@link DreamselfData}, and swapped in and out of the player entity.
 */
@EventBusSubscriber(modid = Minestuck.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class DreamselfHandler
{
	private static final Logger LOGGER = LogManager.getLogger();
	
	public static final String WAKE_UP_BODY = "minestuck.dreamself.wake_up_body";
	public static final String WAKE_UP_DREAM = "minestuck.dreamself.wake_up_dream";
	public static final String DREAM_DEATH = "minestuck.dreamself.dream_death";
	public static final String NOT_AVAILABLE = "minestuck.dreamself.not_available";
	public static final String EDITMODE = "minestuck.dreamself.editmode";
	public static final String NOT_DREAMING = "minestuck.dreamself.not_dreaming";
	private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	
	public static final String DREAMSELF_LOST = "minestuck.dreamself.lost";
	public static final String FAILED = "minestuck.dreamself.failed";
	
	@Nullable
	public static DreamselfData getData(ServerPlayer player)
	{
		return PlayerData.get(player).map(playerData -> playerData.getData(MSAttachments.DREAMSELF)).orElse(null);
	}
	
	public static DreamselfData getData(PlayerIdentifier player, MinecraftServer server)
	{
		return PlayerData.get(player, server).getData(MSAttachments.DREAMSELF);
	}
	
	@SubscribeEvent
	private static void onEntry(OnEntryEvent event)
	{
		ServerPlayer player = event.getPlayer().getPlayer(event.getMcServer());
		if(player != null) ensureInitialized(player);
	}
	
	//also covers players that entered before the dreamself system existed
	@SubscribeEvent
	private static void onLogin(PlayerEvent.PlayerLoggedInEvent event)
	{
		if(!(event.getEntity() instanceof ServerPlayer player)) return;
		ensureInitialized(player);
	}
	
	//gives the player a lunar sway and a dreamself if they dont have those yet
	public static boolean ensureInitialized(ServerPlayer player)
	{
		DreamselfData data = getData(player);
		if(data == null) return false;
		if(data.isDreamselfDead()) return false;
		if(data.isReady()) return true;
		
		//only players that have entered the medium have a dreamself
		PlayerIdentifier identifier = IdentifierHandler.encode(player);
		if(identifier == null || !SburbPlayerData.get(identifier, player.server).hasEntered()) return false;
		
		if(data.sway() == null) data.setSway(LunarSway.random(player.getRandom()));
		
		return createDreamself(player, data);
	}
	
	private static boolean createDreamself(ServerPlayer player, DreamselfData data)
	{
		LunarSway sway = data.sway();
		ServerLevel level = player.server.getLevel(sway.getDreamDimension());
		if(level == null)
		{
			LOGGER.warn("Unable to create dreamself for {}, as the dimension {} is missing.", player.getGameProfile().getName(), sway.getDreamDimension().location());
			return false;
		}
		
		RandomSource random = level.getRandom();
		double angle = random.nextDouble() * Math.PI * 2;
		double distance = 3 + random.nextDouble() * 5;
		int x = Mth.floor(Math.cos(angle) * distance), z = Mth.floor(Math.sin(angle) * distance);
		int y = level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z)).getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 1;
		float yaw = random.nextFloat() * 360F;
		
		Inventory inventory = new Inventory(player);
		for(EquipmentSlot slot : ARMOR_SLOTS)
			inventory.armor.set(slot.getIndex(), sway.createRobe(slot));
		
		SelfState state = new SelfState(level.dimension(), x + 0.5, y, z + 0.5, yaw, 0, player.getMaxHealth(), SelfState.freshFood(), inventory.save(new ListTag()), 0, 0, 0, null);
		
		SleepingSelfEntity entity = createEntity(level, player, true, state);
		data.set(false, state, entity.getUUID());
		level.addFreshEntity(entity);
		return true;
	}
	
	private static SleepingSelfEntity createEntity(ServerLevel level, ServerPlayer owner, boolean dreamSelf, SelfState state)
	{
		SleepingSelfEntity entity = SleepingSelfEntity.create(level, owner, dreamSelf, state.x, state.y, state.z, state.yRot, state.bedPos);
		
		Inventory shownInventory = new Inventory(owner);
		shownInventory.load(state.inventory);
		for(EquipmentSlot slot : ARMOR_SLOTS)
			entity.setItemSlot(slot, shownInventory.armor.get(slot.getIndex()).copy());
		return entity;
	}
	
	private static final int FADE_OUT = 25, FADE_HOLD = 15, FADE_IN = 35;
	private static final int SWAP_DELAY = FADE_OUT + 6;
	private static final int ABRUPT_FADE_IN = 20;
	private static final int DISTURBANCE_COOLDOWN = 60;
	private static final int DELAYED_DAMAGE_TIMEOUT = 40;
	
	private static final Map<UUID, PendingSwap> PENDING_SWAPS = new HashMap<>();
	private static final Map<UUID, Integer> LAST_SWAP = new HashMap<>();
	private static final List<DelayedDamage> DELAYED_DAMAGE = new ArrayList<>();
	
	private static final class PendingSwap
	{
		@Nullable
		final BlockPos bedPos;
		int ticksLeft = SWAP_DELAY;
		
		PendingSwap(@Nullable BlockPos bedPos)
		{
			this.bedPos = bedPos;
		}
	}
	
	private static final class DelayedDamage
	{
		final UUID player;
		final DamageSource source;
		final float amount;
		int waited;
		
		DelayedDamage(UUID player, DamageSource source, float amount)
		{
			this.player = player;
			this.source = source;
			this.amount = amount;
		}
	}
	
	/**
	 * Starts the animation of the player falling asleep in the currently controlled self and waking up in the other self.
	 *
	 * @param bedPos the bed that the player went to sleep in
	 * @return true if the swap was started
	 */
	public static boolean requestSwap(ServerPlayer player, @Nullable BlockPos bedPos)
	{
		if(PENDING_SWAPS.containsKey(player.getUUID())) return false;
		
		DreamselfData data = checkSwapAllowed(player, true);
		if(data == null) return false;
		
		PENDING_SWAPS.put(player.getUUID(), new PendingSwap(bedPos));
		PacketDistributor.sendToPlayer(player, new DreamselfFadePacket(FADE_OUT, FADE_HOLD, FADE_IN, swapSound(data.sway()).getLocation()));
		return true;
	}
	
	/**
	 * Used by the return-to-body key
	 */
	public static void requestReturnToBody(ServerPlayer player)
	{
		DreamselfData data = getData(player);
		if(data == null || !data.isReady() && !data.isDreamselfDead())
		{
			player.displayClientMessage(Component.translatable(NOT_AVAILABLE), true);
			return;
		}
		if(data.isDreamselfDead())
		{
			player.displayClientMessage(Component.translatable(DREAMSELF_LOST), true);
			return;
		}
		if(!data.isDreaming())
		{
			player.displayClientMessage(Component.translatable(NOT_DREAMING), true);
			return;
		}
		
		requestSwap(player, null);
	}
	
	private static SoundEvent swapSound(@Nullable LunarSway sway)
	{
		return (sway == LunarSway.DERSE ? MSSoundEvents.DREAMSELF_SWAP_DERSE : MSSoundEvents.DREAMSELF_SWAP_PROSPIT).get();
	}
	
	@Nullable
	private static DreamselfData checkSwapAllowed(ServerPlayer player, boolean notify)
	{
		DreamselfData data = getData(player);
		if(data != null && data.isDreamselfDead())
		{
			if(notify) player.sendSystemMessage(Component.translatable(DREAMSELF_LOST));
			return null;
		}
		if(data == null || !ensureInitialized(player))
		{
			if(notify) player.sendSystemMessage(Component.translatable(NOT_AVAILABLE));
			return null;
		}
		if(ServerEditHandler.isInEditmode(player))
		{
			if(notify) player.sendSystemMessage(Component.translatable(EDITMODE));
			return null;
		}
		if(!player.isAlive()) return null;
		return data;
	}
	
	/**
	 * Makes the player go to sleep in the currently controlled self, and take control of the other self right now.
	 *
	 * @param abrupt true if the player is woken up suddenly, which gives a shorter animation and a different sound
	 * @param notify true if the player should be told when the swap isn't possible
	 * @return true if the swap was successful
	 */
	private static boolean performSwap(ServerPlayer player, @Nullable BlockPos bedPos, boolean abrupt, boolean notify)
	{
		PENDING_SWAPS.remove(player.getUUID());
		
		DreamselfData data = checkSwapAllowed(player, notify);
		if(data == null) return false;
		
		SelfState target = data.sleepingSelf();
		ServerLevel targetLevel = player.server.getLevel(target.dimension);
		if(targetLevel == null)
		{
			LOGGER.warn("Unable to swap selves for {}, as the dimension {} is missing.", player.getGameProfile().getName(), target.dimension.location());
			if(notify) player.sendSystemMessage(Component.translatable(FAILED));
			return false;
		}
		
		ServerLevel currentLevel = player.serverLevel();
		boolean wasDreaming = data.isDreaming();
		UUID oldEntityId = data.sleepingEntity();
		
		Entity oldEntity = oldEntityId != null ? targetLevel.getEntity(oldEntityId) : null;
		if(oldEntity != null)
			target = target.withPosition(oldEntity.getX(), oldEntity.getY(), oldEntity.getZ(), oldEntity.getYRot());
		
		player.closeContainer();
		
		SelfState current = SelfState.capture(player, bedPos);
		SleepingSelfEntity body = createEntity(currentLevel, player, wasDreaming, current);
		
		if(abrupt)
			PacketDistributor.sendToPlayer(player, new DreamselfFadePacket(0, 0, ABRUPT_FADE_IN, MSSoundEvents.DREAMSELF_SWAP_HIT.get().getLocation()));
		
		if(Teleport.teleportEntity(player, targetLevel, target.x, target.y, target.z, target.yRot, target.xRot) == null)
		{
			if(notify) player.sendSystemMessage(Component.translatable(FAILED));
			return false;
		}
		
		applyState(player, target);
		
		data.set(!wasDreaming, current, body.getUUID());
		currentLevel.addFreshEntity(body);
		LAST_SWAP.put(player.getUUID(), player.server.getTickCount());
		
		if(oldEntity != null) oldEntity.discard();
		
		player.displayClientMessage(Component.translatable(data.isDreaming() ? WAKE_UP_DREAM : WAKE_UP_BODY, data.sway().getDisplayName()), true);
		return true;
	}
	
	private static void applyState(ServerPlayer player, SelfState state)
	{
		Inventory inventory = player.getInventory();
		inventory.clearContent();
		inventory.load(state.inventory);
		
		player.setHealth(Mth.clamp(state.health, 1, player.getMaxHealth()));
		player.getFoodData().readAdditionalSaveData(state.food);
		
		player.experienceLevel = state.xpLevel;
		player.totalExperience = state.xpTotal;
		player.experienceProgress = state.xpProgress;
		
		player.fallDistance = 0;
		player.setDeltaMovement(Vec3.ZERO);
		player.resetSentInfo();
	}
	
	/**
	 * Called when a sleeping self takes damage
	 *
	 * @return true if the owner was woken up
	 */
	public static boolean wakeUpFromAttack(ServerPlayer owner, SleepingSelfEntity attackedEntity, DamageSource source, float damage)
	{
		DreamselfData data = getData(owner);
		if(data == null || !attackedEntity.getUUID().equals(data.sleepingEntity())) return false;
		
		if(!performSwap(owner, null, true, false)) return false;
		
		if(damage > 0) DELAYED_DAMAGE.add(new DelayedDamage(owner.getUUID(), source, damage));
		return true;
	}
	
	/**
	 * Called when a sleeping self has lost its support or been moved
	 *
	 * @return true if the owner was woken up
	 */
	public static boolean wakeUpFromDisturbance(ServerPlayer owner, SleepingSelfEntity entity)
	{
		Integer lastSwap = LAST_SWAP.get(owner.getUUID());
		if(lastSwap != null && owner.server.getTickCount() - lastSwap < DISTURBANCE_COOLDOWN) return false;
		
		return wakeUpFromAttack(owner, entity, owner.damageSources().generic(), 0);
	}
	
	/**
	 * Keeps the stored state in line with the entity after it has landed in its final place.
	 */
	public static void syncStoredPosition(ServerPlayer owner, SleepingSelfEntity entity)
	{
		DreamselfData data = getData(owner);
		if(data == null || !entity.getUUID().equals(data.sleepingEntity())) return;
		
		SelfState state = data.sleepingSelf();
		if(state == null || !state.dimension.equals(entity.level().dimension())) return;
		if(entity.distanceToSqr(state.x, state.y, state.z) > 1.0E-4)
			data.setSleepingSelf(state.withPosition(entity.getX(), entity.getY(), entity.getZ(), state.yRot));
	}
	
	@SubscribeEvent
	private static void onServerTick(ServerTickEvent.Post event)
	{
		MinecraftServer server = event.getServer();
		
		if(!PENDING_SWAPS.isEmpty())
		{
			List<Map.Entry<UUID, PendingSwap>> due = new ArrayList<>();
			for(Map.Entry<UUID, PendingSwap> entry : PENDING_SWAPS.entrySet())
			{
				if(--entry.getValue().ticksLeft <= 0) due.add(entry);
			}
			
			for(Map.Entry<UUID, PendingSwap> entry : due)
			{
				PENDING_SWAPS.remove(entry.getKey());
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if(player != null) performSwap(player, entry.getValue().bedPos, false, true);
			}
		}
		
		if(!DELAYED_DAMAGE.isEmpty())
		{
			//A player that has just been teleported can't be damaged until the client has caught up
			DELAYED_DAMAGE.removeIf(damage -> {
				ServerPlayer player = server.getPlayerList().getPlayer(damage.player);
				if(player == null || !player.isAlive()) return true;
				if(player.isChangingDimension && ++damage.waited < DELAYED_DAMAGE_TIMEOUT) return false;
				player.hurt(damage.source, damage.amount);
				return true;
			});
		}
	}
	
	@SubscribeEvent
	private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event)
	{
		UUID id = event.getEntity().getUUID();
		PENDING_SWAPS.remove(id);
		LAST_SWAP.remove(id);
		DELAYED_DAMAGE.removeIf(damage -> damage.player.equals(id));
	}
	
	@SubscribeEvent
	private static void onServerStopped(ServerStoppedEvent event)
	{
		PENDING_SWAPS.clear();
		LAST_SWAP.clear();
		DELAYED_DAMAGE.clear();
	}
	
	@SubscribeEvent
	private static void onBedUse(PlayerInteractEvent.RightClickBlock event)
	{
		if(!(event.getEntity() instanceof ServerPlayer player) || event.getHand() != InteractionHand.MAIN_HAND) return;
		
		Level level = event.getLevel();
		BlockState state = level.getBlockState(event.getPos());
		if(!(state.getBlock() instanceof BedBlock) || !level.dimensionType().bedWorks()) return;
		
		if(player.isSecondaryUseActive() && (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()))
			return;
		
		DreamselfData data = getData(player);
		if(data == null || !ensureInitialized(player) || ServerEditHandler.isInEditmode(player)) return;
		
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.SUCCESS);
		
		BlockPos bedPos = event.getPos();
		if(state.getValue(BedBlock.PART) == BedPart.FOOT) bedPos = bedPos.relative(state.getValue(BedBlock.FACING));
		
		boolean wasDreaming = data.isDreaming();
		if(requestSwap(player, bedPos) && !wasDreaming)
			player.setRespawnPosition(level.dimension(), event.getPos(), player.getYRot(), false, true);
	}
	
	/**
	 * The dreamself only has one life. If it dies, it is gone for good!!!
	 * The player wakes up in their waking body, and will not get another dreamself.
	 */
	// TODO: make this (^^^) optional in config
	@SubscribeEvent(priority = EventPriority.HIGH)
	private static void onDeath(LivingDeathEvent event)
	{
		if(!(event.getEntity() instanceof ServerPlayer player)) return;
		DreamselfData data = getData(player);
		if(data == null || !data.isDreaming() || !data.isReady() || ServerEditHandler.isInEditmode(player)) return;
		
		if(killDreamself(player, data)) event.setCanceled(true);
	}
	
	/**
	 * Permanently ends the dreamself that the player is currently controlling, and returns the player to the waking body
	 *
	 * @return true if the player was moved to the waking body
	 */
	private static boolean killDreamself(ServerPlayer player, DreamselfData data)
	{
		SelfState body = data.sleepingSelf();
		ServerLevel bodyLevel = player.server.getLevel(body.dimension);
		if(bodyLevel == null)
		{
			LOGGER.warn("Unable to return {} to their body, as the dimension {} is missing.", player.getGameProfile().getName(), body.dimension.location());
			return false;
		}
		
		UUID bodyEntityId = data.sleepingEntity();
		ServerLevel dreamLevel = player.serverLevel();
		
		player.closeContainer();
		
		if(!dreamLevel.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) player.getInventory().dropAll();
		
		if(Teleport.teleportEntity(player, bodyLevel, body.x, body.y, body.z, body.yRot, body.xRot) == null)
		{
			LOGGER.warn("Unable to teleport {} to their body.", player.getGameProfile().getName());
			return false;
		}
		
		applyState(player, body);
		data.killDreamself();
		PENDING_SWAPS.remove(player.getUUID());
		
		if(bodyEntityId != null)
		{
			Entity oldEntity = bodyLevel.getEntity(bodyEntityId);
			if(oldEntity != null) oldEntity.discard();
		}
		
		player.sendSystemMessage(Component.translatable(DREAM_DEATH));
		return true;
	}
}
