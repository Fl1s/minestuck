package com.mraof.minestuck.player.godtier;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.util.INBTSerializable;

public final class GodTierKarma implements INBTSerializable<CompoundTag>
{
	public static final int CAP = 200;
	
	private int staticKarma;
	private float tempKarma;
	
	public int getStatic() { return staticKarma; }
	
	public float getTemp() { return tempKarma; }
	
	public int getTotal() { return (int) (tempKarma + staticKarma); }
	
	public void setStatic(int value) { staticKarma = Mth.clamp(value, -CAP, CAP); }
	
	public void setTemp(float value) { tempKarma = Mth.clamp(value, -CAP, CAP); }
	
	public void reset()
	{
		staticKarma = 0;
		tempKarma = 0;
	}
	
	public void decay(float speedMultiplier)
	{
		if(tempKarma == 0)
			return;
		tempKarma -= (1f / 1200f) * speedMultiplier * Math.signum(tempKarma);
		if(tempKarma > -0.001f && tempKarma < 0.001f)
			tempKarma = 0;
	}
	
	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider registries)
	{
		CompoundTag nbt = new CompoundTag();
		nbt.putInt("StaticKarma", staticKarma);
		nbt.putFloat("TempKarma", tempKarma);
		return nbt;
	}
	
	@Override
	public void deserializeNBT(HolderLookup.Provider registries, CompoundTag nbt)
	{
		staticKarma = nbt.getInt("StaticKarma");
		tempKarma = nbt.getFloat("TempKarma");
	}
}