package com.mraof.minestuck.player.godtier.skill;

import com.mraof.minestuck.player.EnumClass;
import com.mraof.minestuck.player.PlayerData;
import com.mraof.minestuck.player.Title;
import com.mraof.minestuck.player.godtier.GodTierStat;
import com.mraof.minestuck.util.MSAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class BadgeOverlord extends Badge
{
	public static final int REQUIRED_LEVEL = 80;
	
	public BadgeOverlord(ResourceLocation id, int sortIndex)
	{
		super(id, sortIndex);
	}
	
	@Override
	public boolean canAppearOnList(ServerLevel level, ServerPlayer player)
	{
		return false;
	}
	
	@Override
	public boolean isReadable(ServerLevel level, ServerPlayer player)
	{
		return PlayerData.get(player).map(data -> {
			var skills = data.getData(MSAttachments.GOD_TIER_SKILLS);
			int levelStat = data.getData(MSAttachments.GOD_TIER_STATS).getLevel(GodTierStat.GENERAL);
			return (skills.masterBadge() != null || levelStat >= REQUIRED_LEVEL);
		}).orElse(false);
	}
}
