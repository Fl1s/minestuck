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
			if(!(skill instanceof Badge badge)) return;
			if(!(player.level() instanceof ServerLevel level)) return;
			
			PlayerData.get(player).ifPresent(playerData -> {
				GodTierState state = playerData.getData(MSAttachments.GOD_TIER_STATE);
				GodTierSkills skills = playerData.getData(MSAttachments.GOD_TIER_SKILLS);
				Title title = Title.getTitle(playerData).orElse(null);
				if(!state.isGodTier() || title == null || skills.hasSkill(skill)) return;
				
				if(skill instanceof MasterBadge)
				{
					if(skills.masterBadge() != null) return;
				} else if(skills.badgesLeft() <= 0)
					return;
				
				boolean masterControlUnlock = player.isCreative() && state.hasMasterControl();
				if(!masterControlUnlock && (!badge.isReadable(level, player) || !badge.canUnlock(level, player))) return;
				
				if(!skills.addSkill(skill)) return;
				badge.onBadgeUnlocked(level, player);
				GodTierTickHandler.sendDataPacket(player, playerData);
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
			if(!(skill instanceof Badge)) return;
			
			PlayerData.get(player).ifPresent(playerData -> {
				GodTierSkills skills = playerData.getData(MSAttachments.GOD_TIER_SKILLS);
				if(!skills.hasSkill(skill)) return;
				skills.setBadgeEnabled(skill, !skills.isBadgeEnabled(skill));
				GodTierTickHandler.sendSkillDataPacket(player, playerData);
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
			if(amount < 1 || amount > 5) return;
			
			PlayerData.get(player).ifPresent(playerData -> {
				GodTierState state = playerData.getData(MSAttachments.GOD_TIER_STATE);
				GodTierStats stats = playerData.getData(MSAttachments.GOD_TIER_STATS);
				Title title = Title.getTitle(playerData).orElse(null);
				if(!state.isGodTier() || title == null || stat == GodTierStat.GENERAL) return;
				
				int maxLevel = MinestuckConfig.SERVER.maxGodTier.get();
				if(maxLevel >= 0 && stats.getLevel(GodTierStat.GENERAL) >= maxLevel) return;
				
				if(!player.isCreative() && player.experienceLevel < MinestuckConfig.SERVER.godTierXpThreshold.get()) return;
				int actualAmount = player.isCreative() ? amount : Math.min(player.experienceLevel, amount);
				if(actualAmount <= 0) return;
				
				stats.addXp(stat, actualAmount, title.heroClass());
				if(!player.isCreative())
					player.giveExperienceLevels(-actualAmount);
				GodTierTickHandler.sendStatsPacket(player, playerData);
			});
		}
	}
}
