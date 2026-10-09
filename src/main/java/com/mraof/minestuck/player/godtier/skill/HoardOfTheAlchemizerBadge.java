package com.mraof.minestuck.player.godtier.skill;

import com.mraof.minestuck.api.alchemy.GristType;
import com.mraof.minestuck.api.alchemy.GristTypes;
import com.mraof.minestuck.network.GodTierHoardPackets;
import com.mraof.minestuck.player.ClientPlayerData;
import com.mraof.minestuck.player.GristCache;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

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
			if(isBaseType(type) && !cache.canAfford(type.amount(REQUIRED_GRIST)))
				return false;
		for(GristType type : GristTypes.REGISTRY)
			if(isBaseType(type))
				cache.tryTake(type.amount(REQUIRED_GRIST), null);
		return true;
	}
	
	private static boolean isBaseType(GristType type)
	{
		return type.getId() != null && type.getId().getNamespace().equals(com.mraof.minestuck.Minestuck.MOD_ID);
	}
	
	@Override
	public void onBadgeUnlocked(ServerLevel level, ServerPlayer player)
	{
		PacketDistributor.sendToPlayer(player, new GodTierHoardPackets.OpenSelector());
	}
	
	@Override
	public Component getUnlockRequirements()
	{
		return Component.translatable(translationKey() + ".unlock", REQUIRED_GRIST);
	}
	
	@Override
	public Component getDisplayTooltip(@Nullable Player player)
	{
		Component type = Component.translatable(translationKey() + ".tooltip.any");
		ResourceLocation hoardId = ClientPlayerData.getGristHoard();
		if(player != null && player.level().isClientSide() && hoardId != null && ClientPlayerData.hasSkill(id()))
		{
			GristType hoard = GristTypes.REGISTRY.get(hoardId);
			if(hoard != null)
				type = hoard.getNameWithSuffix();
		}
		return Component.translatable(translationKey() + ".tooltip", type);
	}
	
	@Override
	public Component getDisplayTooltip()
	{
		return getDisplayTooltip(null);
	}
	
	@Override
	public boolean canDisable()
	{
		return false;
	}
}
