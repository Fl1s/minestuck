package com.mraof.minestuck.player.godtier;

import com.mraof.minestuck.MinestuckConfig;
import com.mraof.minestuck.entity.consort.EnumConsort;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class GodTierState implements INBTSerializable<CompoundTag>
{
	private boolean godTier = false;
	private boolean canGodTier = true;
	private boolean climbedTheSpire = false;
	
	private boolean pendingReset = false;
	
	public enum LunarSway
	{
		PROSPIT, DERSE
	}
	
	private boolean pendingArmor = false;
	
	private boolean flightGranted = false;
	private boolean aspectEffectsApplied = false;
	
	@Nullable
	private Boolean masterControl = null; //null means "use the config value"
	private int maxBadges = -1;
	private int scrollsUsed = 0;
	private LunarSway lunarSway;
	private EnumConsort consortType;
	private ResourceLocation gristHoard;
	
	public void reset()
	{
		godTier = false;
		pendingArmor = false;
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
	
	public boolean isPendingArmor()
	{
		return pendingArmor;
	}
	
	public void setPendingArmor(boolean pendingArmor)
	{
		this.pendingArmor = pendingArmor;
	}
	
	public boolean isFlightGranted()
	{
		return flightGranted;
	}
	
	public void setFlightGranted(boolean flightGranted)
	{
		this.flightGranted = flightGranted;
	}
	
	public boolean areAspectEffectsApplied()
	{
		return aspectEffectsApplied;
	}
	
	public void setAspectEffectsApplied(boolean aspectEffectsApplied)
	{
		this.aspectEffectsApplied = aspectEffectsApplied;
	}
	
	public boolean hasMasterControl()
	{
		return masterControl != null ? masterControl : MinestuckConfig.SERVER.godTierMasterControl.get();
	}
	
	public void setMasterControl(boolean masterControl)
	{
		this.masterControl = masterControl;
	}
	
	public int getMaxBadges()
	{
		return maxBadges;
	}
	
	public void setMaxBadges(int maxBadges)
	{
		this.maxBadges = maxBadges;
	}
	
	public int getScrollsUsed()
	{
		return scrollsUsed;
	}
	
	public void addScrollUsed()
	{
		scrollsUsed++;
	}
	
	@Nullable
	public LunarSway getLunarSway()
	{
		return lunarSway;
	}
	
	public void setLunarSway(@Nullable LunarSway lunarSway)
	{
		this.lunarSway = lunarSway;
	}
	
	@Nullable
	public EnumConsort getConsortType()
	{
		return consortType;
	}
	
	public void setConsortType(@Nullable EnumConsort consortType)
	{
		this.consortType = consortType;
	}
	
	@Nullable
	public ResourceLocation getGristHoard()
	{
		return gristHoard;
	}
	
	public void setGristHoard(@Nullable ResourceLocation gristHoard)
	{
		this.gristHoard = gristHoard;
	}
	
	@Override
	public CompoundTag serializeNBT(net.minecraft.core.HolderLookup.Provider registries)
	{
		CompoundTag nbt = new CompoundTag();
		nbt.putBoolean("GodTier", godTier);
		nbt.putBoolean("CanGodTier", canGodTier);
		nbt.putBoolean("ClimbedTheSpire", climbedTheSpire);
		if(pendingReset) nbt.putBoolean("PendingReset", true);
		if(pendingArmor) nbt.putBoolean("PendingArmor", true);
		if(masterControl != null) nbt.putBoolean("AllBadges", masterControl);
		nbt.putInt("MaxBadges", maxBadges);
		nbt.putInt("ScrollsUsed", scrollsUsed);
		nbt.putInt("LunarSway", lunarSway == null ? -1 : lunarSway.ordinal());
		nbt.putInt("ConsortType", consortType == null ? -1 : consortType.ordinal());
		if(gristHoard != null) nbt.putString("GristHoardType", gristHoard.toString());
		return nbt;
	}
	
	@Override
	public void deserializeNBT(net.minecraft.core.HolderLookup.Provider registries, CompoundTag nbt)
	{
		this.godTier = nbt.getBoolean("GodTier");
		this.canGodTier = !nbt.contains("CanGodTier") || nbt.getBoolean("CanGodTier");
		this.climbedTheSpire = nbt.getBoolean("ClimbedTheSpire");
		this.pendingReset = nbt.getBoolean("PendingReset");
		this.pendingArmor = nbt.getBoolean("PendingArmor");
		this.masterControl = nbt.contains("AllBadges") ? nbt.getBoolean("AllBadges") : null;
		this.maxBadges = nbt.contains("MaxBadges") ? nbt.getInt("MaxBadges") : -1;
		this.scrollsUsed = nbt.getInt("ScrollsUsed");
		int lunarSwayId = nbt.getInt("LunarSway");
		this.lunarSway = lunarSwayId >= 0 && lunarSwayId < LunarSway.values().length ? LunarSway.values()[lunarSwayId] : null;
		int consortId = nbt.getInt("ConsortType");
		this.consortType = consortId >= 0 && consortId < EnumConsort.values().length ? EnumConsort.values()[consortId] : null;
		this.gristHoard = nbt.contains("GristHoardType") ? ResourceLocation.tryParse(nbt.getString("GristHoardType")) : null;
	}
}
