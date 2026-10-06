package com.mraof.minestuck.player.godtier.skill;

import com.mraof.minestuck.Minestuck;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public abstract class Skill
{
	private final ResourceLocation id;
	private final int sortIndex;
	
	protected Skill(ResourceLocation id, int sortIndex)
	{
		this.id = id;
		this.sortIndex = sortIndex;
	}
	
	public ResourceLocation id()
	{
		return id;
	}
	
	public int sortIndex()
	{
		return sortIndex;
	}
	
	public SkillType type()
	{
		return this instanceof Abilitech ? SkillType.ABILITECH : SkillType.BADGE;
	}
	
	public boolean canAppearOnList(ServerLevel level, ServerPlayer player)
	{
		return true;
	}
	
	public boolean isReadable(ServerLevel level, ServerPlayer player)
	{
		return true;
	}
	
	public boolean canUnlock(ServerLevel level, ServerPlayer player)
	{
		return true;
	}
	
	public void onUnlock(ServerLevel level, ServerPlayer player)
	{
	}
	
	public boolean canUse(ServerLevel level, ServerPlayer player)
	{
		return true;
	}
	
	public boolean canDisable()
	{
		return true;
	}
	
	private String translationKey()
	{
		return "skill." + id.toLanguageKey();
	}
	
	public Component getDisplayName()
	{
		return Component.translatable(translationKey());
	}
	
	public Component getUnlockRequirements()
	{
		return Component.translatable(translationKey() + ".unlock");
	}
	
	public Component getReadRequirements()
	{
		return Component.translatable(translationKey() + ".read");
	}
	
	public int getColor()
	{
		return 0xFFFFFF;
	}
	
	public static ResourceLocation defaultId(String path)
	{
		return Minestuck.id(path);
	}
}
