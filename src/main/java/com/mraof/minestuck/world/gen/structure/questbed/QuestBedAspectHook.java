package com.mraof.minestuck.world.gen.structure.questbed;

import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.Title;
import com.mraof.minestuck.skaianet.SburbPlayerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;

import javax.annotation.Nullable;

public final class QuestBedAspectHook
{
	private QuestBedAspectHook()
	{
	}

	@Nullable
	public static EnumAspect getLandOwnerAspect(WorldGenLevel level)
	{
		ServerLevel serverLevel = level.getLevel();
		return SburbPlayerData.getForLand(serverLevel).flatMap(landData -> Title.getTitle(landData.playerId(), serverLevel.getServer())).map(Title::heroAspect).orElse(null);
	}
}
