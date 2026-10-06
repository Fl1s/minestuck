package com.mraof.minestuck.player.godtier.skill;

import com.mraof.minestuck.api.alchemy.GristTypes;
import com.mraof.minestuck.player.GristCache;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;

import java.util.List;

public class RevenantsRetaliationBadge extends BadgeLevel
{
	public RevenantsRetaliationBadge(ResourceLocation id, int sortIndex, int requiredLevel)
	{
		super(id, sortIndex, requiredLevel);
	}
	
	@Override
	public boolean canUnlock(ServerLevel level, ServerPlayer player)
	{
		GristCache cache = GristCache.get(player);
		var cost = GristTypes.DIAMOND.get().amount(10000);
		List<Creeper> creepers = level.getEntitiesOfClass(Creeper.class, player.getBoundingBox().inflate(10));
		
		if(creepers.isEmpty() || !cache.canAfford(cost))
			return false;
		
		Creeper creeper = creepers.getFirst();
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, creeper.getX(), creeper.getY() + 0.25, creeper.getZ(), 30, 1, 0, 0, 0.2);
		creeper.discard();
		cache.tryTake(cost, null);
		return true;
	}
}
