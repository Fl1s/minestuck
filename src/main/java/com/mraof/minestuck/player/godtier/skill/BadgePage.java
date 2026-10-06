package com.mraof.minestuck.player.godtier.skill;

import com.mraof.minestuck.player.EnumClass;
import com.mraof.minestuck.player.PlayerData;
import com.mraof.minestuck.player.Title;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class BadgePage extends Badge
{
	private static final int REQUIRED_XP = 80;
	
	public BadgePage(ResourceLocation id, int sortIndex)
	{
		super(id, sortIndex);
	}
	
	@Override
	public boolean canAppearOnList(ServerLevel level, ServerPlayer player)
	{
		return PlayerData.get(player)
				.flatMap(data -> Title.getTitle(data))
				.map(title -> title.heroClass() == EnumClass.PAGE)
				.orElse(false);
	}
	
	@Override
	public boolean canUnlock(ServerLevel level, ServerPlayer player)
	{
		if(!canAppearOnList(level, player))
			return false;
		if(player.experienceLevel < REQUIRED_XP)
			return false;
		player.giveExperienceLevels(-REQUIRED_XP);
		return true;
	}
}
