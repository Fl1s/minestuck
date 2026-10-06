package com.mraof.minestuck.network;

import com.mraof.minestuck.Minestuck;
import com.mraof.minestuck.player.ClientPlayerData;
import com.mraof.minestuck.player.godtier.GodTierStat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record GodTierDataPacket(boolean godTier, boolean canGodTier, boolean climbedTheSpire, List<StatData> stats, int karma) implements MSPacket.PlayToClient
{
	public static final Type<GodTierDataPacket> ID = new Type<>(Minestuck.id("god_tier_data"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GodTierDataPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL,
			GodTierDataPacket::godTier,
			ByteBufCodecs.BOOL,
			GodTierDataPacket::canGodTier,
			ByteBufCodecs.BOOL,
			GodTierDataPacket::climbedTheSpire,
			StatData.STREAM_CODEC.apply(ByteBufCodecs.list()),
			GodTierDataPacket::stats,
			ByteBufCodecs.VAR_INT,
			GodTierDataPacket::karma,
			GodTierDataPacket::new
	);
	
	public record StatData(GodTierStat stat, int level, float xp)
	{
		public static final StreamCodec<RegistryFriendlyByteBuf, StatData> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.idMapper(GodTierStat::fromOrdinal, GodTierStat::ordinal),
				StatData::stat,
				ByteBufCodecs.VAR_INT,
				StatData::level,
				ByteBufCodecs.FLOAT,
				StatData::xp,
				StatData::new
		);
	}
	
	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	
	@Override
	public void execute(IPayloadContext context)
	{
		ClientPlayerData.handleDataPacket(this);
	}
}
