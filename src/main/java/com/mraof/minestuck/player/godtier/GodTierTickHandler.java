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

import java.util.ArrayList;
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
			if(!state.isGodTier())
			{
				removeGodTierModifiers(player);
				updateFlight(player, state);
			}
		});
	}
	
	private static void updateAttributes(ServerPlayer player, GodTierStats stats, GodTierState state, GodTierSkills skills)
	{
		boolean active = state.isGodTier();
		for(GodTierStat stat : GodTierStat.values())
		{
			if(!stat.hasAttribute()) continue;
			
			AttributeInstance attribute = player.getAttribute(stat.attribute());
			double multiplier = badgeMultiplier(skills);
			AttributeModifier modifier = new AttributeModifier(stat.modifierId(), stats.getAttributeBonus(stat, multiplier), stat.operation());
			
			boolean current = attribute.hasModifier(stat.modifierId());
			if(!active)
			{
				if(current) attribute.removeModifier(stat.modifierId());
				continue;
			}
			
			boolean shouldApply = stat != GodTierStat.SPEED || player.isSprinting();
			if(!shouldApply)
			{
				if(current) attribute.removeModifier(stat.modifierId());
				continue;
			}
			
			if(current) attribute.removeModifier(stat.modifierId());
			attribute.addTransientModifier(modifier);
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
		if(player.isCreative() || player.isSpectator()) return;
		
		boolean mayFly = state.isGodTier() && !player.hasEffect(MSEffects.EARTHBOUND);
		player.getAbilities().mayfly = mayFly;
		if(!mayFly) player.getAbilities().flying = false;
		player.onUpdateAbilities();
	}
	
	private static void updateQuestBedArea(ServerPlayer player, GodTierState state)
	{
		SburbPlayerData sburbData = SburbPlayerData.get(player);
		if(!sburbData.hasEntered() || sburbData.getLandDimensionIfEntered() != player.level().dimension()) return;
		
		QuestBedPiece questBed = QuestBedPlacement.findQuestBedPiece((ServerLevel) player.level());
		if(questBed == null) return;
		
		BlockPos origin = questBed.getOrigin();
		int radius = 250;
		double dx = player.getX() - origin.getX();
		double dz = player.getZ() - origin.getZ();
		boolean nearBed = Math.abs(dx) < radius && Math.abs(dz) < radius;
		
		if(!nearBed || player.isCreative() || state.isGodTier()) return;
		
		refreshEffect(player, MSEffects.EARTHBOUND, 40, 0);
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
		
		QuestBedPiece questBed = QuestBedPlacement.findQuestBedPiece((ServerLevel) player.level());
		if(questBed == null) return;
		
		BlockPos origin = questBed.getOrigin();
		if(player.getY() >= QuestBedPiece.TOP && Math.abs(player.getX() - origin.getX()) < QuestBedPiece.RADIUS && Math.abs(player.getZ() - origin.getZ()) < QuestBedPiece.RADIUS)
			state.setClimbedTheSpire(true);
	}
	
	public static void sendDataPacket(ServerPlayer player, PlayerData playerData)
	{
		GodTierState state = playerData.getData(MSAttachments.GOD_TIER_STATE);
		GodTierStats stats = playerData.getData(MSAttachments.GOD_TIER_STATS);
		GodTierKarma karma = playerData.getData(MSAttachments.GOD_TIER_KARMA);
		
		List<GodTierDataPacket.StatData> statData = new ArrayList<>();
		for(GodTierStat stat : GodTierStat.values())
			statData.add(new GodTierDataPacket.StatData(stat, stats.getLevel(stat), stats.getXp(stat)));
		
		PacketDistributor.sendToPlayer(player, new GodTierDataPacket(state.isGodTier(), state.canGodTier(), state.hasClimbedTheSpire(), statData, karma.getTotal()));
		sendSkillDataPacket(player, playerData);
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
		Title title = Title.getTitle(playerData).orElse(null);
		boolean locked = player.hasEffect(MSEffects.GOD_TIER_LOCK);
		
		if(title == null)
		{
			clearAspectEffects(player, null);
			return;
		}
		
		EnumAspect aspect = title.heroAspect();
		boolean toggle = player.getData(MSAttachments.EFFECT_TOGGLE);
		boolean active = MinestuckConfig.SERVER.aspectEffects.get() && state.isGodTier() && toggle && !locked;
		
		if(!active)
		{
			clearAspectEffects(player, aspect);
			return;
		}
		
		Holder<MobEffect> effect = ASPECT_EFFECTS.get(aspect);
		if(effect == null)
		{
			clearAspectEffects(player, aspect);
			return;
		}
		
		int amplifier = aspectEffectAmplifier(player, playerData, state, aspect);
		MobEffectInstance current = player.getEffect(effect);
		if(current == null || current.getDuration() < 200 || current.getAmplifier() < amplifier)
			player.addEffect(new MobEffectInstance(effect, ASPECT_EFFECT_DURATION, amplifier, true, false));
		
		if((state.isGodTier() || Echeladder.get(player).getRung() > 18) && aspect == EnumAspect.HOPE)
			player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, ASPECT_EFFECT_DURATION, 0, true, false));
	}
	
	private static int aspectEffectAmplifier(ServerPlayer player, PlayerData playerData, GodTierState state, EnumAspect aspect)
	{
		GodTierSkills skills = playerData.getData(MSAttachments.GOD_TIER_SKILLS);
		int rung = Echeladder.get(player).getRung();
		int level = (int) (ASPECT_STRENGTH.get(aspect) * (state.isGodTier() ? 60F : rung));
		if(skills.isBadgeActive(SkillRegistry.BADGE_PAGE.get())) level += 2;
		if(skills.isBadgeActive(SkillRegistry.EFFECT_BUFF.get())) level *= 2;
		
		return Math.max(0, level - 1);
	}
	
	private static void clearAspectEffects(ServerPlayer player, @Nullable EnumAspect aspect)
	{
		if(aspect != null)
		{
			Holder<MobEffect> effect = ASPECT_EFFECTS.get(aspect);
			if(effect != null) player.removeEffect(effect);
		}
		player.removeEffect(MobEffects.WATER_BREATHING);
	}
}
