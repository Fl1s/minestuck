package com.mraof.minestuck.network;

import com.mraof.minestuck.Minestuck;
import com.mraof.minestuck.api.alchemy.GristTypes;
import com.mraof.minestuck.client.gui.MSScreenFactories;
import com.mraof.minestuck.player.PlayerData;
import com.mraof.minestuck.player.godtier.GodTierState;
import com.mraof.minestuck.player.godtier.GodTierTickHandler;
import com.mraof.minestuck.player.godtier.skill.SkillRegistry;
import com.mraof.minestuck.util.MSAttachments;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packets for choosing the type of grist that the Hoard of the Alchemizer badge keeps stocked.
 */
public final class GodTierHoardPackets
{
	private GodTierHoardPackets()
	{
	}
	//asks the player to pick a type of grist
	public record OpenSelector() implements MSPacket.PlayToClient
	{
		public static final Type<OpenSelector> ID = new Type<>(Minestuck.id("god_tier/open_grist_hoard_selector"));
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenSelector> STREAM_CODEC = StreamCodec.unit(new OpenSelector());
		
		@Override
		public Type<? extends CustomPacketPayload> type()
		{
			return ID;
		}
		
		@Override
		public void execute(IPayloadContext context)
		{
			MSScreenFactories.displayGristHoardSelector();
		}
	}
	
	public record SelectType(ResourceLocation gristType) implements MSPacket.PlayToServer
	{
		public static final Type<SelectType> ID = new Type<>(Minestuck.id("god_tier/select_grist_hoard"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SelectType> STREAM_CODEC = StreamCodec.composite(ResourceLocation.STREAM_CODEC, SelectType::gristType, SelectType::new);
		
		@Override
		public Type<? extends CustomPacketPayload> type()
		{
			return ID;
		}
		
		@Override
		public void execute(IPayloadContext context, ServerPlayer player)
		{
			if(GristTypes.REGISTRY.get(gristType) == null)
				return;
			
			PlayerData.get(player).ifPresent(playerData -> {
				GodTierState state = playerData.getData(MSAttachments.GOD_TIER_STATE);
				if(!state.isGodTier() || !playerData.getData(MSAttachments.GOD_TIER_SKILLS).hasSkill(SkillRegistry.HOARD_OF_THE_ALCHEMIZER.get()))
					return;
				state.setGristHoard(gristType);
				GodTierTickHandler.sendSkillDataPacket(player, playerData);
			});
		}
	}
}
