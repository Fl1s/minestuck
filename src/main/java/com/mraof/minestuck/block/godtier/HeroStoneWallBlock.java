package com.mraof.minestuck.block.godtier;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.world.level.block.WallBlock;

import javax.annotation.Nullable;

public class HeroStoneWallBlock extends WallBlock implements IGodTierBlock
{
	@Nullable
	private final EnumAspect aspect;

	public HeroStoneWallBlock(Properties properties, @Nullable EnumAspect aspect)
	{
		super(properties);
		this.aspect = aspect;
	}

	@Nullable
	@Override
	public EnumAspect getAspect()
	{
		return aspect;
	}

	@Override
	public boolean canGodTier()
	{
		return false;
	}
}
