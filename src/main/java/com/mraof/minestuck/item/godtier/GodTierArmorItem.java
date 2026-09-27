package com.mraof.minestuck.item.godtier;

import com.mraof.minestuck.item.components.MSItemComponents;
import com.mraof.minestuck.player.Title;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

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
		String className = title != null ? title.heroClass().name().toLowerCase() : "knight";
		
		ResourceLocation self = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(this);
		return self.withPath("textures/models/armor/god_tier/" + className + ".png");
	}
}
