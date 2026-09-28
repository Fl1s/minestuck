package com.mraof.minestuck.player.godtier;

import com.mraof.minestuck.Minestuck;
import com.mraof.minestuck.player.PlayerData;
import com.mraof.minestuck.player.Title;
import com.mraof.minestuck.util.MSAttachments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.ChatFormatting;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = Minestuck.MOD_ID)
public final class GodTierKarmaHandler
{
	public static final TagKey<DamageType> GODPROOF = TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Minestuck.MOD_ID, "godproof"));
	
	private static final Set<UUID> CRITICAL_KILLS = new HashSet<>();
	
	private GodTierKarmaHandler()
	{
	}
	
	private static int minKarmaForRealDeath(ServerPlayer player)
	{
		return 20;
	}
	
	@SubscribeEvent
	public static void onPlayerDeath(LivingDeathEvent event)
	{
		if(!(event.getEntity() instanceof ServerPlayer target) || target.level().isClientSide()) return;
		
		Optional<PlayerData> targetDataOpt = PlayerData.get(target);
		if(targetDataOpt.isEmpty() || Title.getTitle(targetDataOpt.get()).isEmpty()) return;
		
		PlayerData targetData = targetDataOpt.get();
		GodTierState targetState = targetData.getData(MSAttachments.GOD_TIER_STATE);
		GodTierKarma targetKarma = targetData.getData(MSAttachments.GOD_TIER_KARMA);
		
		int totalKarma = targetKarma.getTotal();
		ServerPlayer attacker = event.getSource().getEntity() instanceof ServerPlayer p && p != target ? p : null;
		boolean pvpKill = attacker != null;
		boolean criticalKill = CRITICAL_KILLS.remove(target.getUUID());
		
		if(pvpKill)
		{
			var attackerDataOpt = PlayerData.get(attacker);
			if(attackerDataOpt.isPresent() && Title.getTitle(attackerDataOpt.get()).isPresent() && attackerDataOpt.get().getData(MSAttachments.GOD_TIER_STATE).isGodTier())
			{
				GodTierKarma attackerKarma = attackerDataOpt.get().getData(MSAttachments.GOD_TIER_KARMA);
				
				if(totalKarma >= -1 && totalKarma < 5) totalKarma = 5;
				else if(totalKarma > -5 && totalKarma < -1) totalKarma = -5;
				
				if(criticalKill)
				{
					totalKarma = (int) targetKarma.getTemp();
					attackerKarma.setStatic(attackerKarma.getStatic() - totalKarma * 2);
				} else attackerKarma.setStatic(attackerKarma.getStatic() - totalKarma);
			}
		}
		
		if(!targetState.isGodTier() || event.getSource().is(GODPROOF)) return;
		
		int minKarma = minKarmaForRealDeath(target);
		if(Math.abs(totalKarma) >= minKarma)
		{
			Component name = target.getDisplayName();
			if(totalKarma >= minKarma)
				target.server.getPlayerList().broadcastSystemMessage(Component.translatable("status.heroic_death", name).withStyle(ChatFormatting.GOLD), false);
			else
				target.server.getPlayerList().broadcastSystemMessage(Component.translatable("status.just_death", name).withStyle(ChatFormatting.DARK_PURPLE), false);
			targetKarma.reset();
		} else
		{
			target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
			target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0));
			if(target.level() instanceof ServerLevel level)
			{
				level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, target.getX(), target.getY() + 0.25, target.getZ(), 30, 1, 0, 0, 0.5);
				level.playSound(null, target.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
			}
			target.setHealth(20.0F);
			event.setCanceled(true);
			
			if(!pvpKill) targetKarma.setTemp(targetKarma.getTemp() - 15);
		}
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onPlayerDamage(LivingDamageEvent.Pre event)
	{
		if(!(event.getEntity() instanceof ServerPlayer target) || !(event.getSource().getEntity() instanceof ServerPlayer attacker) || attacker == target)
			return;
		
		var attackerDataOpt = PlayerData.get(attacker);
		var targetDataOpt = PlayerData.get(target);
		if(attackerDataOpt.isEmpty() || targetDataOpt.isEmpty() || Title.getTitle(attackerDataOpt.get()).isEmpty())
			return;
		
		int targetKarma = targetDataOpt.get().getData(MSAttachments.GOD_TIER_KARMA).getTotal();
		GodTierKarma attackerKarma = attackerDataOpt.get().getData(MSAttachments.GOD_TIER_KARMA);
		
		if(targetKarma >= 0) attackerKarma.setTemp(attackerKarma.getTemp() - Math.max(1, targetKarma / 5));
		else attackerKarma.setTemp(attackerKarma.getTemp() - Math.min(-1, targetKarma / 5));
		
		if(target.getHealth() - event.getNewDamage() <= 0 && target.getHealth() >= 10)
			CRITICAL_KILLS.add(target.getUUID());
	}
	
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Pre event)
	{
		if(!(event.getEntity() instanceof ServerPlayer player)) return;
		PlayerData.get(player).ifPresent(data -> data.getData(MSAttachments.GOD_TIER_KARMA).decay(1f));
	}
	
	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event)
	{
		CRITICAL_KILLS.remove(event.getEntity().getUUID());
	}
}