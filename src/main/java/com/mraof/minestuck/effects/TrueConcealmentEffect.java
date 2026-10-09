package com.mraof.minestuck.effects;

import com.mraof.minestuck.Minestuck;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/**
 * Makes the entity impossible to see so other mobs can't spot it, and it isn't rendered (see {@code TrueConcealmentRender}).
 * Any glowing effect cancels it.
 * <p>
 * This is an adapted version of Cibernet's PotionConceal in Minestuck Universe.
 */
@EventBusSubscriber(modid = Minestuck.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class TrueConcealmentEffect extends MobEffect
{
	public TrueConcealmentEffect()
	{
		super(MobEffectCategory.BENEFICIAL, 9062);
	}
	
	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier)
	{
		return true;
	}
	
	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier)
	{
		if(entity.hasEffect(MobEffects.GLOWING))
			entity.removeEffect(MSEffects.TRUE_CONCEALMENT);
		return true;
	}
	
	@SubscribeEvent
	private static void onVisibility(LivingEvent.LivingVisibilityEvent event)
	{
		if(event.getEntity().hasEffect(MSEffects.TRUE_CONCEALMENT))
			event.modifyVisibility(0);
	}
}
