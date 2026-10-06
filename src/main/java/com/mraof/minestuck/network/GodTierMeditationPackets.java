package com.mraof.minestuck.network;

import com.mraof.minestuck.Minestuck;
import com.mraof.minestuck.MinestuckConfig;
import com.mraof.minestuck.client.gui.MSScreenFactories;
import com.mraof.minestuck.player.PlayerData;
import com.mraof.minestuck.player.Title;
import com.mraof.minestuck.player.godtier.GodTierStat;
import com.mraof.minestuck.player.godtier.GodTierStats;
import com.mraof.minestuck.player.godtier.GodTierState;
import com.mraof.minestuck.player.godtier.skill.*;
import com.mraof.minestuck.player.godtier.GodTierTickHandler;
import com.mraof.minestuck.util.MSAttachments;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class GodTierMeditationPackets
{
	private GodTierMeditationPackets()
	{
	}
	
	public record OpenScreen() implements MSPacket.PlayToClient
	{
		public static final Type<OpenScreen> ID = new Type<>(Minestuck.id("god_tier/meditation_open"));
		public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, OpenScreen> STREAM_CODEC = StreamCodec.unit(new OpenScreen());
		
		@Override
		public Type<? extends CustomPacketPayload> type()
		{
			return ID;
		}
		
		@Override
		public void execute(net.neoforged.neoforge.network.handling.IPayloadContext context)
		{
			MSScreenFactories.displayGodTierMeditationScreen();
		}
	}
	
	
	public record OpenBadgeScreen() implements MSPacket.PlayToClient
	{
		public static final Type<OpenBadgeScreen> ID = new Type<>(Minestuck.id("god_tier/badge_open"));
		public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, OpenBadgeScreen> STREAM_CODEC = StreamCodec.unit(new OpenBadgeScreen());
		
		@Override
		public Type<? extends CustomPacketPayload> type()
		{
			return ID;
		}
		
		@Override
		public void execute(net.neoforged.neoforge.network.handling.IPayloadContext context)
		{
			MSScreenFactories.displayGodTierBadgeScreen();
		}
	}
	
	public record AttemptBadgeUnlock(ResourceLocation skillId) implements MSPacket.PlayToServer
	{
		public static final Type<AttemptBadgeUnlock> ID = new Type<>(Minestuck.id("god_tier/attempt_badge_unlock"));
		public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, AttemptBadgeUnlock> STREAM_CODEC = StreamCodec.composite(ResourceLocation.STREAM_CODEC, AttemptBadgeUnlock::skillId, AttemptBadgeUnlock::new);
		
		@Override
		public Type<? extends CustomPacketPayload> type()
		{
			return ID;
		}
		
		@Override
		public void execute(net.neoforged.neoforge.network.handling.IPayloadContext context, ServerPlayer player)
		{
			Skill skill = SkillRegistry.get(skillId);
			if(skill == null || !(skill instanceof Badge badge)) return;
			
			PlayerData.get(player).ifPresent(playerData -> {
				GodTierState state = playerData.getData(MSAttachments.GOD_TIER_STATE);
				GodTierSkills skills = playerData.getData(MSAttachments.GOD_TIER_SKILLS);
				Title title = Title.getTitle(playerData).orElse(null);
				
				if(!state.isGodTier() || title == null || skills.hasSkill(skill)) return;
				if(player.level() instanceof ServerLevel level && !badge.canUnlock(level, player)) return;
				
				if(skills.badgesLeft() <= 0 && !(skill instanceof MasterBadge)) return;
				
				skills.addSkill(skill);
				if(skill instanceof MasterBadge) skills.setMasterBadge(skill);
				badge.onBadgeUnlocked((ServerLevel) player.level(), player);
				GodTierTickHandler.sendDataPacket(player, playerData);
				GodTierTickHandler.sendSkillDataPacket(player, playerData);
			});
		}
	}
	
	public record ToggleBadge(ResourceLocation skillId) implements MSPacket.PlayToServer
	{
		public static final Type<ToggleBadge> ID = new Type<>(Minestuck.id("god_tier/toggle_badge"));
		public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, ToggleBadge> STREAM_CODEC = StreamCodec.composite(ResourceLocation.STREAM_CODEC, ToggleBadge::skillId, ToggleBadge::new);
		
		@Override
		public Type<? extends CustomPacketPayload> type()
		{
			return ID;
		}
		
		@Override
		public void execute(net.neoforged.neoforge.network.handling.IPayloadContext context, ServerPlayer player)
		{
			Skill skill = SkillRegistry.get(skillId);
			if(skill == null || !(skill instanceof Badge)) return;
			
			PlayerData.get(player).ifPresent(playerData -> {
				GodTierSkills skills = playerData.getData(MSAttachments.GOD_TIER_SKILLS);
				if(!skills.hasSkill(skill)) return;
				
				if(skills.isBadgeActive(skill)) skills.setBadgeEnabled(skill, false);
				else if(skills.badgesLeft() > 0 || skill instanceof MasterBadge) skills.setBadgeEnabled(skill, true);
			});
		}
	}
	
	public record UpgradeSkill(GodTierStat stat, int amount) implements MSPacket.PlayToServer
	{
		public static final Type<UpgradeSkill> ID = new Type<>(Minestuck.id("god_tier/upgrade_skill"));
		public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, UpgradeSkill> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.idMapper(GodTierStat::fromOrdinal, GodTierStat::ordinal),
				UpgradeSkill::stat,
				ByteBufCodecs.VAR_INT,
				UpgradeSkill::amount,
				UpgradeSkill::new);
		
		@Override
		public Type<? extends CustomPacketPayload> type()
		{
			return ID;
		}
		
		@Override
		public void execute(net.neoforged.neoforge.network.handling.IPayloadContext context, ServerPlayer player)
		{
			PlayerData.get(player).ifPresent(playerData -> {
				GodTierState state = playerData.getData(MSAttachments.GOD_TIER_STATE);
				GodTierStats stats = playerData.getData(MSAttachments.GOD_TIER_STATS);
				Title title = Title.getTitle(playerData).orElse(null);
				
				if(!state.isGodTier() || title == null || stat == GodTierStat.GENERAL) return;
				
				int maxLevel = MinestuckConfig.SERVER.maxGodTier.get();
				if(maxLevel >= 0 && stats.getLevel(GodTierStat.GENERAL) >= maxLevel) return;
				
				int xpCost = getUpgradeCost(stats, stat, amount);
				if(amount <= 0 || player.experienceLevel < xpCost) return;
				
				player.giveExperienceLevels(-xpCost);
				stats.addXp(stat, amount, title.heroClass());
				GodTierTickHandler.sendDataPacket(player, playerData);
			});
		}
		
		private static int getUpgradeCost(GodTierStats stats, GodTierStat stat, int amount)
		{
			// Progression-balanced cost: base cost grows slowly with the current level.
				int level = stats.getLevel(stat);
				int baseCost = Math.max(1, MinestuckConfig.SERVER.godTierXpThreshold.get() / 6);
				int levelCost = baseCost + Math.floorDiv(level, 5);
				return levelCost * amount;
		}
	}
}
