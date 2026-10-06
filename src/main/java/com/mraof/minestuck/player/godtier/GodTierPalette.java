package com.mraof.minestuck.player.godtier;

import com.mraof.minestuck.player.EnumAspect;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

public final class GodTierPalette
{
	private GodTierPalette()
	{
	}
	
	public static final int[] DEFAULT = colors(0xB9B9B9, 0x484848, 0x979797, 0xAEFE00, 0xC50128, 0xAEFE00, 0x5CCB00);
	
	private static final Map<EnumAspect, int[]> PALETTES = new EnumMap<>(EnumAspect.class);
	
	static
	{
		PALETTES.put(EnumAspect.BLOOD, colors(0x3D190A, 0x290704, 0x5C2913, 0xB90F15, 0x583980));
		PALETTES.put(EnumAspect.BREATH, colors(0x0187EB, 0x0053F1, 0x006EE8, 0x0FE1FE, 0xFFED08));
		PALETTES.put(EnumAspect.DOOM, colors(0x204121, 0x242E26, 0x1C3823, 0x000000, 0xD6BB78));
		PALETTES.put(EnumAspect.HEART, colors(0x6E0E2E, 4395044, 5575722, 0xBD1764, 1394208));
		PALETTES.put(EnumAspect.HOPE, colors(0xFEDA82, 16693286, 16172362, 0xFDFDFD, 5796184, 0xFEC433, 0xFFE094));
		PALETTES.put(EnumAspect.LIFE, colors(13419444, 6313026, 8022868, 7783246, 996473, 0x3E990C, 0x76C24E));
		PALETTES.put(EnumAspect.LIGHT, colors(0xF98100, 16402176, 15164928, 16251726, 1106175));
		PALETTES.put(EnumAspect.MIND, colors(4039514, 3695654, 6403645, 458697, 8133949));
		PALETTES.put(EnumAspect.RAGE, colors(0x442769, 2496058, 5060968, 0x9C4DAD, 3347210, 0x7C43B1, 0x9B4EAA));
		PALETTES.put(EnumAspect.SPACE, colors(0x0F0E0E, 0x2F2F2F, 0x1D1D1D, 0xFFFFFF, 12256514, 0x848484, 0x4D4D4D));
		PALETTES.put(EnumAspect.TIME, colors(11996430, 5309958, 9311510, 16720134, 1973794, 16720134, 0xAB4032));
		PALETTES.put(EnumAspect.VOID, colors(9062, 2050176, 1204608, 571, 71038341 & 0xFFFFFF, 0x004CB0, 0x043476));
	}
	
	public static int[] get(@Nullable EnumAspect aspect)
	{
		if(aspect == null) return DEFAULT;
		return PALETTES.getOrDefault(aspect, DEFAULT);
	}
	
	public static int get(@Nullable EnumAspect aspect, Slot slot)
	{
		return get(aspect)[slot.ordinal()];
	}
	
	public enum Slot
	{SHIRT, PRIMARY, SECONDARY, SYMBOL, SHOES, DETAIL_PRIMARY, DETAIL_SECONDARY}
	
	private static int[] colors(int shirt, int primary, int secondary, int symbol, int shoes)
	{
		return colors(shirt, primary, secondary, symbol, shoes, symbol, shirt);
	}
	
	private static int[] colors(int shirt, int primary, int secondary, int symbol, int shoes, int detailA, int detailB)
	{
		return new int[]{shirt, primary, secondary, symbol, shoes, detailA, detailB, 0xFFFFFF};
	}
}