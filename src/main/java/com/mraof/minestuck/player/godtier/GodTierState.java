package com.mraof.minestuck.player.godtier;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class GodTierState implements INBTSerializable<CompoundTag>
{
	private boolean godTier = false;
	private boolean canGodTier = true;
	private boolean climbedTheSpire = false;

	private boolean pendingReset = false;

	public void reset()
	{
		godTier = false;
		climbedTheSpire = false;
		pendingReset = true;
	}

	public boolean isGodTier()
	{
		return godTier;
	}

	public void setGodTier(boolean godTier)
	{
		this.godTier = godTier;
	}

	public boolean canGodTier()
	{
		return canGodTier;
	}

	public void setCanGodTier(boolean canGodTier)
	{
		this.canGodTier = canGodTier;
	}

	public boolean hasClimbedTheSpire()
	{
		return climbedTheSpire;
	}

	public void setClimbedTheSpire(boolean climbedTheSpire)
	{
		this.climbedTheSpire = climbedTheSpire;
	}
	
	public void markForReset()
	{
		this.pendingReset = true;
	}

	public boolean isPendingReset()
	{
		return pendingReset;
	}

	public void clearPendingReset()
	{
		this.pendingReset = false;
	}

	@Override
	public CompoundTag serializeNBT(net.minecraft.core.HolderLookup.Provider registries)
	{
		CompoundTag nbt = new CompoundTag();
		nbt.putBoolean("GodTier", godTier);
		nbt.putBoolean("CanGodTier", canGodTier);
		nbt.putBoolean("ClimbedTheSpire", climbedTheSpire);
		if(pendingReset)
			nbt.putBoolean("PendingReset", true);
		return nbt;
	}

	@Override
	public void deserializeNBT(net.minecraft.core.HolderLookup.Provider registries, CompoundTag nbt)
	{
		this.godTier = nbt.getBoolean("GodTier");
		this.canGodTier = !nbt.contains("CanGodTier") || nbt.getBoolean("CanGodTier");
		this.climbedTheSpire = nbt.getBoolean("ClimbedTheSpire");
		this.pendingReset = nbt.getBoolean("PendingReset");
	}
}
