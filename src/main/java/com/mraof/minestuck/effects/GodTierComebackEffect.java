package com.mraof.minestuck.effects;

import com.mraof.minestuck.Minestuck;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Granted when a godtier player's conditional immortality triggers.
 */
@EventBusSubscriber(modid = Minestuck.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class GodTierComebackEffect extends MobEffect
{
	protected GodTierComebackEffect()
	{
		super(MobEffectCategory.BENEFICIAL, 0xFFD700);
		this.addAttributeModifier(Attributes.ATTACK_DAMAGE, ResourceLocation.fromNamespaceAndPath(Minestuck.MOD_ID, "god_tier_comeback"), 3.0, AttributeModifier.Operation.ADD_VALUE);
	}
	
	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier)
	{
		if(entity.getHealth() < entity.getMaxHealth())
			entity.heal(1.0F);
		return true;
	}
	
	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier)
	{
		if(duration < 60)
			return false;
		int interval = 20 >> amplifier;
		return interval <= 0 || duration % interval == 0;
	}
	
	@SubscribeEvent
	private static void onIncomingDamage(LivingIncomingDamageEvent event)
	{
		var effect = event.getEntity().getEffect(MSEffects.GOD_TIER_COMEBACK);
		if(effect == null || event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD))
			return;
		event.setAmount(event.getAmount() * Math.max(0, 25 - (effect.getAmplifier() + 1) * 5) / 25F);
	}
}
