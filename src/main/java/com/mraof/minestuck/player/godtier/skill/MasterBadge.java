package com.mraof.minestuck.player.godtier.skill;

import java.util.Locale;
import javax.annotation.Nullable;
import com.mraof.minestuck.player.ClientPlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
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
	
	public float statNumber(Player player, boolean overlord)
	{
		double luck = player.getAttributeValue(Attributes.LUCK) + 1;
		return (float) Math.max(0, Math.min(limit + (overlord ? 20 : 0), luck * baseStat));
	}
	
	public float statNumber(Player player)
	{
		return statNumber(player, isOverlordActive(player));
	}
	
	public static boolean isOverlordActive(Player player)
	{
		if(player instanceof ServerPlayer serverPlayer)
			return PlayerData.get(serverPlayer)
					.map(data -> data.getData(MSAttachments.GOD_TIER_SKILLS).isBadgeActive(SkillRegistry.BADGE_OVERLORD.get()))
					.orElse(false);
		return ClientPlayerData.isSkillEnabled(SkillRegistry.BADGE_OVERLORD.get().id());
	}
	
	@Override
	public Component getUnlockRequirements()
	{
		return Component.translatable("skill.minestuck.unlock.master_badge", requiredXp);
	}
	
	@Override
	public Component getDisplayTooltip(@Nullable Player player)
	{
		if(player == null)
			return getDisplayTooltip();
		return Component.translatable(translationKey() + ".tooltip",
				String.format(Locale.ROOT, "%.1f", statNumber(player)),
				String.format(Locale.ROOT, "%.1f", SkillRegistry.MASTER_BADGE_WISE.get().statNumber(player) / 2));
	}
	
	@Override
	public ResourceLocation getTextureLocation()
	{
		boolean overlord = ClientPlayerData.isSkillEnabled(SkillRegistry.BADGE_OVERLORD.get().id());
		return ResourceLocation.fromNamespaceAndPath(id().getNamespace(), "textures/gui/skills/badges/" + id().getPath() + (overlord ? "_overlord" : "") + ".png");
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
