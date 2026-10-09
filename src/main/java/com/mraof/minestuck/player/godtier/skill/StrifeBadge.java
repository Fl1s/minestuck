package com.mraof.minestuck.player.godtier.skill;

import com.mraof.minestuck.MinestuckConfig;
import com.mraof.minestuck.item.MSItems;
import com.mraof.minestuck.player.ClientPlayerData;
import com.mraof.minestuck.player.PlayerData;
import com.mraof.minestuck.util.MSAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The Medal of the Omni-Dextrous lets a god tier player use any weapon without having it assigned to a strife specibus.
 * It only does something (and is only offered) when restricted strife is enabled.
 * <p>
 * Based on the Strife Badge of Minestuck Universe by Cibernet.
 */
public class StrifeBadge extends BadgeLevel
{
	//the legendary weapons which are needed to unlock the badge
	private static final List<Supplier<? extends Item>> ZILLIUM_WEAPONS = List.of(
			MSItems.ZILLYHOO_HAMMER, MSItems.POPAMATIC_VRILLYHOO, MSItems.SCARLET_ZILLYHOO, MSItems.UNBREAKABLE_KATANA,
			MSItems.CUTLASS_OF_ZILLYWAIR, MSItems.PILLOW_TALK, MSItems.THISTLE_OF_ZILLYWICH);
	private static final int REQUIRED_WEAPONS = 5, REQUIRED_STRIFE_CARDS = 2;
	
	public StrifeBadge(ResourceLocation id, int sortIndex, int requiredLevel)
	{
		super(id, sortIndex, requiredLevel);
	}
	
	@Override
	public boolean canUnlock(ServerLevel level, ServerPlayer player)
	{
		List<ItemStack> required = new ArrayList<>();
		required.add(new ItemStack(MSItems.STRIFE_CARD.get(), REQUIRED_STRIFE_CARDS));
		
		int weapons = 0;
		for(Supplier<? extends Item> weapon : ZILLIUM_WEAPONS)
			if(weapons < REQUIRED_WEAPONS && hasItems(player, List.of(new ItemStack(weapon.get()))))
			{
				required.add(new ItemStack(weapon.get()));
				weapons++;
			}
		if(weapons < REQUIRED_WEAPONS || !hasItems(player, required))
			return false;
		
		consumeItems(player, required, true);
		return super.canUnlock(level, player);
	}
	
	@Override
	public boolean canAppearOnList(ServerLevel level, ServerPlayer player)
	{
		return isRelevant();
	}
	
	// Without restricted strife, any weapon can be used already.
	public static boolean isRelevant()
	{
		return MinestuckConfig.SERVER.restrictedStrife.get();
	}
	
	public static boolean isActive(Player player)
	{
		if(player instanceof ServerPlayer serverPlayer)
			return PlayerData.get(serverPlayer).map(data ->
					data.getData(MSAttachments.GOD_TIER_STATE).isGodTier()
							&& data.getData(MSAttachments.GOD_TIER_SKILLS).isBadgeActive(SkillRegistry.STRIFE_BADGE.get())).orElse(false);
		return ClientPlayerData.isGodTier() && ClientPlayerData.isSkillEnabled(SkillRegistry.STRIFE_BADGE.get().id());
	}
}
