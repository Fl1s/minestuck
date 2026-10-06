package com.mraof.minestuck.item.godtier;

import com.mraof.minestuck.entity.MSEntityTypes;
import com.mraof.minestuck.entity.item.LocatorEyeEntity;
import com.mraof.minestuck.skaianet.SburbPlayerData;
import com.mraof.minestuck.world.gen.structure.questbed.QuestBedPiece;
import com.mraof.minestuck.world.gen.structure.questbed.QuestBedPlacement;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class DenizenEyeItem extends Item
{
	public DenizenEyeItem(Properties properties)
	{
		super(properties);
	}
	
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack stack = player.getItemInHand(hand);
		if(level.isClientSide() || !(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel))
			return InteractionResultHolder.pass(stack);
		
		SburbPlayerData sburbData = SburbPlayerData.get(serverPlayer);
		if(!sburbData.hasEntered() || sburbData.getLandDimensionIfEntered() != level.dimension())
			return InteractionResultHolder.fail(stack);
		
		QuestBedPiece questBed = QuestBedPlacement.findQuestBedPiece(serverLevel);
		if(questBed == null)
			return InteractionResultHolder.fail(stack);
		
		LocatorEyeEntity eye = new LocatorEyeEntity(level, player.getX(), player.getY() + player.getBbHeight() / 2.0, player.getZ());
		eye.moveTowards(questBed.getOrigin(), 0.95F);
		level.addFreshEntity(eye);
		
		level.playSound(null, player.blockPosition(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.NEUTRAL, 0.5F, 0.4F / (player.getRandom().nextFloat() * 0.4F + 0.8F));
		
		if(!player.isCreative())
			stack.shrink(1);
		
		return InteractionResultHolder.success(stack);
	}
	
	@Override
	public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltip, TooltipFlag flag)
	{
		tooltip.add(Component.translatable("item.minestuck.denizen_eye.tooltip"));
	}
}
