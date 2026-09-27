package com.mraof.minestuck.item.godtier;

import com.mraof.minestuck.item.MSItems;
import com.mraof.minestuck.item.components.MSItemComponents;
import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import com.mraof.minestuck.player.PlayerIdentifier;
import com.mraof.minestuck.player.Title;
import com.mraof.minestuck.util.MSAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class GodTierKitItem extends Item
{
	public GodTierKitItem(Properties properties)
	{
		super(properties);
	}
	
	public void fillItemCategory(CreativeModeTab.Output output)
	{
		addCreativeTabEntries(output);
	}
	
	public static void addCreativeTabEntries(CreativeModeTab.Output output)
	{
		for(EnumClass heroClass : EnumClass.values())
			output.accept(generateKit(heroClass, null));
	}
	
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack stack = player.getItemInHand(hand);
		if(level.isClientSide() || !(player instanceof ServerPlayer serverPlayer))
			return InteractionResultHolder.pass(stack);
		
		Title title = getOrAssignTitle(stack, serverPlayer);
		if(title == null) return InteractionResultHolder.pass(stack);
		
		Item[] pieces = {MSItems.GOD_TIER_HOOD.get(), MSItems.GOD_TIER_SHIRT.get(), MSItems.GOD_TIER_PANTS.get(), MSItems.GOD_TIER_SHOES.get()};
		for(Item piece : pieces)
		{
			ItemStack armorStack = new ItemStack(piece);
			armorStack.set(MSItemComponents.GOD_TIER_TITLE.get(), title);
			if(!player.getInventory().add(armorStack)) player.drop(armorStack, false);
		}
		
		stack.shrink(1);
		return InteractionResultHolder.success(stack);
	}
	
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
	{
		Title title = stack.get(MSItemComponents.GOD_TIER_TITLE.get());
		tooltip.add(Component.translatable(Title.FORMAT, title != null ? title.heroClass().asTextComponent() : Component.translatable("title.unknown_class"), title != null ? title.heroAspect().asTextComponent() : Component.translatable("title.unknown_aspect")));
	}
	
	private static Title getOrAssignTitle(ItemStack stack, ServerPlayer player)
	{
		Title existing = stack.get(MSItemComponents.GOD_TIER_TITLE.get());
		if(existing != null) return existing;
		
		Title playerTitle = player.getData(MSAttachments.TITLE);
		if(playerTitle == null) return null;
		
		stack.set(MSItemComponents.GOD_TIER_TITLE.get(), playerTitle);
		return playerTitle;
	}
	
	public static ItemStack generateKit(EnumClass heroClass, EnumAspect aspect)
	{
		ItemStack stack = new ItemStack(MSItems.GOD_TIER_KIT.get());
		if(aspect != null) stack.set(MSItemComponents.GOD_TIER_TITLE.get(), new Title(heroClass, aspect));
		return stack;
	}
}
