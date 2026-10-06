package com.mraof.minestuck.item.godtier;

import com.mraof.minestuck.client.godtier.GodTierArmorTextures;
import com.mraof.minestuck.item.components.MSItemComponents;
import com.mraof.minestuck.player.EnumClass;
import com.mraof.minestuck.player.Title;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public class GodTierArmorItem extends ArmorItem
{
	public GodTierArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties)
	{
		super(material, type, properties);
	}
	
	@Nullable
	@Override
	public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel)
	{
		Title title = stack.get(MSItemComponents.GOD_TIER_TITLE.get());
		if(title == null) return null;
		return GodTierArmorTextures.get(title.heroClass(), title.heroAspect());
	}
	
	private boolean hasHideableExtras(ItemStack stack)
	{
		Title title = stack.get(MSItemComponents.GOD_TIER_TITLE.get());
		if(title == null) return false;
		return (title.heroClass() == EnumClass.ROGUE && getType() == Type.HELMET) || (title.heroClass() == EnumClass.LORD && getType() == Type.CHESTPLATE);
	}
	
	@Override
	public Component getName(ItemStack stack)
	{
		Title title = stack.get(MSItemComponents.GOD_TIER_TITLE.get());
		if(title == null)
			return super.getName(stack);
		
		String slotKey = switch(getType())
		{
			case HELMET -> "hood";
			case CHESTPLATE -> "shirt";
			case LEGGINGS -> "pants";
			case BOOTS -> "shoes";
			default -> "armor";
		};
		return Component.translatable("item.minestuck.god_tier." + slotKey + ".named", title.heroClass().asTextComponent(), title.heroAspect().asTextComponent());
	}
	
	@Override
	public boolean isFoil(ItemStack stack)
	{
		return stack.has(MSItemComponents.GOD_TIER_TITLE.get());
	}
	
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack stack = player.getItemInHand(hand);
		if(player.isShiftKeyDown() && hasHideableExtras(stack))
		{
			boolean hidden = Boolean.TRUE.equals(stack.get(MSItemComponents.GOD_TIER_HIDE_EXTRAS.get()));
			stack.set(MSItemComponents.GOD_TIER_HIDE_EXTRAS.get(), !hidden);
			return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
		}
		return super.use(level, player, hand);
	}
	
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
	{
		super.appendHoverText(stack, context, tooltip, flag);
		if(hasHideableExtras(stack) && Boolean.TRUE.equals(stack.get(MSItemComponents.GOD_TIER_HIDE_EXTRAS.get())))
			tooltip.add(Component.translatable(getType() == Type.HELMET ? "item.minestuck.god_tier.hidden_extras.rogue" : "item.minestuck.god_tier.hidden_extras.lord"));
	}
}