package com.mraof.minestuck.player.godtier.skill;

import net.minecraft.network.chat.Component;
import com.mraof.minestuck.player.EnumClass;
import com.mraof.minestuck.player.PlayerData;
import com.mraof.minestuck.player.Title;
import com.mraof.minestuck.player.godtier.GodTierStat;
import com.mraof.minestuck.util.MSAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class BadgeOverlord extends Badge
{
	public static final int REQUIRED_LEVEL = 80;
	
	public BadgeOverlord(ResourceLocation id, int sortIndex)
	{
		super(id, sortIndex);
	}
	
	@Override
	public ResourceLocation getTextureLocation()
	{
		return ResourceLocation.fromNamespaceAndPath(id().getNamespace(), "textures/gui/skills/badges/world_ender.png");
	}
	
	@Override
	public Component getReadRequirements()
	{
		return Component.translatable("skill.minestuck.read.secret");
	}
	
	@Override
	public boolean canUnlock(ServerLevel level, ServerPlayer player)
	{
		//only ever awarded by a Lord's sacrificial death
		return false;
	}
	
	@Override
	public boolean canAppearOnList(ServerLevel level, ServerPlayer player)
	{
		return false;
	}
	
	@Override
	public boolean isReadable(ServerLevel level, ServerPlayer player)
	{
		return PlayerData.get(player).map(data -> {
			var skills = data.getData(MSAttachments.GOD_TIER_SKILLS);
			int levelStat = data.getData(MSAttachments.GOD_TIER_STATS).getLevel(GodTierStat.GENERAL);
			return skills.hasSkill(this) || (data.getData(MSAttachments.GOD_TIER_STATE).hasMasterControl() && levelStat >= REQUIRED_LEVEL);
		}).orElse(false);
	}
}
