package com.mraof.minestuck.player.godtier.skill;

import com.mraof.minestuck.item.MSItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class SkeletonKeyBadge extends BadgeLevel
{
	public SkeletonKeyBadge(ResourceLocation id, int sortIndex, int requiredLevel)
	{
		super(id, sortIndex, requiredLevel);
	}
	
	@Override
	public boolean canUnlock(ServerLevel level, ServerPlayer player)
	{
		List<Skeleton> skeletons = level.getEntitiesOfClass(Skeleton.class, player.getBoundingBox().inflate(10));
		if(skeletons.isEmpty())
			return false;
		if(!consumeItems(player, new ItemStack(MSItems.CAPTCHA_CARD.get(), 16), false))
			return false;
		
		consumeItems(player, new ItemStack(MSItems.CAPTCHA_CARD.get(), 16), true);
		Skeleton skeleton = skeletons.getFirst();
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, skeleton.getX(), skeleton.getY() + 0.25, skeleton.getZ(), 30, 1, 0, 0, 0.2);
		skeleton.discard();
		return true;
	}
}
