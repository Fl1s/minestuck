package com.mraof.minestuck.player.godtier;

import com.mraof.minestuck.effects.MSEffects;
import com.mraof.minestuck.player.Echeladder;
import com.mraof.minestuck.player.PlayerData;
import com.mraof.minestuck.player.Title;
import com.mraof.minestuck.util.MSAttachments;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Takes a player back to before their ascension. Port of {@code MSUUtils#resetGodTier}.
 */
public final class GodTierReset
{
	private GodTierReset()
	{
	}
	
	/**
	 * @return true if the player was god tier and has been reset
	 */
	public static boolean reset(ServerPlayer player)
	{
		Optional<PlayerData> optData = PlayerData.get(player);
		if(optData.isEmpty())
			return false;
		PlayerData data = optData.get();
		GodTierState state = data.getData(MSAttachments.GOD_TIER_STATE);
		if(!state.isGodTier())
			return false;
		
		Title title = Title.getTitle(data).orElse(null);
		if(title != null && state.areAspectEffectsApplied())
			GodTierTickHandler.getAspectEffects(player, data, state, title.heroAspect()).keySet().forEach(player::removeEffect);
		state.setAspectEffectsApplied(false);
		player.removeEffect(MSEffects.GOD_TIER_COMEBACK);
		
		data.getData(MSAttachments.GOD_TIER_STATS).resetAll();
		data.getData(MSAttachments.GOD_TIER_SKILLS).reset();
		data.getData(MSAttachments.GOD_TIER_KARMA).reset();
		state.reset();
		
		Echeladder.get(player).setProgressEnabled(true);
		GodTierTickHandler.sendDataPacket(player, data);
		return true;
	}
}
