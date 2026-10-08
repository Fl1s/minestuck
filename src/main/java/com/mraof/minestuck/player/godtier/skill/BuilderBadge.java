package com.mraof.minestuck.player.godtier.skill;

import com.mraof.minestuck.computer.editmode.ServerEditHandler;
import com.mraof.minestuck.player.ClientPlayerData;
import com.mraof.minestuck.player.PlayerData;
import com.mraof.minestuck.player.GristCache;
import com.mraof.minestuck.api.alchemy.GristTypes;
import com.mraof.minestuck.util.MSAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The Badge of the Architecturally Adept lets a god tier player use all the tools of the editmode outside of editmode
 */
public class BuilderBadge extends BadgeLevel
{
	public static final int BUILD_GRIST_COST = 20000;
	
	public BuilderBadge(ResourceLocation id, int sortIndex, int requiredLevel)
	{
		super(id, sortIndex, requiredLevel);
	}
	
	@Override
	public boolean canUnlock(ServerLevel level, ServerPlayer player)
	{
		//TODO 1.12.2 also required the Battlepick of Zillydew, which doesn't exist in this version of the mod
		GristCache cache = GristCache.get(player);
		var cost = GristTypes.BUILD.get().amount(BUILD_GRIST_COST);
		if(!cache.canAfford(cost))
			return false;
		cache.tryTake(cost, null);
		return true;
	}
	
	/**
	 * Whether the player may use the builder tool right now.
	 */
	public static boolean isActive(Player player)
	{
		if(player instanceof ServerPlayer serverPlayer)
		{
			if(ServerEditHandler.isInEditmode(serverPlayer))
				return false;
			return PlayerData.get(serverPlayer).map(data ->
					data.getData(MSAttachments.GOD_TIER_STATE).isGodTier()
							&& data.getData(MSAttachments.GOD_TIER_SKILLS).isBadgeActive(SkillRegistry.BUILDER_BADGE.get())).orElse(false);
		}
		return ClientPlayerData.isGodTier() && ClientPlayerData.isSkillEnabled(SkillRegistry.BUILDER_BADGE.get().id());
	}
}
