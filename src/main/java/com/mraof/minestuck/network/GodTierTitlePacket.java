package com.mraof.minestuck.network;

import com.mraof.minestuck.Minestuck;
import com.mraof.minestuck.player.ClientPlayerData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record GodTierTitlePacket(Component title) implements MSPacket.PlayToClient
{
	public static final Type<GodTierTitlePacket> ID = new Type<>(Minestuck.id("god_tier/title"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GodTierTitlePacket> STREAM_CODEC = ComponentSerialization.STREAM_CODEC.map(GodTierTitlePacket::new, GodTierTitlePacket::title);
	
	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	
	@Override
	public void execute(net.neoforged.neoforge.network.handling.IPayloadContext context)
	{
		ClientPlayerData.setGodTierTitle(title);
	}
}
