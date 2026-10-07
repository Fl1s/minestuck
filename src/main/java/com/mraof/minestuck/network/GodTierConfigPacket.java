package com.mraof.minestuck.network;

import com.mraof.minestuck.Minestuck;
import com.mraof.minestuck.MinestuckConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record GodTierConfigPacket(int maxGodTier, int godTierXpThreshold, int godTierBadgeSlots,
                                  boolean godTierMasterControl, boolean aspectEffects,
                                  boolean multiAspectUnlocks) implements MSPacket.PlayToClient
{
	public static final Type<GodTierConfigPacket> ID = new Type<>(Minestuck.id("god_tier/config"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GodTierConfigPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, GodTierConfigPacket::maxGodTier, ByteBufCodecs.VAR_INT, GodTierConfigPacket::godTierXpThreshold, ByteBufCodecs.VAR_INT, GodTierConfigPacket::godTierBadgeSlots, ByteBufCodecs.BOOL, GodTierConfigPacket::godTierMasterControl, ByteBufCodecs.BOOL, GodTierConfigPacket::aspectEffects, ByteBufCodecs.BOOL, GodTierConfigPacket::multiAspectUnlocks, GodTierConfigPacket::new);
	
	public static GodTierConfigPacket create()
	{
		return new GodTierConfigPacket(MinestuckConfig.SERVER.maxGodTier.get(), MinestuckConfig.SERVER.godTierXpThreshold.get(), MinestuckConfig.SERVER.godTierBadgeSlots.get(), MinestuckConfig.SERVER.godTierMasterControl.get(), MinestuckConfig.SERVER.aspectEffects.get(), MinestuckConfig.SERVER.multiAspectUnlocks.get());
	}
	
	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	
	@Override
	public void execute(net.neoforged.neoforge.network.handling.IPayloadContext context)
	{
		ClientGodTierConfig.update(this);
	}
	
	public static final class ClientGodTierConfig
	{
		private static GodTierConfigPacket config = createDefaults();
		
		private ClientGodTierConfig()
		{
		}
		
		public static GodTierConfigPacket get()
		{
			return config;
		}
		
		public static void update(GodTierConfigPacket newConfig)
		{
			config = newConfig;
		}
		
		public static int maxGodTier()
		{
			return config.maxGodTier;
		}
		
		public static int godTierXpThreshold()
		{
			return config.godTierXpThreshold;
		}
		
		public static int godTierBadgeSlots()
		{
			return config.godTierBadgeSlots;
		}
		
		public static boolean godTierMasterControl()
		{
			return config.godTierMasterControl;
		}
		
		public static boolean aspectEffects()
		{
			return config.aspectEffects;
		}
		
		public static boolean multiAspectUnlocks()
		{
			return config.multiAspectUnlocks;
		}
		
		private static GodTierConfigPacket createDefaults()
		{
			return new GodTierConfigPacket(-1, 30, 7, false, true, true);
		}
	}
}
