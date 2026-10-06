package com.mraof.minestuck.client.godtier;

import com.mraof.minestuck.Minestuck;
import com.mraof.minestuck.item.MSItems;
import com.mraof.minestuck.item.components.MSItemComponents;
import com.mraof.minestuck.item.godtier.GodTierArmorItem;
import com.mraof.minestuck.player.EnumClass;
import com.mraof.minestuck.player.Title;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jetbrains.annotations.NotNull;


@EventBusSubscriber(modid = Minestuck.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GodTierClientExtensions
{
	private GodTierClientExtensions()
	{
	}
	
	@SubscribeEvent
	public static void register(RegisterClientExtensionsEvent event)
	{
		event.registerItem(new IClientItemExtensions()
		{
			@Override
			public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original)
			{
				if(!(stack.getItem() instanceof GodTierArmorItem armor) || armor.getType().getSlot() != slot)
					return original;
				Title title = stack.get(MSItemComponents.GOD_TIER_TITLE.get());
				if(title == null) return original;
				GodTierArmorModel model = GodTierArmorModel.get(title.heroClass());
				if(model == null) return original;
				
				EnumClass legsClass = null;
				Title legsTitle = entity.getItemBySlot(EquipmentSlot.LEGS).get(MSItemComponents.GOD_TIER_TITLE.get());
				if(legsTitle != null) legsClass = legsTitle.heroClass();
				
				model.setup(entity, original, slot, Boolean.TRUE.equals(stack.get(MSItemComponents.GOD_TIER_HIDE_EXTRAS.get())), legsClass);
				return model;
			}
		}, MSItems.GOD_TIER_HOOD.get(), MSItems.GOD_TIER_SHIRT.get(), MSItems.GOD_TIER_PANTS.get(), MSItems.GOD_TIER_SHOES.get());
	}
}