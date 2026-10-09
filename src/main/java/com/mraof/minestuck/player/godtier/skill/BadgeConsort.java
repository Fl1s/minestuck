package com.mraof.minestuck.player.godtier.skill;

import com.mraof.minestuck.player.ClientPlayerData;
import com.mraof.minestuck.Minestuck;
import net.minecraft.resources.ResourceLocation;

public class BadgeConsort extends BadgeLevel
{
	private static final java.util.Set<String> KNOWN_CONSORTS = java.util.Set.of("salamander", "turtle", "nakagator", "iguana");
	
	public BadgeConsort(ResourceLocation id, int sortIndex, int requiredLevel)
	{
		super(id, sortIndex, requiredLevel);
	}
	
	@Override
	public ResourceLocation getTextureLocation()
	{
		String consort = ClientPlayerData.getConsortType();
		if(!KNOWN_CONSORTS.contains(consort))
			consort = "salamander";
		return ResourceLocation.fromNamespaceAndPath(Minestuck.MOD_ID, "textures/gui/skills/badges/gift_of_gab_" + consort + ".png");
	}
}
