package com.mraof.minestuck.player.godtier.skill;

import com.mraof.minestuck.MinestuckConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.*;

public final class GodTierSkills implements INBTSerializable<CompoundTag>
{
	private static final Logger LOGGER = LogManager.getLogger();
	public static final int TECH_SLOT_COUNT = 3;
	
	private final Map<ResourceLocation, Boolean> unlockedSkills = new LinkedHashMap<>();
	private final Map<ResourceLocation, Boolean> passiveEnabled = new HashMap<>();
	private ResourceLocation masterBadge;
	private final ResourceLocation[] equippedTechs = new ResourceLocation[TECH_SLOT_COUNT];
	private int maxBadges = -1;
	
	public boolean addSkill(Skill skill)
	{
		if(unlockedSkills.containsKey(skill.id()))
			return false;
		
		unlockedSkills.put(skill.id(), skill.canDisable());
		if(skill instanceof Abilitech)
			equipFirstFreeTech(skill.id());
		
		return true;
	}
	
	public boolean revokeSkill(Skill skill)
	{
		if(!unlockedSkills.remove(skill.id(), false) && !unlockedSkills.containsKey(skill.id()))
			return false;
		
		unlockedSkills.remove(skill.id());
		passiveEnabled.remove(skill.id());
		if(masterBadge != null && masterBadge.equals(skill.id()))
			masterBadge = null;
		for(int i = 0; i < TECH_SLOT_COUNT; i++)
			if(skill.id().equals(equippedTechs[i]))
				equippedTechs[i] = null;
		
		return true;
	}
	
	public boolean hasSkill(Skill skill)
	{
		return unlockedSkills.containsKey(skill.id());
	}
	
	public boolean isBadgeEnabled(Skill skill)
	{
		return unlockedSkills.getOrDefault(skill.id(), false);
	}
	
	public boolean isBadgeActive(Skill skill)
	{
		return isBadgeEnabled(skill);
	}
	
	public boolean setBadgeEnabled(Skill skill, boolean enabled)
	{
		if(!hasSkill(skill) || !skill.canDisable())
			return false;
		unlockedSkills.put(skill.id(), enabled);
		return true;
	}
	
	public boolean isPassiveEnabled(Skill skill)
	{
		return passiveEnabled.getOrDefault(skill.id(), false);
	}
	
	public boolean setPassiveEnabled(Skill skill, boolean enabled)
	{
		if(!(skill instanceof Abilitech) || !hasSkill(skill))
			return false;
		passiveEnabled.put(skill.id(), enabled);
		return true;
	}
	
	public ResourceLocation masterBadge()
	{
		return masterBadge;
	}
	
	public boolean setMasterBadge(@Nullable Skill skill)
	{
		if(skill == null)
		{
			this.masterBadge = null;
			return true;
		}
		if(!(skill instanceof MasterBadge) || !hasSkill(skill))
			return false;
		this.masterBadge = skill.id();
		return true;
	}
	
	public boolean equipTech(Skill skill, int slot)
	{
		if(!(skill instanceof Abilitech) || !hasSkill(skill) || invalidSlot(slot))
			return false;
		unequipTechId(skill.id());
		equippedTechs[slot] = skill.id();
		return true;
	}
	
	public boolean unequipTech(int slot)
	{
		if(invalidSlot(slot))
			return false;
		equippedTechs[slot] = null;
		return true;
	}
	
	@Nullable
	public ResourceLocation getTech(int slot)
	{
		return invalidSlot(slot) ? null : equippedTechs[slot];
	}
	
	public List<ResourceLocation> getAllBadges()
	{
		return unlockedSkills.keySet().stream().filter(id -> SkillRegistry.get(id) instanceof Badge).toList();
	}
	
	public List<ResourceLocation> getAllAbilitechs()
	{
		return unlockedSkills.keySet().stream().filter(id -> SkillRegistry.get(id) instanceof Abilitech).toList();
	}
	
	public int getMaxBadges()
	{
		return maxBadges;
	}
	
	public void setMaxBadges(int maxBadges)
	{
		this.maxBadges = maxBadges;
	}
	
	public int badgeLimit()
	{
		return maxBadges >= 0 ? maxBadges : MinestuckConfig.SERVER.godTierBadgeSlots.get();
	}
	
	public void reset()
	{
		unlockedSkills.clear();
		passiveEnabled.clear();
		masterBadge = null;
		Arrays.fill(equippedTechs, null);
		maxBadges = -1;
	}
	
	private boolean invalidSlot(int slot)
	{
		return slot < 0 || slot >= TECH_SLOT_COUNT;
	}
	
	private void equipFirstFreeTech(ResourceLocation id)
	{
		for(int i = 0; i < TECH_SLOT_COUNT; i++)
			if(equippedTechs[i] == null)
			{
				equippedTechs[i] = id;
				return;
			}
	}
	
	private void unequipTechId(ResourceLocation id)
	{
		for(int i = 0; i < TECH_SLOT_COUNT; i++)
			if(id.equals(equippedTechs[i]))
				equippedTechs[i] = null;
	}
	

	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider registries)
	{
		CompoundTag nbt = new CompoundTag();
		
		CompoundTag skillsTag = new CompoundTag();
		unlockedSkills.forEach((id, enabled) -> skillsTag.putBoolean(id.toString(), enabled));
		nbt.put("Skills", skillsTag);
		
		CompoundTag passiveTag = new CompoundTag();
		passiveEnabled.forEach((id, enabled) -> passiveTag.putBoolean(id.toString(), enabled));
		nbt.put("Passive", passiveTag);
		
		if(masterBadge != null)
			nbt.putString("MasterBadge", masterBadge.toString());
		for(int i = 0; i < TECH_SLOT_COUNT; i++)
			if(equippedTechs[i] != null)
				nbt.putString("Tech" + i, equippedTechs[i].toString());
		nbt.putInt("MaxBadges", maxBadges);
		
		return nbt;
	}
	
	@Override
	public void deserializeNBT(HolderLookup.Provider registries, CompoundTag nbt)
	{
		unlockedSkills.clear();
		passiveEnabled.clear();
		masterBadge = null;
		Arrays.fill(equippedTechs, null);
		
		CompoundTag skillsTag = nbt.getCompound("Skills");
		for(String key : skillsTag.getAllKeys())
			try
			{
				unlockedSkills.put(ResourceLocation.parse(key), skillsTag.getBoolean(key));
			} catch(Exception e)
			{
				LOGGER.warn("Ignoring invalid God Tier skill id {}", key, e);
			}
		
		CompoundTag passiveTag = nbt.getCompound("Passive");
		for(String key : passiveTag.getAllKeys())
			try
			{
				passiveEnabled.put(ResourceLocation.parse(key), passiveTag.getBoolean(key));
			} catch(Exception e)
			{
				LOGGER.warn("Ignoring invalid God Tier passive skill id {}", key, e);
			}
		
		if(nbt.contains("MasterBadge"))
			try
			{
				masterBadge = ResourceLocation.parse(nbt.getString("MasterBadge"));
			} catch(Exception e)
			{
				LOGGER.warn("Ignoring invalid God Tier master badge id {}", nbt.getString("MasterBadge"), e);
			}
		for(int i = 0; i < TECH_SLOT_COUNT; i++)
			if(nbt.contains("Tech" + i))
				try
				{
					equippedTechs[i] = ResourceLocation.parse(nbt.getString("Tech" + i));
				} catch(Exception e)
				{
					LOGGER.warn("Ignoring invalid God Tier tech id {}", nbt.getString("Tech" + i), e);
				}
		maxBadges = nbt.getInt("MaxBadges");
	}
}
