package com.mraof.minestuck.item.godtier;

import com.mraof.minestuck.player.godtier.GodTierReset;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Hold right click for 3 secs to give up your god tier status
 */
@ParametersAreNonnullByDefault
public class GodTierResetCharmItem extends Item
{
	private static final int USE_DURATION = 60;
	
	public GodTierResetCharmItem(Properties properties)
	{
		super(properties);
	}
	
	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity)
	{
		return USE_DURATION;
	}
	
	@Override
	public UseAnim getUseAnimation(ItemStack stack)
	{
		return UseAnim.BOW;
	}
	
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(player.getItemInHand(hand));
	}
	
	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity)
	{
		if(!level.isClientSide() && entity instanceof ServerPlayer player && GodTierReset.reset(player) && !player.isCreative())
			stack.shrink(1);
		return stack;
	}
}
