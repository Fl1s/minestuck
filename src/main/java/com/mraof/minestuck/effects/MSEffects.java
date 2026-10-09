package com.mraof.minestuck.effects;

import java.util.List;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectCategory;
import com.mraof.minestuck.Minestuck;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * This is an adapted version of Cibernet's code in Minestuck Universe, credit goes to him!
 */
public class MSEffects
{
	public static final DeferredRegister<MobEffect> REGISTER = DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, Minestuck.MOD_ID);
	
	public static final DeferredHolder<MobEffect, CreativeShockEffect> CREATIVE_SHOCK = REGISTER.register("creative_shock", CreativeShockEffect::new);
	public static final DeferredHolder<MobEffect, EarthboundEffect> EARTHBOUND = REGISTER.register("earthbound", EarthboundEffect::new);
	public static final DeferredHolder<MobEffect, GodTierComebackEffect> GOD_TIER_COMEBACK = REGISTER.register("god_tier_comeback", GodTierComebackEffect::new);
	public static final DeferredHolder<MobEffect, CounterEffect> DECAYPROOF = REGISTER.register("decayproof", () -> new CounterEffect(MobEffectCategory.BENEFICIAL, 0xFEDA82, List.of(MobEffects.WITHER, MobEffects.POISON)));
	public static final DeferredHolder<MobEffect, CounterEffect> MENTAL_FORTITUDE = REGISTER.register("mental_fortitude", () -> new CounterEffect(MobEffectCategory.BENEFICIAL, 458697, List.of(MobEffects.BLINDNESS, MobEffects.CONFUSION)));
	public static final DeferredHolder<MobEffect, TrueConcealmentEffect> TRUE_CONCEALMENT = REGISTER.register("true_concealment", TrueConcealmentEffect::new);
	public static final DeferredHolder<MobEffect, GodTierLockEffect> GOD_TIER_LOCK = REGISTER.register("god_tier_lock", GodTierLockEffect::new);
	
	public static final DeferredHolder<MobEffect, SuspicionEffect> SUSPICION = REGISTER.register("suspicion", SuspicionEffect::new);
	
	public static final DeferredHolder<MobEffect, SoporSicknessEffect> SOPOR_SICKNESS = REGISTER.register("sopor_sickness", SoporSicknessEffect::new);
}