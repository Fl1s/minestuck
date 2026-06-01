package com.mraof.minestuck.client;

import com.mraof.minestuck.Minestuck;
import com.mraof.minestuck.MinestuckConfig;
import com.mraof.minestuck.entry.meteor.MeteorManager;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = Minestuck.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class MeteorScreenShake
{
	private static float miniImpactShake = 0.0f;
	private static int miniImpactTicks = 0;
	
	public static void onMiniMeteorImpactNearby(double distanceToPlayer)
	{
		if(distanceToPlayer < 50)
		{
			float intensity = (float) (1.0 - distanceToPlayer / 50.0);
			miniImpactShake = Math.max(miniImpactShake, intensity * 3.0f);
			miniImpactTicks = 20;
		}
	}
	
	@SubscribeEvent
	public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event)
	{
		if(!MinestuckConfig.CLIENT.impactScreenShake.get()) return;
		if(!MeteorClientHandler.hasActiveMeteor()) return;
		
		Minecraft mc = Minecraft.getInstance();
		if(mc.level == null) return;
		
		int ticksElapsed = MeteorClientHandler.getLocalPlayerMeteorTicks();
		int totalTicks = MeteorManager.TOTAL_TICKS;
		int ticksLeft = totalTicks - ticksElapsed;
		
		float meteorShake = 0.0f;
		if(ticksLeft <= 600)
		{
			float shakeProgress = 1.0f - (float) ticksLeft / 600.0f;
			meteorShake = shakeProgress * shakeProgress * 0.3f;
		}
		
		float currentMiniShake = 0.0f;
		if(miniImpactTicks > 0)
		{
			miniImpactTicks--;
			float decay = (float) miniImpactTicks / 20.0f;
			currentMiniShake = miniImpactShake * decay;
			if(miniImpactTicks == 0) miniImpactShake = 0.0f;
		}
		
		float totalShake = meteorShake + currentMiniShake;
		if(totalShake <= 0) return;
		
		long time = mc.level.getGameTime();
		float noiseX = (float) (Math.sin(time * 0.3 + 1.0) * Math.sin(time * 0.7)) * totalShake;
		float noiseY = (float) (Math.sin(time * 0.5 + 2.0) * Math.cos(time * 0.4)) * totalShake;
		
		event.setPitch(event.getPitch() + noiseX);
		event.setYaw(event.getYaw() + noiseY);
	}
}
