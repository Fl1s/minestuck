package com.mraof.minestuck.player.dreamself;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * A snapshot of everything that belongs to one of the two "selves" of a player (the waking body or the dreamself).
 */
public final class SelfState
{
	public static final double BED_SURFACE = 0.5625;
	
	public final ResourceKey<Level> dimension;
	public final double x, y, z;
	public final float yRot, xRot;
	public final float health;
	public final CompoundTag food;
	public final ListTag inventory;
	public final int xpLevel, xpTotal;
	public final float xpProgress;
	@Nullable
	public final BlockPos bedPos;
	
	public SelfState(ResourceKey<Level> dimension, double x, double y, double z, float yRot, float xRot, float health, CompoundTag food, ListTag inventory, int xpLevel, int xpTotal, float xpProgress, @Nullable BlockPos bedPos)
	{
		this.dimension = dimension;
		this.x = x;
		this.y = y;
		this.z = z;
		this.yRot = yRot;
		this.xRot = xRot;
		this.health = health;
		this.food = food;
		this.inventory = inventory;
		this.xpLevel = xpLevel;
		this.xpTotal = xpTotal;
		this.xpProgress = xpProgress;
		this.bedPos = bedPos;
	}
	
	/**
	 * Captures the current state of the given player.
	 */
	public static SelfState capture(ServerPlayer player, @Nullable BlockPos bedPos)
	{
		CompoundTag food = new CompoundTag();
		player.getFoodData().addAdditionalSaveData(food);
		
		double y = player.getY();
		double x = player.getX(), z = player.getZ();
		if(bedPos != null)
		{
			x = bedPos.getX() + 0.5;
			y = bedPos.getY() + BED_SURFACE;
			z = bedPos.getZ() + 0.5;
		}
		
		return new SelfState(player.level().dimension(), x, y, z, player.getYRot(), player.getXRot(), player.getHealth(), food, player.getInventory().save(new ListTag()), player.experienceLevel, player.totalExperience, player.experienceProgress, bedPos);
	}
	
	/**
	 * @return a copy of this state at another position
	 */
	public SelfState withPosition(double x, double y, double z, float yRot)
	{
		return new SelfState(dimension, x, y, z, yRot, xRot, health, food, inventory, xpLevel, xpTotal, xpProgress, bedPos);
	}
	
	public CompoundTag save()
	{
		CompoundTag nbt = new CompoundTag();
		nbt.putString("dim", dimension.location().toString());
		nbt.putDouble("x", x);
		nbt.putDouble("y", y);
		nbt.putDouble("z", z);
		nbt.putFloat("yaw", yRot);
		nbt.putFloat("pitch", xRot);
		nbt.putFloat("health", health);
		nbt.put("food", food.copy());
		nbt.put("inv", inventory.copy());
		nbt.putInt("xp_level", xpLevel);
		nbt.putInt("xp_total", xpTotal);
		nbt.putFloat("xp_progress", xpProgress);
		if(bedPos != null) nbt.putLong("bed", bedPos.asLong());
		return nbt;
	}
	
	public static SelfState load(CompoundTag nbt)
	{
		ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(nbt.getString("dim")));
		return new SelfState(dim, nbt.getDouble("x"), nbt.getDouble("y"), nbt.getDouble("z"), nbt.getFloat("yaw"), nbt.getFloat("pitch"), nbt.getFloat("health"), nbt.getCompound("food"), nbt.getList("inv", Tag.TAG_COMPOUND), nbt.getInt("xp_level"), nbt.getInt("xp_total"), nbt.getFloat("xp_progress"), nbt.contains("bed", Tag.TAG_LONG) ? BlockPos.of(nbt.getLong("bed")) : null);
	}
	
	/**
	 * Helper for creating the default food data of a self that has never been played as.
	 */
	public static CompoundTag freshFood()
	{
		CompoundTag nbt = new CompoundTag();
		new FoodData().addAdditionalSaveData(nbt);
		return nbt;
	}
}
