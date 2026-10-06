package com.mraof.minestuck.player.godtier.skill;

import com.mraof.minestuck.api.alchemy.GristTypes;
import com.mraof.minestuck.player.GristCache;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class EffectBuffBadge extends BadgeLevel
{
	public EffectBuffBadge(ResourceLocation id, int sortIndex, int requiredLevel)
	{
		super(id, sortIndex, requiredLevel);
	}
	
	@Override
	public boolean canUnlock(ServerLevel level, ServerPlayer player)
	{
			GristCache cache = GristCache.get(player);
		var cost = GristTypes.QUARTZ.get().amount(5000);
		List<com.mraof.minestuck.entity.FrogEntity> frogs = level.getEntitiesOfClass(com.mraof.minestuck.entity.FrogEntity.class, player.getBoundingBox().inflate(10));
		if(frogs.size() < 5 || !cache.canAfford(cost)) return false;
		
		for(int i = 0; i < 5; i++)
		{
			var frog = frogs.get(i);
			level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, frog.getX(), frog.getY() + 0.25, frog.getZ(), 30, 1, 0, 0, 0.2);
			frog.discard();
		}
		cache.tryTake(cost, null);
		return true;
	}
}
