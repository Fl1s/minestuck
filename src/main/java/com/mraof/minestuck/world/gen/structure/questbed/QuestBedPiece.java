package com.mraof.minestuck.world.gen.structure.questbed;

import com.mraof.minestuck.block.MSBlocks;
import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.world.gen.structure.MSStructures;
import com.mraof.minestuck.world.gen.structure.blocks.StructureBlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class QuestBedPiece extends StructurePiece
{
	public static final int BOTTOM = 64;
	public static final int TOP = 154;
	public static final int RADIUS = 32;
	
	private final BlockPos origin;
	
	public QuestBedPiece(BlockPos origin)
	{
		super(MSStructures.QuestBed.PIECE.get(), 0, makeBoundingBox(origin));
		this.origin = origin;
	}
	
	public QuestBedPiece(CompoundTag nbt)
	{
		super(MSStructures.QuestBed.PIECE.get(), nbt);
		this.origin = new BlockPos(nbt.getInt("OriginX"), nbt.getInt("OriginY"), nbt.getInt("OriginZ"));
	}
	
	private static BoundingBox makeBoundingBox(BlockPos origin)
	{
		int margin = 8;
		return new BoundingBox(origin.getX() - RADIUS - margin, BOTTOM - 2, origin.getZ() - RADIUS - margin, origin.getX() + RADIUS + margin, TOP + 10, origin.getZ() + RADIUS + margin);
	}
	
	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag)
	{
		tag.putInt("OriginX", origin.getX());
		tag.putInt("OriginY", origin.getY());
		tag.putInt("OriginZ", origin.getZ());
	}
	
	@Override
	public void postProcess(WorldGenLevel level, StructureManager manager, ChunkGenerator generator, RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos)
	{
		StructureBlockRegistry blocks = StructureBlockRegistry.getOrDefault(generator);
		BlockState ground = blocks.getBlockState(StructureBlockRegistry.GROUND);
		
		EnumAspect aspect = QuestBedAspectHook.getLandOwnerAspect(level);
		BlockState wall = aspect == null ? MSBlocks.WILDCARD_HERO_STONE_WALL.get().defaultBlockState() : MSBlocks.HERO_STONE_WALL.get(aspect).get().defaultBlockState();
		BlockState heroStone = aspect == null ? MSBlocks.WILDCARD_HERO_STONE.get().defaultBlockState() : MSBlocks.HERO_STONE.get(aspect).get().defaultBlockState();
		BlockState chiseledHeroStone = aspect == null ? MSBlocks.WILDCARD_CHISELED_HERO_STONE.get().defaultBlockState() : MSBlocks.CHISELED_HERO_STONE.get(aspect).get().defaultBlockState();
		BlockState light = MSBlocks.GLOWING_HERO_STONE.get().defaultBlockState();
		
		generateMountain(level, box, random, ground);
		generateSummitPlatform(level, box, ground);
		generateAltar(level, box, heroStone, chiseledHeroStone, wall, light);
	}
	
	private void generateMountain(WorldGenLevel level, BoundingBox box, RandomSource random, BlockState ground)
	{
		double j = 1;
		for(double i = 1; i < TOP - BOTTOM; i = i > 32 ? Math.floor(i + 1) : i + Math.min(1, i / 32d))
		{
			int blockSize = Math.max(3, 16 - (int) (i * 10d / (TOP - BOTTOM)));
			
			int x = (int) (origin.getX() + Math.cos(j / 8d * (Math.PI * 2d)) * Math.max(5, RADIUS - i * 0.3d));
			int z = (int) (origin.getZ() + Math.sin(j / 8d * (Math.PI * 2d)) * Math.max(5, RADIUS - i * 0.3d));
			int y = BOTTOM + (int) i;
			
			for(int w = 0; w < blockSize; w++)
				for(int d = 0; d < blockSize; d++)
					for(int h = 0; h < Math.min(32, 9 + i); h++)
					{
						BlockPos target = new BlockPos(x + w, y - h, z + d);
						if(ground.canOcclude() || !level.getBlockState(target).blocksMotion())
							place(level, box, target, ground);
					}
			
			j += j / 48d;
		}
	}
	
	private void generateSummitPlatform(WorldGenLevel level, BoundingBox box, BlockState ground)
	{
		for(int yOff = 1; yOff <= 16; yOff++)
			for(int xOff = -7; xOff <= 7; xOff++)
			{
				int z2 = (int) Math.sqrt(Math.pow(7, 2) - Math.pow(xOff, 2));
				for(int zOff = -z2; zOff < z2; zOff++)
					place(level, box, origin.getX() + xOff + 2, TOP - yOff, origin.getZ() + zOff + 2, ground);
			}
	}
	
	private void generateAltar(WorldGenLevel level, BoundingBox box, BlockState heroStone, BlockState chiseledHeroStone, BlockState wall, BlockState light)
	{
		int ax = origin.getX() + 2;
		int az = origin.getZ() + 1;
		
		for(int xOff = -1; xOff <= 1; xOff++)
			for(int zOff = -1; zOff <= 2; zOff++)
				place(level, box, ax + xOff, TOP, az + zOff, (xOff == 0 && zOff == 0) ? chiseledHeroStone : heroStone);
		
		for(int yOff = 0; yOff < 2; yOff++)
			for(int xOff = -1; xOff <= 1; xOff++)
				place(level, box, ax + xOff, TOP + yOff, az - 2, wall);
		place(level, box, ax, TOP + 2, az - 2, wall);
		
		for(int yOff = 0; yOff < 7; yOff++)
		{
			place(level, box, ax - 2, TOP + yOff, az + 3, wall);
			place(level, box, ax + 2, TOP + yOff, az + 3, wall);
			place(level, box, ax + 2, TOP + yOff, az - 2, wall);
			place(level, box, ax - 2, TOP + yOff, az - 2, wall);
		}
		place(level, box, ax - 2, TOP + 7, az + 3, heroStone);
		place(level, box, ax + 2, TOP + 7, az + 3, heroStone);
		place(level, box, ax + 2, TOP + 7, az - 2, heroStone);
		place(level, box, ax - 2, TOP + 7, az - 2, heroStone);
	}
	
	public BlockPos getOrigin()
	{
		return origin;
	}
	
	public BlockPos[] getPillarLightPositions()
	{
		int ax = origin.getX() + 2;
		int az = origin.getZ() + 1;
		return new BlockPos[]{new BlockPos(ax - 2, TOP + 7, az + 3), new BlockPos(ax + 2, TOP + 7, az + 3), new BlockPos(ax + 2, TOP + 7, az - 2), new BlockPos(ax - 2, TOP + 7, az - 2)};
	}
	
	public BlockPos getAltarCenter()
	{
		return new BlockPos(origin.getX() + 2, TOP, origin.getZ() + 1);
	}
	
	private static void place(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state)
	{
		place(level, box, new BlockPos(x, y, z), state);
	}
	
	private static void place(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state)
	{
		if(box.isInside(pos)) level.setBlock(pos, state, Block.UPDATE_CLIENTS);
	}
}
