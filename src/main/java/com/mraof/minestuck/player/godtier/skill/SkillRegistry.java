package com.mraof.minestuck.player.godtier.skill;

import com.mraof.minestuck.Minestuck;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public final class SkillRegistry
{
	public static final ResourceKey<Registry<Skill>> REGISTRY_KEY = ResourceKey.createRegistryKey(Minestuck.id("skill"));
	private static final DeferredRegister<Skill> REGISTER = DeferredRegister.create(REGISTRY_KEY, Minestuck.MOD_ID);
	public static final Registry<Skill> REGISTRY = REGISTER.makeRegistry(builder -> builder.sync(true));
	
	public static final Supplier<Badge> GIFT_OF_GAB = REGISTER.register("gift_of_gab", () -> new BadgeConsort(Skill.defaultId("gift_of_gab"), 0, 2));
	public static final Supplier<Badge> SKELETON_KEY = REGISTER.register("skeleton_key", () -> new SkeletonKeyBadge(Skill.defaultId("skeleton_key"), 10, 3));
	public static final Supplier<Badge> PATCH_OF_THE_HOARDER = REGISTER.register("patch_of_the_hoarder", () -> new PatchOfTheHoarderBadge(Skill.defaultId("patch_of_the_hoarder"), 20, 3));
	public static final Supplier<Badge> HOARD_OF_THE_ALCHEMIZER = REGISTER.register("hoard_of_the_alchemizer", () -> new HoardOfTheAlchemizerBadge(Skill.defaultId("hoard_of_the_alchemizer"), 30, 4));
	public static final Supplier<Badge> BUILDER_BADGE = REGISTER.register("builder_badge", () -> new BuilderBadge(Skill.defaultId("builder_badge"), 40, 7));
	public static final Supplier<Badge> STRIFE_BADGE = REGISTER.register("strife_badge", () -> new StrifeBadge(Skill.defaultId("strife_badge"), 50, 7));
	public static final Supplier<Badge> REVENANTS_RETALIATION = REGISTER.register("revenants_retaliation", () -> new RevenantsRetaliationBadge(Skill.defaultId("revenants_retaliation"), 60, 4));
	public static final Supplier<Badge> EFFECT_BUFF = REGISTER.register("effect_buff", () -> new EffectBuffBadge(Skill.defaultId("effect_buff"), 70, 4));
	public static final Supplier<Badge> KARMA = REGISTER.register("karma", () -> new KarmaBadge(Skill.defaultId("karma"), 80, 5));
	public static final Supplier<Badge> BADGE_PAGE = REGISTER.register("badge_page", () -> new BadgePage(Skill.defaultId("badge_page"), 90));
	public static final Supplier<Badge> BADGE_OVERLORD = REGISTER.register("badge_overlord", () -> new BadgeOverlord(Skill.defaultId("badge_overlord"), 100));
	public static final Supplier<MasterBadge> MASTER_BADGE_MIGHTY = REGISTER.register("master_badge_mighty", () -> new MasterBadge(Skill.defaultId("master_badge_mighty"), 0, 3, 80, 0.4F, 40));
	public static final Supplier<MasterBadge> MASTER_BADGE_BRAVE = REGISTER.register("master_badge_brave", () -> new MasterBadge(Skill.defaultId("master_badge_brave"), 10, 3, 80, 0.2F, 40));
	public static final Supplier<MasterBadge> MASTER_BADGE_WISE = REGISTER.register("master_badge_wise", () -> new MasterBadge(Skill.defaultId("master_badge_wise"), 20, 3, 80, 0.4F, 60));
	
	
	
	@Nullable
	public static Skill get(ResourceLocation id)
	{
		return REGISTRY.get(id);
	}
	
	public static void register(IEventBus eventBus)
	{
		REGISTER.register(eventBus);
	}
}
