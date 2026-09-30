package com.mraof.minestuck.client;

import com.mraof.minestuck.Minestuck;
import com.mraof.minestuck.network.DreamselfFadePacket;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.Input;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

/**
 * The fade to black that covers a swap between the waking body and the dreamself.
 */
@EventBusSubscriber(modid = Minestuck.MOD_ID, value = Dist.CLIENT)
public final class DreamselfFade
{
	private static boolean active;
	private static int fadeOut, hold, fadeIn;
	private static int elapsed;
	
	public static void start(DreamselfFadePacket packet)
	{
		active = true;
		fadeOut = Math.max(0, packet.fadeOut());
		hold = Math.max(0, packet.hold());
		fadeIn = Math.max(1, packet.fadeIn());
		elapsed = 0;
		
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(packet.sound()), 1.0F, 1.0F));
	}
	
	public static void reset()
	{
		active = false;
		elapsed = 0;
	}
	
	private static boolean isFallingAsleep()
	{
		return active && elapsed < fadeOut + hold;
	}
	
	private static float alpha(float time)
	{
		float value;
		if(time < fadeOut) value = time / fadeOut;
		else if(time < fadeOut + hold) value = 1;
		else value = 1 - (time - fadeOut - hold) / fadeIn;
		
		value = Mth.clamp(value, 0, 1);
		return value * value * (3 - 2 * value);
	}
	
	public static void renderOverlay(GuiGraphics graphics, DeltaTracker deltaTracker)
	{
		if(!active) return;
		
		float time = elapsed + (Minecraft.getInstance().isPaused() ? 0 : deltaTracker.getGameTimeDeltaPartialTick(false));
		int alpha = (int) (alpha(time) * 255);
		if(alpha > 0) graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha << 24);
	}
	
	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event)
	{
		if(!active || Minecraft.getInstance().isPaused()) return;
		
		elapsed++;
		if(elapsed >= fadeOut + hold + fadeIn) reset();
	}
	
	@SubscribeEvent
	public static void onMovementInput(MovementInputUpdateEvent event)
	{
		if(!isFallingAsleep()) return;
		
		Input input = event.getInput();
		input.leftImpulse = 0;
		input.forwardImpulse = 0;
		input.up = false;
		input.down = false;
		input.left = false;
		input.right = false;
		input.jumping = false;
		input.shiftKeyDown = false;
	}
	
	@SubscribeEvent
	public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event)
	{
		reset();
	}
}
