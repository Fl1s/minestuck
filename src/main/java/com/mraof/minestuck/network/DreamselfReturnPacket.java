package com.mraof.minestuck.network;

import com.mraof.minestuck.Minestuck;
import com.mraof.minestuck.player.dreamself.DreamselfHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent when the player presses the key for returning from the dreamself to the waking body.
 */
public record DreamselfReturnPacket() implements MSPacket.PlayToServer
{
	public static final Type<DreamselfReturnPacket> ID = new Type<>(Minestuck.id("dreamself_return"));
	public static final StreamCodec<FriendlyByteBuf, DreamselfReturnPacket> STREAM_CODEC = StreamCodec.unit(new DreamselfReturnPacket());
	
	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	
	@Override
	public void execute(IPayloadContext context, ServerPlayer player)
	{
		DreamselfHandler.requestReturnToBody(player);
	}
}
