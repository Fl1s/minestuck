package com.mraof.minestuck.effects;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * An effect that continuously removes a set of other effects from whoever has it.
 * Used for the Decayproof and Mental Fortitude effects of the Effect Buff badge.
 * <p>
 * This is an adapted version of Cibernet's PotionCounter in Minestuck Universe.
 */
public class CounterEffect extends MobEffect
{
	private final List<Holder<MobEffect>> counteredEffects;
	
	public CounterEffect(MobEffectCategory category, int color, List<Holder<MobEffect>> counteredEffects)
	{
		super(category, color);
		this.counteredEffects = counteredEffects;
	}
	
	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier)
	{
		return true;
	}
	
	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier)
	{
		for(Holder<MobEffect> countered : counteredEffects)
			if(entity.hasEffect(countered))
				entity.removeEffect(countered);
		return true;
	}
}
