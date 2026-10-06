package com.mraof.minestuck.player.godtier.skill;

import com.mraof.minestuck.player.PlayerData;
import com.mraof.minestuck.util.MSAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class MasterBadge extends BadgeLevel
{
	private final int requiredXp;
	private final float baseStat;
	private final float limit;
	
	public MasterBadge(ResourceLocation id, int sortIndex, int requiredLevel, int requiredXp, float baseStat, float limit)
	{
		super(id, sortIndex, requiredLevel);
		this.requiredXp = requiredXp;
		this.baseStat = baseStat;
		this.limit = limit;
	}
	
	public int requiredXp()
	{
		return requiredXp;
	}
	
	public float statNumber(ServerPlayer player, boolean overlord)
	{
		double luck = player.getAttributeValue(Attributes.LUCK) + 1;
		return (float) Math.max(0, Math.min(limit + (overlord ? 20 : 0), luck * baseStat));
	}
	
	public float statNumber(ServerPlayer player)
	{
		return statNumber(player, isOverlordActive(player));
	}
	
	public static boolean isOverlordActive(ServerPlayer player)
	{
		return PlayerData.get(player)
				.map(data -> data.getData(MSAttachments.GOD_TIER_SKILLS).isBadgeActive(SkillRegistry.BADGE_OVERLORD.get()))
				.orElse(false);
	}
	
	@Override
	public boolean canUnlock(ServerLevel level, ServerPlayer player)
	{
		if(player.experienceLevel < requiredXp)
			return false;
		player.giveExperienceLevels(-requiredLevel());
		return true;
	}
}
