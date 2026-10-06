package com.mraof.minestuck.player.godtier.skill;

import com.mraof.minestuck.api.alchemy.GristTypes;
import com.mraof.minestuck.item.MSItems;
import com.mraof.minestuck.player.GristCache;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class KarmaBadge extends BadgeLevel
{
	public KarmaBadge(ResourceLocation id, int sortIndex, int requiredLevel)
	{
		super(id, sortIndex, requiredLevel);
	}
	
	@Override
	public boolean canUnlock(ServerLevel level, ServerPlayer player)
	{
		GristCache cache = GristCache.get(player);
		var cost = GristTypes.GOLD.get().amount(8000);
		
		// TODO add moonstone
		ItemStack requiredItems = new ItemStack(net.minecraft.world.item.Items.DIAMOND, 128);
		if(!consumeItems(player, requiredItems, false) || !cache.canAfford(cost))
			return false;
		
		consumeItems(player, requiredItems, true);
		cache.tryTake(cost, null);
		return true;
	}
}
