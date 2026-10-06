package com.mraof.minestuck.player.godtier;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.mraof.minestuck.Minestuck;

import javax.annotation.Nullable;

public enum GodTierStat implements StringRepresentable
{
	GENERAL("general", 1.0, null, null),
	DEFENSE("defense", 0.4, Attributes.ARMOR_TOUGHNESS, AttributeModifier.Operation.ADD_VALUE),
	ATTACK("attack", 0.02, Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
	LUCK("luck", 0.5, Attributes.LUCK, AttributeModifier.Operation.ADD_VALUE),
	SPEED("speed", 0.05, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	
	private final String name;
	private final double attributeMod;
	@Nullable
	private final Holder<Attribute> attribute;
	@Nullable
	private final AttributeModifier.Operation operation;
	private final ResourceLocation modifierId;
	
	GodTierStat(String name, double attributeMod, @Nullable Holder<Attribute> attribute, @Nullable AttributeModifier.Operation operation)
	{
		this.name = name;
		this.attributeMod = attributeMod;
		this.attribute = attribute;
		this.operation = operation;
		this.modifierId = ResourceLocation.fromNamespaceAndPath(Minestuck.MOD_ID, "god_tier_" + name);
	}
	
	@Override
	public String getSerializedName() { return name; }
	
	public double attributeMod() { return attributeMod; }
	
	@Nullable
	public Holder<Attribute> attribute() { return attribute; }
	
	@Nullable
	public AttributeModifier.Operation operation() { return operation; }
	
	public ResourceLocation modifierId() { return modifierId; }
	
	public static GodTierStat fromOrdinal(int ordinal)
	{
		GodTierStat[] values = values();
		return ordinal >= 0 && ordinal < values.length ? values[ordinal] : GENERAL;
	}
	
	public boolean hasAttribute() { return attribute != null; }
}