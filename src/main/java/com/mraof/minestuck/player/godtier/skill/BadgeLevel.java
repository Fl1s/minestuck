package com.mraof.minestuck.player.godtier.skill;

import net.minecraft.network.chat.Component;
import com.mraof.minestuck.player.PlayerData;
import com.mraof.minestuck.player.godtier.GodTierStat;
import com.mraof.minestuck.util.MSAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class BadgeLevel extends Badge
{
	private final int requiredLevel;
	
	public BadgeLevel(ResourceLocation id, int sortIndex, int requiredLevel)
	{
		super(id, sortIndex);
		this.requiredLevel = requiredLevel;
	}
	
	public int requiredLevel()
	{
		return requiredLevel;
	}
	
	@Override
	public Component getReadRequirements()
	{
		return Component.translatable("skill.minestuck.read.level", requiredLevel);
	}
	
	@Override
	public boolean isReadable(ServerLevel level, ServerPlayer player)
	{
		return PlayerData.get(player)
				.map(data -> data.getData(MSAttachments.GOD_TIER_STATS).getLevel(GodTierStat.GENERAL) >= requiredLevel)
				.orElse(false);
	}
}
