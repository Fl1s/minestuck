package com.mraof.minestuck.player.godtier.skill;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class Abilitech extends Skill
{
	private final List<TechType> techTypes;
	
	public Abilitech(ResourceLocation id, int sortIndex, List<TechType> techTypes)
	{
		super(id, sortIndex);
		this.techTypes = List.copyOf(techTypes);
	}
	
	public List<TechType> techTypes()
	{
		return techTypes;
	}
	
	public boolean onUseTick(ServerLevel level, ServerPlayer player, int slot, boolean keyDown)
	{
		return false;
	}
	
	public boolean onPassiveTick(ServerLevel level, ServerPlayer player)
	{
		return false;
	}
	
	public void onEquipped(ServerLevel level, ServerPlayer player, int slot)
	{
	}
	
	public void onUnequipped(ServerLevel level, ServerPlayer player, int slot)
	{
	}
	
	public void onPassiveToggle(ServerLevel level, ServerPlayer player, boolean enabled)
	{
	}
}
