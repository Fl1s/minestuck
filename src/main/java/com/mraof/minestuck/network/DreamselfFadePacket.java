package com.mraof.minestuck.network;

import com.mraof.minestuck.Minestuck;
import com.mraof.minestuck.client.DreamselfFade;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Starts the fade to black that hides a swap between the waking body and the dreamself.
 *
 * @param fadeOut ticks of fading to black (0 makes the screen black immediately)
 * @param hold    ticks of full black
 * @param fadeIn  ticks of fading back in
 * @param sound   the sound event to play at the start of the animation
 */
public record DreamselfFadePacket(int fadeOut, int hold, int fadeIn,
                                  ResourceLocation sound) implements MSPacket.PlayToClient
{
	public static final Type<DreamselfFadePacket> ID = new Type<>(Minestuck.id("dreamself_fade"));
	public static final StreamCodec<ByteBuf, DreamselfFadePacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, DreamselfFadePacket::fadeOut, ByteBufCodecs.VAR_INT, DreamselfFadePacket::hold, ByteBufCodecs.VAR_INT, DreamselfFadePacket::fadeIn, ResourceLocation.STREAM_CODEC, DreamselfFadePacket::sound, DreamselfFadePacket::new);
	
	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	
	@Override
	public void execute(IPayloadContext context)
	{
		DreamselfFade.start(this);
	}
}
