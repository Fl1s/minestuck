package com.mraof.minestuck.world.gen.structure.questbed;

import com.mraof.minestuck.player.EnumAspect;
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
		return null;
	}
}
