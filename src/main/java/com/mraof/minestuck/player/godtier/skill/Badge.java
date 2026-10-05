package com.mraof.minestuck.player.godtier.skill;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class Badge extends Skill
{
	public Badge(ResourceLocation id, int sortIndex)
	{
		super(id, sortIndex);
	}
	
	public void onBadgeUnlocked(ServerLevel level, ServerPlayer player)
	{
	}
}
