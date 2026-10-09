package com.mraof.minestuck.client;

import com.mraof.minestuck.Minestuck;
import com.mraof.minestuck.effects.MSEffects;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

@EventBusSubscriber(modid = Minestuck.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class TrueConcealmentRender
{
	@SubscribeEvent
	public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event)
	{
		if(event.getEntity().hasEffect(MSEffects.TRUE_CONCEALMENT))
			event.setCanceled(true);
	}
}
