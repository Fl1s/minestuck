package com.mraof.minestuck.player.godtier;

import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import com.mraof.minestuck.player.godtier.skill.BuilderBadge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import com.mraof.minestuck.Minestuck;
import com.mraof.minestuck.player.PlayerData;
import com.mraof.minestuck.player.godtier.skill.MasterBadge;
import com.mraof.minestuck.player.godtier.skill.SkillRegistry;
import com.mraof.minestuck.util.MSAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Optional;

@EventBusSubscriber(modid = Minestuck.MOD_ID)
public final class GodTierBadgeEventHandler
{
	private GodTierBadgeEventHandler() {}
	
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onIncomingDamage(LivingIncomingDamageEvent event)
	{
		if(event.getEntity().level().isClientSide()) return;
		
		ServerPlayer attacker = event.getSource().getEntity() instanceof ServerPlayer player ? player : null;
		ServerPlayer target = event.getEntity() instanceof ServerPlayer player ? player : null;
		
		if(target != null && hasBadge(target, SkillRegistry.MASTER_BADGE_BRAVE.get())
				&& !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
				&& target.getRandom().nextDouble() * 100 < ((MasterBadge) SkillRegistry.MASTER_BADGE_BRAVE.get()).statNumber(target))
			event.setAmount(0);
		
		if(attacker != null && hasBadge(attacker, SkillRegistry.MASTER_BADGE_MIGHTY.get())
				&& attacker.getRandom().nextDouble() * 100 < ((MasterBadge) SkillRegistry.MASTER_BADGE_MIGHTY.get()).statNumber(attacker))
			event.setAmount(event.getAmount() * 2);
	}
	
	/**
	 * God tier DEFENSE reduces incoming damage, on top of the armor toughness it grants.
	 * As in 1.12.2, damage that ignores armor is not reduced unless it is fire, magic or hitting a wall at speed.
	 */
	@SubscribeEvent
	public static void onPlayerHurt(LivingIncomingDamageEvent event)
	{
		if(!(event.getEntity() instanceof ServerPlayer target) || target.level().isClientSide()) return;
		DamageSource source = event.getSource();
		boolean magic = source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC) || source.is(DamageTypes.DRAGON_BREATH);
		if(source.is(DamageTypeTags.BYPASSES_ARMOR) && !source.is(DamageTypeTags.IS_FIRE) && !magic && !source.is(DamageTypes.FLY_INTO_WALL))
			return;
		
		PlayerData.get(target).ifPresent(data -> {
			if(!data.getData(MSAttachments.GOD_TIER_STATE).isGodTier()) return;
			double multiplier = 1.0;
			var skills = data.getData(MSAttachments.GOD_TIER_SKILLS);
			if(skills.isBadgeActive(SkillRegistry.BADGE_PAGE.get())) multiplier *= 2.0;
			if(skills.isBadgeActive(SkillRegistry.BADGE_OVERLORD.get())) multiplier *= 3.0;
			double reduction = data.getData(MSAttachments.GOD_TIER_STATS).getLevel(GodTierStat.DEFENSE) * 0.002 * multiplier;
			if(reduction <= 0) return;
			
			if(magic)
				reduction *= 0.5;
			if(source.is(DamageTypes.CACTUS) || source.is(DamageTypeTags.IS_FIRE))
				reduction *= 0.25;
			
			event.setAmount((float) (event.getAmount() * Math.max(0.0, 1.0 - reduction)));
		});
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDrops(LivingDropsEvent event)
	{
		ServerPlayer player = event.getSource().getEntity() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
		if(player == null || !hasBadge(player, SkillRegistry.MASTER_BADGE_WISE.get())) return;
		
		MasterBadge wise = (MasterBadge) SkillRegistry.MASTER_BADGE_WISE.get();
		if(player.getRandom().nextDouble() * 100 >= wise.statNumber(player)) return;
		
		for(ItemEntity drop : event.getDrops())
		{
			ItemStack stack = drop.getItem();
			stack.setCount(Math.min(stack.getCount() * 4, stack.getMaxStackSize()));
			drop.setItem(stack);
		}
	}
	
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event)
	{
		if(event.getEntity().level().isClientSide()) return;
		if(event.getEntity().getMainHandItem().getItem() instanceof BlockItem && BuilderBadge.isActive(event.getEntity()))
			event.setCanceled(true);
	}
	
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event)
	{
		if(event.getEntity().level().isClientSide()) return;
		if(event.getItemStack().getItem() instanceof BlockItem && BuilderBadge.isActive(event.getEntity()))
		{
			event.setCanceled(true);
		}
	}
	
	@SubscribeEvent
	public static void onSkeletonKeyUse(PlayerInteractEvent.RightClickBlock event)
	{
		if(!(event.getEntity() instanceof ServerPlayer player) || event.getHand() != InteractionHand.MAIN_HAND || !event.getItemStack().isEmpty())
			return;
		if(!hasBadge(player, SkillRegistry.SKELETON_KEY.get()))
			return;
		
		Level level = event.getLevel();
		BlockPos pos = event.getPos();
		BlockState state = level.getBlockState(pos);
		boolean used = false;
		
		if(state.is(Blocks.IRON_DOOR) && state.getBlock() instanceof DoorBlock door)
		{
			door.setOpen(player, level, state, pos, !state.getValue(DoorBlock.OPEN));
			used = true;
		} else if(state.is(Blocks.IRON_TRAPDOOR))
		{
			boolean open = !state.getValue(TrapDoorBlock.OPEN);
			level.setBlock(pos, state.setValue(TrapDoorBlock.OPEN, open), 2);
			if(state.getValue(TrapDoorBlock.WATERLOGGED))
				level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
			level.playSound(null, pos, open ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.1F + 0.9F);
			level.gameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
			used = true;
		} else if(state.is(Blocks.END_PORTAL_FRAME) && !state.getValue(EndPortalFrameBlock.HAS_EYE))
		{
			fillEndPortalFrame(level, pos, state);
			used = true;
		} else if(state.is(Blocks.OBSIDIAN) && event.getFace() == Direction.UP)
		{
			Optional<PortalShape> shape = PortalShape.findEmptyPortalShape(level, pos.above(), Direction.Axis.X);
			if(shape.isPresent())
			{
				shape.get().createPortalBlocks();
				level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.4F + 0.8F);
				used = true;
			}
		}
		
		if(used)
		{
			event.setCancellationResult(InteractionResult.SUCCESS);
			event.setCanceled(true);
		}
	}
	
	//the same as using an eye of ender on the frame
	private static void fillEndPortalFrame(Level level, BlockPos pos, BlockState state)
	{
		level.setBlock(pos, state.setValue(EndPortalFrameBlock.HAS_EYE, true), 2);
		level.updateNeighbourForOutputSignal(pos, Blocks.END_PORTAL_FRAME);
		level.levelEvent(1503, pos, 0);
		BlockPattern.BlockPatternMatch match = EndPortalFrameBlock.getOrCreatePortalShape().find(level, pos);
		if(match != null)
		{
			BlockPos portalPos = match.getFrontTopLeft().offset(-3, 0, -3);
			for(int x = 0; x < 3; x++)
				for(int z = 0; z < 3; z++)
					level.setBlock(portalPos.offset(x, 0, z), Blocks.END_PORTAL.defaultBlockState(), 2);
			level.globalLevelEvent(1038, portalPos.offset(1, 0, 1), 0);
		}
	}
	
	private static boolean hasBadge(ServerPlayer player, com.mraof.minestuck.player.godtier.skill.Skill skill)
	{
		return PlayerData.get(player)
				.map(data -> data.getData(MSAttachments.GOD_TIER_SKILLS).isBadgeActive(skill))
				.orElse(false);
	}
}
