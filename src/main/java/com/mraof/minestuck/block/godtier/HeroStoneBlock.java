package com.mraof.minestuck.block.godtier;

import com.mraof.minestuck.network.GodTierMeditationPackets;
import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.PlayerData;
import com.mraof.minestuck.player.Title;
import com.mraof.minestuck.util.MSAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import javax.annotation.Nullable;

public class HeroStoneBlock extends Block implements IGodTierBlock
{
	@Nullable
	private final EnumAspect aspect;
	private final boolean chiseled;

	public HeroStoneBlock(Properties properties, @Nullable EnumAspect aspect, boolean chiseled)
	{
		super(properties);
		this.aspect = aspect;
		this.chiseled = chiseled;
	}

	@Nullable
	@Override
	public EnumAspect getAspect()
	{
		return aspect;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
	{
		if(!isChiseled() || level.isClientSide() || !(player instanceof ServerPlayer serverPlayer))
			return InteractionResult.PASS;
		
		PlayerData.get(serverPlayer).ifPresent(playerData -> {
			Title title = Title.getTitle(playerData).orElse(null);
			var stateData = playerData.getData(MSAttachments.GOD_TIER_STATE);
			if(title == null || !stateData.isGodTier())
				return;
			if(getAspect() != null && getAspect() != title.heroAspect())
				return;
			
			GodTierMeditationPackets.OpenScreen packet = new GodTierMeditationPackets.OpenScreen();
			net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer, packet);
		});
		
		return InteractionResult.SUCCESS;
	}
	
	public boolean isChiseled()
	{
		return chiseled;
	}

	/**
	 * Direct port of {@code BlockHeroStone#getAspectMapColor}'s switch, translated to 1.21's
	 * {@link MapColor} palette (the 1.12.2 stained-hardened-clay color constants have no 1:1
	 * equivalent, so the closest plain color was picked for each).
	 */
	public static MapColor getAspectMapColor(@Nullable EnumAspect aspect)
	{
		if(aspect == null)
			return MapColor.STONE;
		return switch(aspect)
		{
			case DOOM -> MapColor.COLOR_GREEN;
			case HOPE -> MapColor.SAND;
			case LIFE -> MapColor.STONE;
			case MIND -> MapColor.PODZOL;
			case RAGE -> MapColor.COLOR_PURPLE;
			case TIME -> MapColor.COLOR_RED;
			case VOID -> MapColor.COLOR_BLUE;
			case BLOOD -> MapColor.COLOR_RED;
			case HEART -> MapColor.COLOR_MAGENTA;
			case LIGHT -> MapColor.TERRACOTTA_WHITE;
			case SPACE -> MapColor.COLOR_BLACK;
			case BREATH -> MapColor.COLOR_LIGHT_BLUE;
		};
	}
}
