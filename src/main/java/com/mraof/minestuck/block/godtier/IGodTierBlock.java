package com.mraof.minestuck.block.godtier;

import com.mraof.minestuck.player.EnumAspect;

public interface IGodTierBlock
{
	EnumAspect getAspect();

	default boolean canGodTier()
	{
		return true;
	}
}
