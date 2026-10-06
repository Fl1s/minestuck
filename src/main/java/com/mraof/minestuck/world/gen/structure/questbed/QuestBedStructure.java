package com.mraof.minestuck.world.gen.structure.questbed;

import com.mojang.serialization.MapCodec;
import com.mraof.minestuck.world.gen.structure.MSStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Optional;

public class QuestBedStructure extends Structure
{
	public static final MapCodec<QuestBedStructure> CODEC = simpleCodec(QuestBedStructure::new);
	
	public QuestBedStructure(StructureSettings settings)
	{
		super(settings);
	}
	
	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context)
	{
		ChunkPos chunkPos = context.chunkPos();
		BlockPos center = new BlockPos(chunkPos.getMinBlockX() + 8, 0, chunkPos.getMinBlockZ() + 8);
		
		return Optional.of(new GenerationStub(center, builder -> generatePieces(builder, center)));
	}
	
	@Override
	public StructureType<?> type()
	{
		return MSStructures.QuestBed.TYPE.get();
	}
	
	private static void generatePieces(StructurePiecesBuilder builder, BlockPos center)
	{
		builder.addPiece(new QuestBedPiece(center));
	}
}
