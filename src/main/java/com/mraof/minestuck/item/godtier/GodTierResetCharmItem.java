package com.mraof.minestuck.item.godtier;

import com.mraof.minestuck.network.GodTierDataPacket;
import com.mraof.minestuck.player.PlayerData;
import com.mraof.minestuck.player.godtier.GodTierKarma;
import com.mraof.minestuck.player.godtier.GodTierStat;
import com.mraof.minestuck.player.godtier.GodTierState;
import com.mraof.minestuck.player.godtier.GodTierStats;
import com.mraof.minestuck.util.MSAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class GodTierResetCharmItem extends Item
{
	public GodTierResetCharmItem(Properties properties)
	{
		super(properties);
	}
	
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack stack = player.getItemInHand(hand);
		if(level.isClientSide() || !(player instanceof ServerPlayer serverPlayer))
			return InteractionResultHolder.pass(stack);
		
		PlayerData.get(serverPlayer).ifPresent(playerData -> {
			GodTierState state = playerData.getData(MSAttachments.GOD_TIER_STATE);
			if(state.isGodTier())
			{
				GodTierStats stats = playerData.getData(MSAttachments.GOD_TIER_STATS);
				GodTierKarma karma = playerData.getData(MSAttachments.GOD_TIER_KARMA);
				stats.resetAll();
				karma.reset();
				state.reset();
				stack.shrink(1);
				
				List<GodTierDataPacket.StatData> statData = new ArrayList<>();
				for(GodTierStat stat : GodTierStat.values())
					statData.add(new GodTierDataPacket.StatData(stat, 0, 0F));
				PacketDistributor.sendToPlayer(serverPlayer, new GodTierDataPacket(false, state.canGodTier(), false, statData, 0));
			}
		});
		
		return InteractionResultHolder.success(stack);
	}
}
