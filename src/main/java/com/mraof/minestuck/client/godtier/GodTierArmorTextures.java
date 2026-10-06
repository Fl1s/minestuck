package com.mraof.minestuck.client.godtier;

import com.mojang.blaze3d.platform.NativeImage;
import com.mraof.minestuck.Minestuck;
import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import com.mraof.minestuck.player.godtier.GodTierPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class GodTierArmorTextures
{
	private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();
	
	private record Key(EnumClass heroClass, @Nullable EnumAspect aspect)
	{
	}
	
	private static final Map<Key, ResourceLocation> CACHE = new HashMap<>();
	
	private GodTierArmorTextures()
	{
	}
	
	public static ResourceLocation get(EnumClass heroClass, @Nullable EnumAspect aspect)
	{
		return CACHE.computeIfAbsent(new Key(heroClass, aspect), GodTierArmorTextures::build);
	}
	
	private static ResourceLocation build(Key key)
	{
		ResourceManager resources = Minecraft.getInstance().getResourceManager();
		String cls = key.heroClass().name().toLowerCase(Locale.ROOT);
		String asp = key.aspect() == null ? "default" : key.aspect().getSerializedName();
		int[] palette = GodTierPalette.get(key.aspect());
		
		NativeImage result = new NativeImage(128, 128, true);
		for(int i = 0; i < palette.length; i++)
		{
			ResourceLocation layerId = ResourceLocation.fromNamespaceAndPath(Minestuck.MOD_ID, "textures/models/armor/god_tier/gt_" + cls + "_layer_" + (i + 1) + ".png");
			Optional<Resource> resource = resources.getResource(layerId);
			if(resource.isEmpty()) continue;
			try(InputStream in = resource.get().open(); NativeImage layer = NativeImage.read(in))
			{
				compositeTinted(result, layer, palette[i], 0, 0);
			} catch(IOException e)
			{
				LOGGER.warn("Failed to read god tier armor layer {}", layerId, e);
			}
		}
		
		GodTierModelSpec spec = GodTierModelSpec.get(key.heroClass());
		if(spec != null)
		{
			ResourceLocation symbolId = ResourceLocation.fromNamespaceAndPath(Minestuck.MOD_ID, "textures/models/armor/symbol/" + asp + ".png");
			Optional<Resource> symbol = resources.getResource(symbolId);
			if(symbol.isPresent()) try(InputStream in = symbol.get().open(); NativeImage img = NativeImage.read(in))
			{
				compositeTinted(result, img, 0xFFFFFF, spec.symbolUv()[0], spec.symbolUv()[1]);
			} catch(IOException e)
			{
				LOGGER.warn("Failed to read god tier symbol {}", symbolId, e);
			}
		}
		
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Minestuck.MOD_ID, "generated/god_tier/" + cls + "_" + asp);
		Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(result));
		return id;
	}
	
	private static void compositeTinted(NativeImage dst, NativeImage src, int rgb, int ox, int oy)
	{
		int tr = (rgb >> 16) & 0xFF, tg = (rgb >> 8) & 0xFF, tb = rgb & 0xFF;
		int w = Math.min(dst.getWidth() - ox, src.getWidth()), h = Math.min(dst.getHeight() - oy, src.getHeight());
		for(int y = 0; y < h; y++)
			for(int x = 0; x < w; x++)
			{
				int s = src.getPixelRGBA(x, y); // ABGR
				int dxp = x + ox, dyp = y + oy;
				int sa = s >>> 24;
				if(sa == 0) continue;
				int sr = (s & 0xFF) * tr / 255, sg = ((s >> 8) & 0xFF) * tg / 255, sb = ((s >> 16) & 0xFF) * tb / 255;
				int d = dst.getPixelRGBA(dxp, dyp);
				int da = d >>> 24;
				int outA = sa + da * (255 - sa) / 255;
				if(sa == 255 || da == 0)
				{
					dst.setPixelRGBA(dxp, dyp, (sa << 24) | (sb << 16) | (sg << 8) | sr);
					continue;
				}
				int dr = d & 0xFF, dg = (d >> 8) & 0xFF, db = (d >> 16) & 0xFF;
				int wd = da * (255 - sa) / 255;
				int r = (sr * sa + dr * wd) / outA, g = (sg * sa + dg * wd) / outA, b = (sb * sa + db * wd) / outA;
				dst.setPixelRGBA(dxp, dyp, (outA << 24) | (b << 16) | (g << 8) | r);
			}
	}
	
	private static void clear()
	{
		var textures = Minecraft.getInstance().getTextureManager();
		CACHE.values().forEach(textures::release);
		CACHE.clear();
		GodTierModelSpec.clearCache();
		GodTierArmorModel.clearCache();
	}
	
	@EventBusSubscriber(modid = Minestuck.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static final class Reload
	{
		@SubscribeEvent
		public static void register(RegisterClientReloadListenersEvent event)
		{
			event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) rm -> clear());
		}
	}
}