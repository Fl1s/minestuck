package com.mraof.minestuck.player.godtier.skill;

import com.mraof.minestuck.Minestuck;
import net.minecraft.resources.ResourceLocation;

public class BadgeConsort extends BadgeLevel
{
	public BadgeConsort(ResourceLocation id, int sortIndex, int requiredLevel)
	{
		super(id, sortIndex, requiredLevel);
	}
	
	@Override
	public ResourceLocation getTextureLocation()
	{
		return ResourceLocation.fromNamespaceAndPath(Minestuck.MOD_ID, "textures/gui/skills/badges/gift_of_gab_salamander.png");
	}
}
