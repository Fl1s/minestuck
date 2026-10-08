package com.mraof.minestuck.player.godtier.skill;

import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;
import com.mraof.minestuck.Minestuck;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

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
	
	public String translationKey()
	{
		return "skill." + id.toLanguageKey();
	}
	
	public Component getDisplayName()
	{
		return Component.translatable(translationKey());
	}
	
	public Component getDisplayTooltip()
	{
		return Component.translatable(translationKey() + ".tooltip");
	}
	
	public Component getDisplayTooltip(@Nullable Player player)
	{
		return getDisplayTooltip();
	}
	
	public Component getUnlockRequirements()
	{
		return Component.translatable(translationKey() + ".unlock");
	}
	
	public Component getReadRequirements()
	{
		return Component.translatable(translationKey() + ".read");
	}
	
	public ResourceLocation getTextureLocation()
	{
		return ResourceLocation.fromNamespaceAndPath(id().getNamespace(), "textures/gui/skills/badges/" + id().getPath() + ".png");
	}
	
	protected static boolean consumeItems(ServerPlayer player, ItemStack required, boolean decr)
	{
		return consumeItems(player, List.of(required), decr);
	}
	
	protected static boolean consumeItems(ServerPlayer player, List<ItemStack> required, boolean decr)
	{
		if(!hasItems(player, required))
			return false;
		if(decr)
			for(ItemStack need : required)
				removeItems(player, need);
		return true;
	}
	
	protected static boolean hasItems(ServerPlayer player, List<ItemStack> required)
	{
		for(ItemStack need : required)
			if(countItems(player, need) < need.getCount())
				return false;
		return true;
	}
	
	protected static int countItems(ServerPlayer player, ItemStack required)
	{
		int count = 0;
		for(int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
		{
			ItemStack stack = player.getInventory().getItem(slot);
			if(stack.is(required.getItem()))
				count += stack.getCount();
		}
		return count;
	}
	
	protected static void removeItems(ServerPlayer player, ItemStack required)
	{
		int remaining = required.getCount();
		for(int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++)
		{
			ItemStack stack = player.getInventory().getItem(slot);
			if(stack.is(required.getItem()))
			{
				int removed = Math.min(stack.getCount(), remaining);
				stack.shrink(removed);
				remaining -= removed;
			}
		}
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
