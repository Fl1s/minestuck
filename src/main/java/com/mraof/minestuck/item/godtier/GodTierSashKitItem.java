package com.mraof.minestuck.item.godtier;

import com.mraof.minestuck.network.GodTierMeditationPackets;
import com.mraof.minestuck.player.PlayerData;
import com.mraof.minestuck.util.MSAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;

public class GodTierSashKitItem extends Item
{
	public GodTierSashKitItem(Properties properties)
	{
		super(properties);
	}
	
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack stack = player.getItemInHand(hand);
		if(!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.pass(stack);
		
		PlayerData.get(serverPlayer).ifPresent(playerData -> {
			if(playerData.getData(MSAttachments.GOD_TIER_STATE).isGodTier())
				PacketDistributor.sendToPlayer(serverPlayer, new GodTierMeditationPackets.OpenBadgeScreen());
		});
		
		return InteractionResultHolder.success(stack);
	}
	
	@Override
	public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltip, TooltipFlag flag)
	{
		tooltip.add(Component.translatable("item.minestuck.god_tier_sash_kit.tooltip"));
	}
}
