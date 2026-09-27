package com.mraof.minestuck.block.godtier;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class HeroStoneBlockItem extends BlockItem
{
	public HeroStoneBlockItem(Block block, Properties properties)
	{
		super(block, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
	{
		super.appendHoverText(stack, context, tooltip, flag);

		EnumAspect aspect = ((IGodTierBlock) this.getBlock()).getAspect();
		tooltip.add(aspect != null ? aspect.asTextComponent() : Component.translatable("title.unknown_aspect"));
	}
}
