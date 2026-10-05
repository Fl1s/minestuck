package com.mraof.minestuck.player.godtier.skill;

import net.minecraft.resources.ResourceLocation;

public class MasterBadge extends Badge
{
	public MasterBadge(ResourceLocation id, int sortIndex)
	{
		super(id, sortIndex);
	}
	
	@Override
	public boolean canDisable()
	{
		return false;
	}
}
