package com.mraof.minestuck.player.godtier;

import com.mraof.minestuck.Minestuck;
import com.mraof.minestuck.MinestuckConfig;
import com.mraof.minestuck.block.MSBlocks;
import com.mraof.minestuck.block.godtier.IGodTierBlock;
import com.mraof.minestuck.inventory.captchalogue.CaptchaDeckHandler;
import com.mraof.minestuck.item.MSItems;
import com.mraof.minestuck.item.components.MSItemComponents;
import com.mraof.minestuck.network.GodTierDataPacket;
import com.mraof.minestuck.network.GodTierSkillDataPacket;
import com.mraof.minestuck.player.Echeladder;
import com.mraof.minestuck.player.PlayerData;
import com.mraof.minestuck.player.godtier.skill.BadgeOverlord;
import com.mraof.minestuck.player.godtier.skill.SkillRegistry;
import com.mraof.minestuck.skaianet.SburbPlayerData;
import com.mraof.minestuck.player.EnumClass;
import com.mraof.minestuck.player.Title;
import com.mraof.minestuck.util.MSAttachments;
import com.mraof.minestuck.util.MSSoundEvents;
import com.mraof.minestuck.world.gen.structure.questbed.QuestBedPiece;
import com.mraof.minestuck.world.gen.structure.questbed.QuestBedPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@EventBusSubscriber(modid = Minestuck.MOD_ID)
public final class GodTierAscensionHandler
{
	private static final Logger LOGGER = LogManager.getLogger();
	
	public static final String GOD_TIER_REJECT_KEY = "status.god_tier_reject";
	public static final String GOD_TIER_KEY = "status.god_tier";
	public static final String GOD_TIER_MEDITATION_UNLOCK_KEY = "status.god_tier_meditation.unlock";
	public static final String OVERLORD_SKILL_LEVEL_KEY = "status.overlord_skill_level";
	public static final String OVERLORD_PVP_DEATH_KEY = "status.overlord_pvp_death";
	public static final String OVERLORD_ASCEND_KEY = "status.overlord_ascend";
	
	private GodTierAscensionHandler()
	{
	}
	
	private static final int[] PILLAR_LIGHT_TICKS = {15 * 20, 19 * 20, 23 * 20, 27 * 20};
	private static final int RELEASE_TICKS = 35 * 20;
	private static final int LEVITATION_AMPLITUDE = 0;
	private static final int DARKNESS_FADE_TICKS = 40;
	private static final int BEAM_HEIGHT = 40;
	private static final int BEAM_INTERVAL_TICKS = 3;
	
	private static final Map<UUID, Cutscene> ACTIVE_CUTSCENES = new HashMap<>();
	
	private static boolean questBedLogged = false;
	
	private static final class Cutscene
	{
		final ServerLevel level;
		final BlockPos[] pillars;
		final boolean[] pillarsLit;
		final long startTick;
		final Title title;
		
		Cutscene(ServerLevel level, BlockPos[] pillars, long startTick, Title title)
		{
			this.level = level;
			this.pillars = pillars;
			this.pillarsLit = new boolean[pillars.length];
			this.startTick = startTick;
			this.title = title;
		}
	}
	
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onLivingDeath(LivingDeathEvent event)
	{
		if(event.getEntity().level().isClientSide()) return;
		
		if(!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) return;
		
		var playerDataOpt = PlayerData.get(player);
		if(playerDataOpt.isEmpty()) return;
		PlayerData playerData = playerDataOpt.get();
		
		Title title = Title.getTitle(playerData).orElse(null);
		if(title == null) return;
		
		BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.1, player.getZ());
		if(!(player.level().getBlockState(below).getBlock() instanceof IGodTierBlock godTierBlock)) return;
		if(!godTierBlock.canGodTier()) return;
		if(godTierBlock.getAspect() != null && godTierBlock.getAspect() != title.heroAspect()) return;
		
		GodTierState state = playerData.getData(MSAttachments.GOD_TIER_STATE);
		
		SburbPlayerData sburbData = SburbPlayerData.get(player);
		if(!sburbData.hasEntered() || sburbData.getLandDimensionIfEntered() != player.level().dimension()) return;
		
		int requiredRung = MinestuckConfig.SERVER.requiredRungToGodTier.get();
		int currentRung = Echeladder.get(player).getRung();
		boolean rungRequirementMet = requiredRung < 0 || currentRung >= requiredRung;
		boolean eligible = state.isGodTier() || player.isCreative() || (state.canGodTier() && rungRequirementMet);
		
		if(!eligible)
		{
			player.displayClientMessage(Component.translatable(GOD_TIER_REJECT_KEY), true);
			return;
		}
		
		if(!state.isGodTier() && title.heroClass() == EnumClass.LORD)
			tryUnlockOverlord(player, playerData, state, event);
		
		if(!state.isGodTier()) ascend(player, title, state);
		else player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
		
		ServerLevel level = (ServerLevel) player.level();
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY(), player.getZ(), 30, 0, 0, 0, 0);
		level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
		level.playSound(null, player.blockPosition(), MSSoundEvents.MUSIC_RISE_UP.get(), SoundSource.RECORDS, 1.0F, 1.0F);
		
		player.setHealth(2.0F);
		event.setCanceled(true);
	}
	
	private static void tryUnlockOverlord(ServerPlayer player, PlayerData playerData, GodTierState state, LivingDeathEvent event)
	{
		var skills = playerData.getData(MSAttachments.GOD_TIER_SKILLS);
		if(skills.hasSkill(SkillRegistry.BADGE_OVERLORD.get())) return;
		
		int generalLevel = playerData.getData(MSAttachments.GOD_TIER_STATS).getLevel(GodTierStat.GENERAL);
		if(generalLevel < BadgeOverlord.REQUIRED_LEVEL)
		{
			player.displayClientMessage(Component.translatable(OVERLORD_SKILL_LEVEL_KEY, BadgeOverlord.REQUIRED_LEVEL), false);
			return;
		}
		if(event.getSource().getEntity() instanceof ServerPlayer)
		{
			player.displayClientMessage(Component.translatable(OVERLORD_PVP_DEATH_KEY), false);
			return;
		}
		
		skills.setMaxBadges(skills.getMaxBadges() + 2);
		skills.addSkill(SkillRegistry.BADGE_OVERLORD.get());
		player.setHealth(10.0F);
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 3));
		player.server.getPlayerList().broadcastSystemMessage(Component.translatable(OVERLORD_ASCEND_KEY, player.getDisplayName()), false);
		event.setCanceled(true);
	}
	
	private static void ascend(ServerPlayer player, Title title, GodTierState state)
	{
		player.level().getServer().getPlayerList().broadcastSystemMessage(Component.translatable(GOD_TIER_KEY, player.getDisplayName()), false);
		player.displayClientMessage(Component.translatable(GOD_TIER_MEDITATION_UNLOCK_KEY), true);
		
		EquipmentSlot[] slots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
		for(EquipmentSlot slot : slots)
			CaptchaDeckHandler.launchAnyItem(player, player.getItemBySlot(slot));
		
		Echeladder.get(player).setProgressEnabled(false);
		state.setClimbedTheSpire(true);
		state.setGodTier(true);
		PlayerData.get(player).ifPresent(d -> {
			d.getData(MSAttachments.GOD_TIER_STATS).initializeOnAscension();
			d.getData(MSAttachments.GOD_TIER_SKILLS).addSkill(SkillRegistry.GIFT_OF_GAB.get());
			d.getData(MSAttachments.GOD_TIER_KARMA).reset();
			
			List<GodTierDataPacket.StatData> statData = new ArrayList<>();
			GodTierStats stats = d.getData(MSAttachments.GOD_TIER_STATS);
			for(GodTierStat stat : GodTierStat.values())
				statData.add(new GodTierDataPacket.StatData(stat, stats.getLevel(stat), stats.getXp(stat)));
			PacketDistributor.sendToPlayer(player, new GodTierDataPacket(true, state.canGodTier(), true, statData, 0));
			
			List<GodTierSkillDataPacket.SkillData> skillData = new ArrayList<>();
			skillData.add(new GodTierSkillDataPacket.SkillData(SkillRegistry.GIFT_OF_GAB.get().id(), true, false));
			PacketDistributor.sendToPlayer(player, new GodTierSkillDataPacket(skillData, Optional.empty(), MinestuckConfig.SERVER.godTierBadgeSlots.get()));
		});
		
		startCutscene(player, title);
	}
	
	private static void equipGodTierArmor(ServerPlayer player, Title title)
	{
		Item[] armor = {MSItems.GOD_TIER_SHOES.get(), MSItems.GOD_TIER_PANTS.get(), MSItems.GOD_TIER_SHIRT.get(), MSItems.GOD_TIER_HOOD.get()};
		EquipmentSlot[] slots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
		
		for(int i = 0; i < armor.length; i++)
		{
			ItemStack armorStack = new ItemStack(armor[i]);
			armorStack.set(MSItemComponents.GOD_TIER_TITLE.get(), title);
			player.setItemSlot(slots[i], armorStack);
		}
	}
	
	private static void startCutscene(ServerPlayer player, Title title)
	{
		if(ACTIVE_CUTSCENES.containsKey(player.getUUID()))
			return;
		player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, RELEASE_TICKS + 20, LEVITATION_AMPLITUDE, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, RELEASE_TICKS + 20, 19));
		player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, RELEASE_TICKS + DARKNESS_FADE_TICKS, 0, false, false));
		
		if(!(player.level() instanceof ServerLevel level))
		{
			equipGodTierArmor(player, title);
			return;
		}
		
		QuestBedPiece piece = QuestBedPlacement.findQuestBedPiece(level);
		if(piece == null)
		{
			LOGGER.error("God tier cutscene aborted for {}: quest bed piece not found, see warnings above.", player.getGameProfile().getName());
			equipGodTierArmor(player, title);
			releasePlayer(player);
			return;
		}
		
		ACTIVE_CUTSCENES.put(player.getUUID(), new Cutscene(level, piece.getPillarLightPositions(), level.getGameTime(), title));
	}
	
	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event)
	{
		if(!questBedLogged)
		{
			var diagServer = ServerLifecycleHooks.getCurrentServer();
			if(diagServer != null)
			{
				for(ServerLevel diagLevel : diagServer.getAllLevels())
				{
					QuestBedPiece diagPiece = QuestBedPlacement.findQuestBedPiece(diagLevel);
					if(diagPiece != null)
					{
						LOGGER.info("Quest bed found at origin {} in dimension {}", diagPiece.getOrigin(), diagLevel.dimension().location());
						LOGGER.info("Quest bed navigation coordinates: [{}, {}, {}] | altar center: {}",
								diagPiece.getOrigin().getX(), diagPiece.getOrigin().getY(), diagPiece.getOrigin().getZ(), diagPiece.getAltarCenter());
						questBedLogged = true;
					} else LOGGER.info("Quest bed not found yet in dimension {}", diagLevel.dimension().location());
				}
			}
		}
		
		if(!ACTIVE_CUTSCENES.isEmpty())
		{
			Iterator<Map.Entry<UUID, Cutscene>> iterator = ACTIVE_CUTSCENES.entrySet().iterator();
			while(iterator.hasNext())
			{
				Map.Entry<UUID, Cutscene> entry = iterator.next();
				Cutscene cutscene = entry.getValue();
				
				ServerPlayer player = cutscene.level.getServer().getPlayerList().getPlayer(entry.getKey());
				if(player == null)
				{
					iterator.remove();
					continue;
				}
				
				long elapsed = cutscene.level.getGameTime() - cutscene.startTick;
				
				for(int i = 0; i < PILLAR_LIGHT_TICKS.length; i++)
				{
					if(!cutscene.pillarsLit[i] && elapsed >= PILLAR_LIGHT_TICKS[i])
					{
						cutscene.pillarsLit[i] = true;
						lightPillar(cutscene.level, cutscene.pillars[i]);
					}
					
					if(cutscene.pillarsLit[i] && elapsed % BEAM_INTERVAL_TICKS == 0)
						spawnPillarBeam(cutscene.level, cutscene.pillars[i]);
				}
				
				if(elapsed >= RELEASE_TICKS)
				{
					equipGodTierArmor(player, cutscene.title);
					releasePlayer(player);
					iterator.remove();
				}
			}
		}
		
		var server = ServerLifecycleHooks.getCurrentServer();
		if(server == null) return;
		
		for(ServerPlayer player : server.getPlayerList().getPlayers())
		{
			if(ACTIVE_CUTSCENES.containsKey(player.getUUID())) continue;
			if(player.isCreative() || player.isSpectator()) continue;
			
			var playerDataOpt = PlayerData.get(player);
			if(playerDataOpt.isEmpty()) continue;
			
			GodTierState state = playerDataOpt.get().getData(MSAttachments.GOD_TIER_STATE);
			if(state.isGodTier() && !player.getAbilities().mayfly)
			{
				player.getAbilities().mayfly = true;
				player.onUpdateAbilities();
			}
		}
	}
	
	private static void lightPillar(ServerLevel level, BlockPos pos)
	{
		level.setBlock(pos, MSBlocks.GLOWING_HERO_STONE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
		level.playSound(null, pos, SoundEvents.CONDUIT_ACTIVATE, SoundSource.BLOCKS, 3.0F, 1.0F);
	}
	
	private static void spawnPillarBeam(ServerLevel level, BlockPos pillarTop)
	{
		double centerX = pillarTop.getX() + 0.5;
		double centerZ = pillarTop.getZ() + 0.5;
		
		for(int step = 0; step < BEAM_HEIGHT; step++)
		{
			double y = pillarTop.getY() + 1.0 + step;
			level.sendParticles(ParticleTypes.END_ROD, centerX, y, centerZ, 2, 0.15, 0.1, 0.15, 0.005);
		}
	}
	
	private static void releasePlayer(ServerPlayer player)
	{
		player.removeEffect(MobEffects.DARKNESS);
		player.removeEffect(MobEffects.LEVITATION);
		player.setDeltaMovement(player.getDeltaMovement().x, 0.0, player.getDeltaMovement().z);
		player.getAbilities().mayfly = true;
		player.getAbilities().flying = true;
		player.onUpdateAbilities();
		
		if(player.level() instanceof ServerLevel level)
		{
			level.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY(), player.getZ(), 1, 0, 0, 0, 0);
			level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.2F);
		}
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void playerDeathPost(LivingDeathEvent event)
	{
		if(!event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) return;
		if(!(player.level() instanceof ServerLevel level) || !level.getLevelData().isHardcore()) return;
		if(player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) return;
		
		var playerDataOpt = PlayerData.get(player);
		if(playerDataOpt.isEmpty()) return;
		
		GodTierState state = playerDataOpt.get().getData(MSAttachments.GOD_TIER_STATE);
		if(state.isGodTier()) player.setGameMode(level.getServer().getDefaultGameType());
	}
}
