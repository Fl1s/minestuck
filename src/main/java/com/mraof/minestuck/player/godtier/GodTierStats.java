package com.mraof.minestuck.player.godtier;

import com.mraof.minestuck.MinestuckConfig;
import com.mraof.minestuck.player.EnumClass;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

public final class GodTierStats implements INBTSerializable<CompoundTag>
{
	private static final class Data
	{
		int level;
		float xp;
	}
	
	private final Map<GodTierStat, Data> data = new EnumMap<>(GodTierStat.class);
	
	public GodTierStats()
	{
		for(GodTierStat stat : GodTierStat.values())
			data.put(stat, new Data());
	}
	
	public int getLevel(GodTierStat stat)
	{
		return data.get(stat).level;
	}
	
	public float getXp(GodTierStat stat)
	{
		return data.get(stat).xp;
	}
	
	public void initializeOnAscension()
	{
		Data general = data.get(GodTierStat.GENERAL);
		if(general.level < 1) general.level = 1;
	}
	
	public void addXp(GodTierStat stat, float amount, @Nullable EnumClass heroClass)
	{
		if(amount <= 0) return;
		addTo(stat, amount, heroClass);
		if(stat != GodTierStat.GENERAL) addTo(GodTierStat.GENERAL, amount, heroClass);
	}
	
	private void addTo(GodTierStat stat, float amount, @Nullable EnumClass heroClass)
	{
		Data d = data.get(stat);
		if(stat == GodTierStat.GENERAL)
		{
			int maxLevel = MinestuckConfig.SERVER.maxGodTier.get();
			if(maxLevel >= 0 && d.level >= maxLevel)
				return;
		}

		d.xp += amount;
		int needed;
		while(d.xp >= (needed = getXpToNextLevel(stat, heroClass)))
		{
			if(stat == GodTierStat.GENERAL)
			{
				int maxLevel = MinestuckConfig.SERVER.maxGodTier.get();
				if(maxLevel >= 0 && d.level >= maxLevel)
				{
					d.xp = 0;
					return;
				}
			}
			d.xp -= needed;
			d.level++;
		}
	}
	
	public void reset(GodTierStat stat)
	{
		Data d = data.get(stat);
		d.level = 0;
		d.xp = 0;
	}
	
	public void resetAll()
	{
		for(GodTierStat stat : GodTierStat.values())
			reset(stat);
	}
	
	public double getAttributeBonus(GodTierStat stat, double multiplier)
	{
		return Math.pow(getLevel(stat), 0.765) * stat.attributeMod() * multiplier;
	}
	
	public int getXpToNextLevel(GodTierStat stat, @Nullable EnumClass heroClass)
	{
		if(heroClass == null) return 50;
		int x = getLevel(stat) + 1;
		int result = switch(heroClass)
		{
			case KNIGHT, PRINCE -> (int) (Math.exp((x - 1) * 0.04) * 50);                    // exponential
			case HEIR, MAID -> (int) Math.min(2500, Math.pow(x - 1, 2) + 50);                // parabolic
			case MUSE, SEER, MAGE ->
					(int) (4 * Math.sin(x - 1) + 2 * x) * 5 + 50;           // fluctuating (cast binds before *5, as in 1.12.2)
			case BARD, WITCH, SYLPH -> (int) (x * 12 + Math.sin(2 * (x - 1)) * 10 + 25);     // erratic
			case THIEF, ROGUE -> 10 * Math.min(x + (x % 10), x + ((10 - x) % 10)) + 50;     // step
			case PAGE -> 14 * x + 50;
			case LORD -> 50 * x;
		};
		return Math.max(1, result);
	}
	
	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider registries)
	{
		CompoundTag nbt = new CompoundTag();
		for(GodTierStat stat : GodTierStat.values())
		{
			CompoundTag tag = new CompoundTag();
			tag.putInt("Level", data.get(stat).level);
			tag.putFloat("Xp", data.get(stat).xp);
			nbt.put(stat.getSerializedName(), tag);
		}
		return nbt;
	}
	
	@Override
	public void deserializeNBT(HolderLookup.Provider registries, CompoundTag nbt)
	{
		for(GodTierStat stat : GodTierStat.values())
		{
			CompoundTag tag = nbt.getCompound(stat.getSerializedName());
			data.get(stat).level = tag.getInt("Level");
			data.get(stat).xp = tag.getFloat("Xp");
		}
	}
}