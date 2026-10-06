package com.mraof.minestuck.player.godtier.skill;

import com.mraof.minestuck.api.alchemy.GristType;
import com.mraof.minestuck.api.alchemy.GristTypes;
import com.mraof.minestuck.player.GristCache;
import com.mraof.minestuck.player.PlayerData;
import com.mraof.minestuck.util.MSAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class HoardOfTheAlchemizerBadge extends BadgeLevel
{
	private static final int REQUIRED_GRIST = 2000;
	
	public HoardOfTheAlchemizerBadge(ResourceLocation id, int sortIndex, int requiredLevel)
	{
		super(id, sortIndex, requiredLevel);
	}
	
	@Override
	public boolean canUnlock(ServerLevel level, ServerPlayer player)
	{
		GristCache cache = GristCache.get(player);
		
		for(GristType type : GristTypes.REGISTRY)
			if(!cache.canAfford(type.amount(REQUIRED_GRIST)))
				return false;
		
		for(GristType type : GristTypes.REGISTRY)
			cache.tryTake(type.amount(REQUIRED_GRIST), null);
		return true;
	}
	
	@Override
	public void onBadgeUnlocked(ServerLevel level, ServerPlayer player)
	{
		PlayerData.get(player).ifPresent(playerData ->
				playerData.getData(MSAttachments.GOD_TIER_STATE).setGristHoard(GristTypes.BUILD.get().getId()));
	}
	
	@Override
	public boolean canDisable()
	{
		return false;
	}
}
