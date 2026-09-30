package com.mraof.minestuck.player.dreamself;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Attached to {@link com.mraof.minestuck.player.PlayerData}.
 * Holds the lunar sway of the player, and the state of whichever self (waking body or dreamself) the player is not currently controlling.
 */
public final class DreamselfData implements INBTSerializable<CompoundTag>
{
	@Nullable
	private LunarSway sway;
	private boolean dreaming;
	private boolean dreamselfDead;
	@Nullable
	private SelfState sleepingSelf;
	@Nullable
	private UUID sleepingEntity;
	
	@Nullable
	public LunarSway sway()
	{
		return sway;
	}
	
	public void setSway(@Nullable LunarSway sway)
	{
		this.sway = sway;
	}
	
	/**
	 * @return true if the player entity is currently controlling the dreamself, false if it controls the waking body
	 */
	public boolean isDreaming()
	{
		return dreaming;
	}
	
	/**
	 * @return the state of the self that is asleep right now, or null if the dreamself has not been created
	 */
	@Nullable
	public SelfState sleepingSelf()
	{
		return sleepingSelf;
	}
	
	/**
	 * @return the uuid of the entity that represents the sleeping self in the world
	 */
	@Nullable
	public UUID sleepingEntity()
	{
		return sleepingEntity;
	}
	
	public boolean isReady()
	{
		return !dreamselfDead && sway != null && sleepingSelf != null;
	}
	
	/**
	 * The dreamself only has one life. Once it has died, it is gone for good.
	 */
	public boolean isDreamselfDead()
	{
		return dreamselfDead;
	}
	
	/**
	 * Marks the dreamself as permanently dead. The player is left controlling the waking body.
	 */
	public void killDreamself()
	{
		this.dreamselfDead = true;
		this.dreaming = false;
		this.sleepingSelf = null;
		this.sleepingEntity = null;
	}
	
	/**
	 * Updates the stored state of the sleeping self without changing which self is asleep.
	 */
	public void setSleepingSelf(SelfState sleepingSelf)
	{
		this.sleepingSelf = sleepingSelf;
	}
	
	public void set(boolean dreaming, @Nullable SelfState sleepingSelf, @Nullable UUID sleepingEntity)
	{
		this.dreaming = dreaming;
		this.sleepingSelf = sleepingSelf;
		this.sleepingEntity = sleepingEntity;
	}
	
	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider provider)
	{
		CompoundTag nbt = new CompoundTag();
		if(sway != null) nbt.putString("sway", sway.getSerializedName());
		nbt.putBoolean("dreaming", dreaming);
		nbt.putBoolean("dreamself_dead", dreamselfDead);
		if(sleepingSelf != null) nbt.put("sleeping_self", sleepingSelf.save());
		if(sleepingEntity != null) nbt.putUUID("sleeping_entity", sleepingEntity);
		return nbt;
	}
	
	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)
	{
		sway = nbt.contains("sway", Tag.TAG_STRING) ? LunarSway.byName(nbt.getString("sway")) : null;
		dreaming = nbt.getBoolean("dreaming");
		dreamselfDead = nbt.getBoolean("dreamself_dead");
		sleepingSelf = nbt.contains("sleeping_self", Tag.TAG_COMPOUND) ? SelfState.load(nbt.getCompound("sleeping_self")) : null;
		sleepingEntity = nbt.hasUUID("sleeping_entity") ? nbt.getUUID("sleeping_entity") : null;
	}
}
