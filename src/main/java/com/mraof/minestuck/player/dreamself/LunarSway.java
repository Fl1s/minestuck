package com.mraof.minestuck.player.dreamself;

import com.mojang.serialization.Codec;
import com.mraof.minestuck.item.MSItems;
import com.mraof.minestuck.world.MSDimensions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * The moon that a player's dreamself belongs to.
 */
public enum LunarSway implements StringRepresentable
{
	PROSPIT("prospit"), DERSE("derse");
	
	public static final Codec<LunarSway> CODEC = StringRepresentable.fromEnum(LunarSway::values);
	
	/**
	 * While this is false, dreamselves spawn on Skaia (the Battlefield).
	 * Set this to true once the moon dimensions are properly generated!!!!
	 */
	// TODO: makr this as true
	public static final boolean USE_MOON_DIMENSIONS = true;
	
	private final String name;
	
	LunarSway(String name)
	{
		this.name = name;
	}
	
	@Override
	public String getSerializedName()
	{
		return name;
	}
	
	@Nullable
	public static LunarSway byName(String name)
	{
		for(LunarSway sway : values())
			if(sway.name.equals(name)) return sway;
		return null;
	}
	
	public static LunarSway random(RandomSource random)
	{
		return values()[random.nextInt(values().length)];
	}
	
	/**
	 * @return the dimension that the dreamselves of this sway are placed in
	 */
	public ResourceKey<Level> getDreamDimension()
	{
		if(!USE_MOON_DIMENSIONS) return MSDimensions.SKAIA;
		return this == PROSPIT ? MSDimensions.PROSPIT : MSDimensions.DERSE;
	}
	
	public Component getDisplayName()
	{
		return Component.translatable("minestuck.lunar_sway." + name);
	}
	
	/**
	 * @return a fresh copy of the default clothes
	 */
	public ItemStack createRobe(EquipmentSlot slot)
	{
		boolean prospit = this == PROSPIT;
		return switch(slot)
		{
			case HEAD -> new ItemStack(prospit ? MSItems.PROSPIT_CIRCLET.get() : MSItems.DERSE_CIRCLET.get());
			case CHEST -> new ItemStack(prospit ? MSItems.PROSPIT_SHIRT.get() : MSItems.DERSE_SHIRT.get());
			case LEGS -> new ItemStack(prospit ? MSItems.PROSPIT_PANTS.get() : MSItems.DERSE_PANTS.get());
			case FEET -> new ItemStack(prospit ? MSItems.PROSPIT_SHOES.get() : MSItems.DERSE_SHOES.get());
			default -> ItemStack.EMPTY;
		};
	}
}
