package com.mraof.minestuck.world.gen.structure.questbed;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mraof.minestuck.world.gen.LandStructureState;
import com.mraof.minestuck.world.gen.structure.MSStructures;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
import java.util.Optional;

public final class QuestBedPlacement extends StructurePlacement
{
	private static final Logger LOGGER = LogManager.getLogger();
	
	public static final MapCodec<QuestBedPlacement> CODEC = RecordCodecBuilder.mapCodec(instance -> placementCodec(instance).apply(instance, QuestBedPlacement::new));
	
	public static QuestBedPiece findQuestBedPiece(ServerLevel level)
	{
		if(level.getChunkSource().getGeneratorState() instanceof LandStructureState landStructureState)
		{
			Structure questBed = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(MSStructures.QuestBed.KEY);
			Objects.requireNonNull(questBed, "Unable to find quest bed structure instance");
			
			ChunkPos chunkPos = landStructureState.getOrFindQuestBedPosition();
			StructureStart start = level.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.STRUCTURE_STARTS).getStartForStructure(questBed);
			
			if(start != null)
			{
				for(var piece : start.getPieces())
					if(piece instanceof QuestBedPiece questBedPiece) return questBedPiece;
				
				LOGGER.error("Did not find a quest bed piece in quest bed structure. Instead had components {}.", start.getPieces());
			} else
				LOGGER.warn("Expected to find quest bed structure at chunk coords {}, in dimension {}, but found nothing!", chunkPos, level.dimension());
		} else LOGGER.warn("No quest bed position could be found for dimension {}.", level.dimension());
		
		return null;
	}
	
	public QuestBedPlacement()
	{
		this(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1, 0, Optional.empty());
	}
	
	public QuestBedPlacement(Vec3i locateOffset, FrequencyReductionMethod frequencyReductionMethod, float frequency, int salt, @SuppressWarnings({"deprecation", "OptionalUsedAsFieldOrParameterType"}) Optional<ExclusionZone> exclusionZone)
	{
		super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone);
	}
	
	@Override
	public StructurePlacementType<QuestBedPlacement> type()
	{
		return MSStructures.QuestBed.PLACEMENT.get();
	}
	
	@Override
	public boolean isPlacementChunk(ChunkGeneratorStructureState structureState, int chunkX, int chunkZ)
	{
		if(structureState instanceof LandStructureState landState)
		{
			ChunkPos chunkPos = landState.getOrFindQuestBedPosition();
			return chunkPos.x == chunkX && chunkPos.z == chunkZ;
		} else return false;
	}
}
