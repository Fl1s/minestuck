package com.mraof.minestuck.player.godtier.skill;

import com.mraof.minestuck.api.alchemy.GristTypes;
import com.mraof.minestuck.item.MSItems;
import com.mraof.minestuck.player.GristCache;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class PatchOfTheHoarderBadge extends BadgeLevel
{
	public PatchOfTheHoarderBadge(ResourceLocation id, int sortIndex, int requiredLevel)
	{
		super(id, sortIndex, requiredLevel);
	}
	
	@Override
	public boolean canUnlock(ServerLevel level, ServerPlayer player)
	{
		GristCache cache = GristCache.get(player);
		var cost = GristTypes.SHALE.get().amount(5000);
		
		if(!consumeItems(player, new ItemStack(MSItems.CAPTCHA_CARD.get(), 256), false))
			return false;
		if(!cache.canAfford(cost))
			return false;
		
		consumeItems(player, new ItemStack(MSItems.CAPTCHA_CARD.get(), 256), true);
		cache.tryTake(cost, null);
		return true;
	}
	
	@Override
	public boolean canDisable()
	{
		return false;
	}
	
	@Override
	public void onBadgeUnlocked(ServerLevel level, ServerPlayer player)
	{
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
	}
}
