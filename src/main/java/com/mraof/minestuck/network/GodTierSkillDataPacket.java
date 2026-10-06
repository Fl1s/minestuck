package com.mraof.minestuck.network;

import com.mraof.minestuck.Minestuck;
import com.mraof.minestuck.player.ClientPlayerData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.Optional;

public record GodTierSkillDataPacket(List<SkillData> skills, Optional<ResourceLocation> masterBadge, List<ResourceLocation> techs, int badgeLimit) implements MSPacket.PlayToClient
{
	public static final Type<GodTierSkillDataPacket> ID = new Type<>(Minestuck.id("god_tier_skill_data"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GodTierSkillDataPacket> STREAM_CODEC = StreamCodec.composite(
			SkillData.STREAM_CODEC.apply(ByteBufCodecs.list()),
			GodTierSkillDataPacket::skills,
			ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC),
			GodTierSkillDataPacket::masterBadge,
			ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(3)),
			GodTierSkillDataPacket::techs,
			ByteBufCodecs.VAR_INT,
			GodTierSkillDataPacket::badgeLimit,
			GodTierSkillDataPacket::new
	);
	
	public record SkillData(ResourceLocation id, boolean enabled, boolean passiveEnabled)
	{
		public static final StreamCodec<RegistryFriendlyByteBuf, SkillData> STREAM_CODEC = StreamCodec.composite(
				ResourceLocation.STREAM_CODEC,
				SkillData::id,
				ByteBufCodecs.BOOL,
				SkillData::enabled,
				ByteBufCodecs.BOOL,
				SkillData::passiveEnabled,
				SkillData::new
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
