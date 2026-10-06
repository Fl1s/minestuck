package com.mraof.minestuck.client.renderer.entity;

import com.mraof.minestuck.entity.item.LocatorEyeEntity;
import com.mraof.minestuck.item.MSItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

import static net.minecraft.world.item.ItemDisplayContext.*;

public class LocatorEyeRenderer extends EntityRenderer<LocatorEyeEntity>
{
	public LocatorEyeRenderer(EntityRendererProvider.Context context)
	{
		super(context);
	}
	
	@Override
	public void render(LocatorEyeEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight)
	{
		poseStack.pushPose();
		float rotation = (entity.tickCount + partialTick) * 20;
		poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotation));
		var itemRenderer = net.minecraft.client.Minecraft.getInstance().getItemRenderer();
		itemRenderer.renderStatic(new ItemStack(MSItems.DENIZEN_EYE.get()), FIXED, 15728880, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
		poseStack.popPose();
		super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
	}
	
	@Override
	public net.minecraft.resources.ResourceLocation getTextureLocation(LocatorEyeEntity entity)
	{
		return InventoryMenu.BLOCK_ATLAS;
	}
}
