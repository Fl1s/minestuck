package com.mraof.minestuck.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mraof.minestuck.entity.SleepingSelfEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

/**
 * Renders the sleeping self of a player using the skin of that player.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SleepingSelfRenderer extends MobRenderer<SleepingSelfEntity, PlayerModel<SleepingSelfEntity>>
{
	private static final double LIE_HEIGHT = 0.125;
	
	private final PlayerModel<SleepingSelfEntity> defaultModel;
	private final PlayerModel<SleepingSelfEntity> slimModel;
	
	public SleepingSelfRenderer(EntityRendererProvider.Context context)
	{
		super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.3F);
		defaultModel = getModel();
		slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
		this.addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)), new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
		this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
	}
	
	@Override
	public void render(SleepingSelfEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight)
	{
		PlayerSkin skin = getSkin(entity.getOwnerId());
		model = skin.model() == PlayerSkin.Model.SLIM ? slimModel : defaultModel;
		
		poseStack.pushPose();
		poseStack.translate(0, LIE_HEIGHT, 0);
		super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
		poseStack.popPose();
	}
	
	@Override
	public ResourceLocation getTextureLocation(SleepingSelfEntity entity)
	{
		return getSkin(entity.getOwnerId()).texture();
	}
	
	private static PlayerSkin getSkin(UUID playerId)
	{
		ClientPacketListener packetListener = Minecraft.getInstance().getConnection();
		PlayerInfo info = packetListener != null ? packetListener.getPlayerInfo(playerId) : null;
		return info != null ? info.getSkin() : DefaultPlayerSkin.get(playerId);
	}
}
