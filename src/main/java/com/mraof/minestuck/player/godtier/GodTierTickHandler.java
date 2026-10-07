package com.mraof.minestuck.player.godtier;

import com.mraof.minestuck.Minestuck;
import com.mraof.minestuck.MinestuckConfig;
import com.mraof.minestuck.effects.MSEffects;
import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.Echeladder;
import com.mraof.minestuck.player.PlayerData;
import com.mraof.minestuck.network.GodTierConfigPacket;
import com.mraof.minestuck.network.GodTierDataPacket;
import com.mraof.minestuck.network.GodTierSkillDataPacket;
import com.mraof.minestuck.network.GodTierTitlePacket;
import com.mraof.minestuck.player.godtier.skill.*;
import com.mraof.minestuck.player.Title;
import com.mraof.minestuck.skaianet.SburbPlayerData;
import com.mraof.minestuck.world.gen.structure.questbed.QuestBedPiece;
import com.mraof.minestuck.world.gen.structure.questbed.QuestBedPlacement;
import com.mraof.minestuck.util.MSAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Abilities;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

@EventBusSubscriber(modid = Minestuck.MOD_ID)
public final class GodTierTickHandler
{
	private static final int ASPECT_EFFECT_DURATION = 600;
	
	private static final Map<EnumAspect, Holder<MobEffect>> ASPECT_EFFECTS = Map.ofEntries(Map.entry(EnumAspect.BLOOD, MobEffects.ABSORPTION), Map.entry(EnumAspect.BREATH, MobEffects.MOVEMENT_SPEED), Map.entry(EnumAspect.DOOM, MobEffects.DAMAGE_RESISTANCE), Map.entry(EnumAspect.HEART, MobEffects.ABSORPTION), Map.entry(EnumAspect.HOPE, MobEffects.FIRE_RESISTANCE), Map.entry(EnumAspect.LIFE, MobEffects.REGENERATION), Map.entry(EnumAspect.LIGHT, MobEffects.LUCK), Map.entry(EnumAspect.MIND, MobEffects.NIGHT_VISION), Map.entry(EnumAspect.RAGE, MobEffects.DAMAGE_BOOST), Map.entry(EnumAspect.SPACE, MobEffects.JUMP), Map.entry(EnumAspect.TIME, MobEffects.DIG_SPEED), Map.entry(EnumAspect.VOID, MobEffects.INVISIBILITY));
	
	private static final Map<EnumAspect, Float> ASPECT_STRENGTH = Map.ofEntries(Map.entry(EnumAspect.BLOOD, 1F / 12), Map.entry(EnumAspect.BREATH, 1F / 15), Map.entry(EnumAspect.DOOM, 1F / 28), Map.entry(EnumAspect.HEART, 1F / 25), Map.entry(EnumAspect.HOPE, 1F / 18), Map.entry(EnumAspect.LIFE, 1F / 20), Map.entry(EnumAspect.LIGHT, 1F / 10), Map.entry(EnumAspect.MIND, 1F / 12), Map.entry(EnumAspect.RAGE, 1F / 25), Map.entry(EnumAspect.SPACE, 1F / 10), Map.entry(EnumAspect.TIME, 1F / 13), Map.entry(EnumAspect.VOID, 1F / 12));
	
	private GodTierTickHandler()
	{
	}
	
	@SubscribeEvent
	private static void onPlayerTick(PlayerTickEvent.Post event)
	{
		if(!(event.getEntity() instanceof ServerPlayer player)) return;
		
		PlayerData.get(player).ifPresent(playerData -> {
			GodTierState state = playerData.getData(MSAttachments.GOD_TIER_STATE);
			GodTierStats stats = playerData.getData(MSAttachments.GOD_TIER_STATS);
			
			updateAttributes(player, stats, state, playerData.getData(MSAttachments.GOD_TIER_SKILLS));
			updateFlight(player, state);
			updateQuestBedArea(player, state);
			updateClimbedTheSpire(player, state);
			updateAspectEffects(player, playerData, state);
		});
	}
	
	@SubscribeEvent
	private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
	{
		if(!(event.getEntity() instanceof ServerPlayer player)) return;
		PlayerData.get(player).ifPresent(playerData -> {
			GodTierState state = playerData.getData(MSAttachments.GOD_TIER_STATE);
			sendDataPacket(player, playerData);
			PacketDistributor.sendToPlayer(player, GodTierConfigPacket.create());
			Title title = Title.getTitle(playerData).orElse(null);
			if(title != null) PacketDistributor.sendToPlayer(player, new GodTierTitlePacket(title.asTextComponent()));
			if(!state.isGodTier()) removeGodTierModifiers(player);
			if(state.isPendingArmor() && state.isGodTier())
				GodTierAscensionHandler.finishInterruptedCutscene(player, title);
		});
	}
	
	@SubscribeEvent
	private static void onServerStopped(ServerStoppedEvent event)
	{
		QUEST_BED_ORIGINS.clear();
		QUEST_BED_RETRY_TIME.clear();
	}
	
	private static final Map<ResourceKey<Level>, BlockPos> QUEST_BED_ORIGINS = new HashMap<>();
	private static final Map<ResourceKey<Level>, Long> QUEST_BED_RETRY_TIME = new HashMap<>();
	
	@Nullable
	public static BlockPos getQuestBedOrigin(ServerLevel level)
	{
		BlockPos cached = QUEST_BED_ORIGINS.get(level.dimension());
		if(cached != null) return cached;
		long gameTime = level.getGameTime();
		if(QUEST_BED_RETRY_TIME.getOrDefault(level.dimension(), 0L) > gameTime) return null;
		QuestBedPiece piece = QuestBedPlacement.findQuestBedPiece(level);
		if(piece == null)
		{
			QUEST_BED_RETRY_TIME.put(level.dimension(), gameTime + 600);
			return null;
		}
		QUEST_BED_ORIGINS.put(level.dimension(), piece.getOrigin());
		return piece.getOrigin();
	}
	
	private static void updateAttributes(ServerPlayer player, GodTierStats stats, GodTierState state, GodTierSkills skills)
	{
		boolean active = state.isGodTier();
		double multiplier = active ? badgeMultiplier(skills) : 1.0;
		for(GodTierStat stat : GodTierStat.values())
		{
			if(!stat.hasAttribute()) continue;
			AttributeInstance attribute = player.getAttribute(stat.attribute());
			if(attribute == null) continue;
			AttributeModifier existing = attribute.getModifier(stat.modifierId());
			boolean shouldApply = active && (stat != GodTierStat.SPEED || player.isSprinting());
			if(!shouldApply)
			{
				if(existing != null) attribute.removeModifier(stat.modifierId());
				continue;
			}
			double amount = stats.getAttributeBonus(stat, multiplier);
			//Only touch the attribute when something actually changed, as every change gets synced to the client
			if(existing != null && existing.amount() == amount && existing.operation() == stat.operation()) continue;
			if(existing != null) attribute.removeModifier(stat.modifierId());
			attribute.addTransientModifier(new AttributeModifier(stat.modifierId(), amount, stat.operation()));
		}
	}
	
	private static double badgeMultiplier(GodTierSkills skills)
	{
		double multiplier = 1.0;
		if(skills.isBadgeActive(SkillRegistry.BADGE_PAGE.get())) multiplier *= 2.0;
		if(skills.isBadgeActive(SkillRegistry.BADGE_OVERLORD.get())) multiplier *= 3.0;
		return multiplier;
	}
	
	private static void removeGodTierModifiers(ServerPlayer player)
	{
		for(GodTierStat stat : GodTierStat.values())
		{
			if(!stat.hasAttribute()) continue;
			
			AttributeInstance attribute = player.getAttribute(stat.attribute());
			if(attribute.hasModifier(stat.modifierId())) attribute.removeModifier(stat.modifierId());
		}
	}
	
	private static void updateFlight(ServerPlayer player, GodTierState state)
	{
		if(player.isCreative() || player.isSpectator())
		{
			state.setFlightGranted(false);
			return;
		}
		Abilities abilities = player.getAbilities();
		boolean changed = false;
		if(state.isGodTier() && !player.hasEffect(MSEffects.EARTHBOUND))
		{
			if(!abilities.mayfly)
			{
				abilities.mayfly = true;
				changed = true;
			}
			state.setFlightGranted(true);
		} else if(state.isGodTier() || state.isFlightGranted() || state.isPendingReset())
		{
			if(abilities.mayfly)
			{
				abilities.mayfly = false;
				changed = true;
			}
			if(abilities.flying)
			{
				abilities.flying = false;
				changed = true;
			}
			state.setFlightGranted(false);
			if(!state.isGodTier()) state.clearPendingReset();
		}
		if(changed) player.onUpdateAbilities();
	}
	
	private static void updateQuestBedArea(ServerPlayer player, GodTierState state)
	{
		if(state.isGodTier() || player.isCreative()) return;
		SburbPlayerData sburbData = SburbPlayerData.get(player);
		if(!sburbData.hasEntered() || sburbData.getLandDimensionIfEntered() != player.level().dimension()) return;
		BlockPos origin = getQuestBedOrigin((ServerLevel) player.level());
		if(origin == null) return;
		int radius = 250;
		double dx = player.getX() - origin.getX();
		double dz = player.getZ() - origin.getZ();
		if(Math.abs(dx) >= radius || Math.abs(dz) >= radius) return;
		if(!state.hasClimbedTheSpire()) refreshEffect(player, MSEffects.EARTHBOUND, 40, 0);
		refreshEffect(player, MSEffects.CREATIVE_SHOCK, 40, 0);
	}
	
	private static void refreshEffect(ServerPlayer player, Holder<MobEffect> effect, int duration, int amplifier)
	{
		MobEffectInstance current = player.getEffect(effect);
		if(current == null || current.getDuration() < 20)
			player.addEffect(new MobEffectInstance(effect, duration, amplifier, true, true));
	}
	
	private static void updateClimbedTheSpire(ServerPlayer player, GodTierState state)
	{
		if(state.hasClimbedTheSpire() || !player.onGround()) return;
		SburbPlayerData sburbData = SburbPlayerData.get(player);
		if(!sburbData.hasEntered() || sburbData.getLandDimensionIfEntered() != player.level().dimension()) return;
		if(player.getY() < QuestBedPiece.TOP) return;
		BlockPos origin = getQuestBedOrigin((ServerLevel) player.level());
		if(origin == null) return;
		if(Math.abs(player.getX() - origin.getX()) < QuestBedPiece.RADIUS && Math.abs(player.getZ() - origin.getZ()) < QuestBedPiece.RADIUS)
			state.setClimbedTheSpire(true);
	}
	
	public static void sendDataPacket(ServerPlayer player, PlayerData playerData)
	{
		sendStatsPacket(player, playerData);
		sendSkillDataPacket(player, playerData);
	}
	
	public static void sendStatsPacket(ServerPlayer player, PlayerData playerData)
	{
		GodTierState state = playerData.getData(MSAttachments.GOD_TIER_STATE);
		GodTierStats stats = playerData.getData(MSAttachments.GOD_TIER_STATS);
		GodTierKarma karma = playerData.getData(MSAttachments.GOD_TIER_KARMA);
		List<GodTierDataPacket.StatData> statData = new ArrayList<>();
		for(GodTierStat stat : GodTierStat.values())
			statData.add(new GodTierDataPacket.StatData(stat, stats.getLevel(stat), stats.getXp(stat)));
		PacketDistributor.sendToPlayer(player, new GodTierDataPacket(state.isGodTier(), state.canGodTier(), state.hasClimbedTheSpire(), statData, karma.getTotal()));
	}
	
	public static void sendSkillDataPacket(ServerPlayer player, PlayerData playerData)
	{
		GodTierSkills skills = playerData.getData(MSAttachments.GOD_TIER_SKILLS);
		List<GodTierSkillDataPacket.SkillData> skillData = new ArrayList<>();
		skills.getAllBadges().forEach(id -> skillData.add(new GodTierSkillDataPacket.SkillData(id, skills.isBadgeEnabledById(id), skills.isPassiveEnabledById(id))));
		PacketDistributor.sendToPlayer(player, new GodTierSkillDataPacket(skillData, Optional.ofNullable(skills.masterBadge()), skills.badgeLimit()));
	}
	
	private static void updateAspectEffects(ServerPlayer player, PlayerData playerData, GodTierState state)
	{
		boolean wasApplied = state.areAspectEffectsApplied();
		Title title = Title.getTitle(playerData).orElse(null);
		boolean locked = player.hasEffect(MSEffects.GOD_TIER_LOCK);
		boolean toggle = player.getData(MSAttachments.EFFECT_TOGGLE);
		boolean active = title != null && MinestuckConfig.SERVER.aspectEffects.get() && state.isGodTier() && toggle && !locked;
		
		if(!active && !wasApplied) return;
		if(title == null)
		{
			state.setAspectEffectsApplied(false);
			return;
		}
		
		Map<Holder<MobEffect>, MobEffectInstance> effects = getAspectEffects(player, playerData, state, title.heroAspect());
		if(!active)
		{
			effects.keySet().forEach(player::removeEffect);
			state.setAspectEffectsApplied(false);
			return;
		}
		
		for(MobEffectInstance effect : effects.values())
		{
			MobEffectInstance current = player.getEffect(effect.getEffect());
			boolean refreshOnly = REFRESH_EFFECTS.contains(effect.getEffect());
			if(current == null ? (!refreshOnly || player.tickCount % 600 == 0) : (!refreshOnly && current.getDuration() <= 200 && current.getAmplifier() <= effect.getAmplifier()))
				player.addEffect(effect);
		}
		state.setAspectEffectsApplied(true);
	}
	
	private static final List<Holder<MobEffect>> REFRESH_EFFECTS = List.of(MobEffects.ABSORPTION, MobEffects.REGENERATION, MobEffects.WITHER, MobEffects.POISON);
	
	public static Map<Holder<MobEffect>, MobEffectInstance> getAspectEffects(ServerPlayer player, PlayerData playerData, GodTierState state, EnumAspect aspect)
	{
		Map<Holder<MobEffect>, MobEffectInstance> effects = new LinkedHashMap<>();
		GodTierSkills skills = playerData.getData(MSAttachments.GOD_TIER_SKILLS);
		int rung = Echeladder.get(player).getRung();
		
		int level = (int) (ASPECT_STRENGTH.get(aspect) * (state.isGodTier() ? 60F : rung));
		if(skills.isBadgeActive(SkillRegistry.BADGE_PAGE.get())) level += 2;
		
		if(skills.isBadgeActive(SkillRegistry.EFFECT_BUFF.get()))
		{
			switch(aspect)
			{
				case DOOM ->
						effects.put(MobEffects.ABSORPTION, new MobEffectInstance(MobEffects.ABSORPTION, ASPECT_EFFECT_DURATION, 2, true, false));
				//TODO HOPE (decayproof), MIND (mind fortitude) and VOID (void conceal) get their own effects in 1.12.2. Those effects are not ported yet.
				case HOPE, MIND, VOID ->
				{
				}
				default -> level *= 2;
			}
		}
		
		Holder<MobEffect> effect = ASPECT_EFFECTS.get(aspect);
		if(level > 0 && effect != null)
		effects.put(effect, new MobEffectInstance(effect, ASPECT_EFFECT_DURATION, level - 1, true, false));
		
		if((state.isGodTier() || rung > 18) && aspect == EnumAspect.HOPE)
			effects.put(MobEffects.WATER_BREATHING, new MobEffectInstance(MobEffects.WATER_BREATHING, ASPECT_EFFECT_DURATION, 0, true, false));
		
		return effects;
	}
}
