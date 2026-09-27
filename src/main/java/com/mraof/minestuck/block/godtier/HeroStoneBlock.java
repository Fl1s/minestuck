package com.mraof.minestuck.block.godtier;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;

import javax.annotation.Nullable;

public class HeroStoneBlock extends Block implements IGodTierBlock
{
	@Nullable
	private final EnumAspect aspect;
	private final boolean chiseled;

	public HeroStoneBlock(Properties properties, @Nullable EnumAspect aspect, boolean chiseled)
	{
		super(properties);
		this.aspect = aspect;
		this.chiseled = chiseled;
	}

	@Nullable
	@Override
	public EnumAspect getAspect()
	{
		return aspect;
	}

	public boolean isChiseled()
	{
		return chiseled;
	}

	/**
	 * Direct port of {@code BlockHeroStone#getAspectMapColor}'s switch, translated to 1.21's
	 * {@link MapColor} palette (the 1.12.2 stained-hardened-clay color constants have no 1:1
	 * equivalent, so the closest plain color was picked for each).
	 */
	public static MapColor getAspectMapColor(@Nullable EnumAspect aspect)
	{
		if(aspect == null)
			return MapColor.STONE;
		return switch(aspect)
		{
			case DOOM -> MapColor.COLOR_GREEN;
			case HOPE -> MapColor.SAND;
			case LIFE -> MapColor.STONE;
			case MIND -> MapColor.PODZOL;
			case RAGE -> MapColor.COLOR_PURPLE;
			case TIME -> MapColor.COLOR_RED;
			case VOID -> MapColor.COLOR_BLUE;
			case BLOOD -> MapColor.COLOR_RED;
			case HEART -> MapColor.COLOR_MAGENTA;
			case LIGHT -> MapColor.TERRACOTTA_WHITE;
			case SPACE -> MapColor.COLOR_BLACK;
			case BREATH -> MapColor.COLOR_LIGHT_BLUE;
		};
	}
}
