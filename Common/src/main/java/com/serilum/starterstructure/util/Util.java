package com.serilum.starterstructure.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.natamus.collective.data.GlobalVariables;
import com.natamus.collective.functions.*;
import com.natamus.collective.schematic.ParseSchematicFile;
import com.natamus.collective.schematic.ParsedSchematicObject;
import com.serilum.starterstructure.config.ConfigHandler;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Util {
	public static HashMap<ResourceKey<Level>, List<BlockPos>> protectedMap = new HashMap<>();

	private static final String dirpath = DataFunctions.getConfigDirectory() + File.separator + Reference.MOD_ID;
	private static final File schematicDir = new File(dirpath + File.separator + "schematics");
	private static final File signDataDir = new File(dirpath + File.separator + "signdata");

	private static final Logger logger = LogUtils.getLogger();
	private static final String logPrefix = "[" + Reference.NAME + "] ";

	private static final int maxGapFillDepth = 16;
	private static final int maxTreePartSize = 1024;

	private static boolean isAwaitingStructure = false;
	private static final List<ServerPlayer> playersAwaitingStructure = new ArrayList<>();

	public static boolean initDirs() {
		if (!schematicDir.isDirectory()) {
			if (!schematicDir.mkdirs()) {
				return false;
			}
		}
		if (!signDataDir.isDirectory()) {
			return signDataDir.mkdirs();
		}
		return true;
	}

	public static BlockPos generateSchematic(ServerLevel serverLevel) {
		return generateSchematic(serverLevel, serverLevel.getSharedSpawnPos(), null);
	}

	public static void generateSchematicForArrivingPlayer(ServerLevel serverLevel, ServerPlayer serverPlayer) {
		if (!hasGeneratedStructure(serverLevel)) {
			generateSchematic(serverLevel, serverPlayer.blockPosition(), serverPlayer);
		}
	}

	public static BlockPos generateSchematic(ServerLevel serverLevel, BlockPos originPos, ServerPlayer arrivingPlayer) {
		if (!schematicDir.isDirectory()) {
			if (!initDirs()) {
				logger.info(logPrefix + "Unable to generate directories.");
				return null;
			}
		}

		List<File> listOfSchematicFiles = new ArrayList<>();

		File[] listOfFiles = schematicDir.listFiles();
		for (File file : listOfFiles) {
			if (file.getName().endsWith(".schem") || file.getName().endsWith(".schematic") || file.getName().endsWith(".nbt")) {
				listOfSchematicFiles.add(file);
			}
		}

		if (listOfSchematicFiles.isEmpty()) {
			logger.info(logPrefix + "No schematics found to generate the starter structure.");
			return null;
		}

		File schematicFile = listOfSchematicFiles.get(GlobalVariables.random.nextInt(listOfSchematicFiles.size()));
		if (!schematicFile.isFile()) {
			logger.info(logPrefix + "Unable to find starter structure file.");
			return null;
		}

		boolean automaticCenter = schematicFile.getName().endsWith(".nbt");
		boolean fitToTerrain = shouldFitStructureToTerrain(serverLevel);

		BlockPos structurePos = getStructurePos(serverLevel, originPos);
		if (fitToTerrain) {
			structurePos = getTerrainFittedPos(serverLevel, schematicFile, structurePos, automaticCenter);
		}

		ParsedSchematicObject parsedSchematicObject = parseSchematic(schematicFile, serverLevel, structurePos, automaticCenter);
		if (parsedSchematicObject == null) {
			return null;
		}

		writeGeneratedMarker(serverLevel, schematicFile.getName());

		BlockPos finalStructurePos = structurePos;
		MinecraftServer minecraftServer = serverLevel.getServer();

		minecraftServer.execute(() -> {
			List<BlockPos> placedPositions = placeStructureBlocks(serverLevel, parsedSchematicObject);
			if (fitToTerrain) {
				fillGapsBelowStructure(serverLevel, placedPositions);
			}
			if (ConfigHandler.ignoreTreesDuringStructurePlacement) {
				removeFloatingTreeParts(serverLevel, placedPositions);
			}

			if (ConfigHandler.protectStructureBlocks) {
				writeProtectedList(serverLevel, placedPositions);
			}

			minecraftServer.execute(() -> {
				if (ConfigHandler.generateJigsawStructures) {
					generateJigsawStructures(serverLevel, parsedSchematicObject);
				}

				Registry<EntityType<?>> entityTypeRegistry = serverLevel.registryAccess().registryOrThrow(Registries.ENTITY_TYPE);
				for (Pair<BlockPos, BlockEntity> blockEntityPair : parsedSchematicObject.getBlockEntities(serverLevel)) {
					BlockPos blockPos = blockEntityPair.getFirst();
					BlockEntity blockEntity = blockEntityPair.getSecond();
					if (blockEntity instanceof SignBlockEntity signBlockEntity) {
						List<String> signLines = SignFunctions.getSignText(signBlockEntity);

						String firstLine = signLines.get(0);
						signLines.remove(0);
						String signContent = String.join("", signLines);

						Entity newEntity = null;
						if (firstLine.contains("[Mob]") || firstLine.contains("[Entity]")) {
							EntityType<?> entityType = entityTypeRegistry.get(new ResourceLocation(signContent));
							if (entityType != null) {
								newEntity = entityType.create(serverLevel);
							}
						}
						else if (firstLine.contains("[NBT]")) {
							String nbtFilePath = signDataDir + File.separator + signContent + ".txt";

							File nbtTextFile = new File(nbtFilePath);
							if (nbtTextFile.isFile()) {
								int n = 1;
								while (n >= 0) {
									String rawNBT = "";
									try {
										rawNBT = new String(Files.readAllBytes(Paths.get(nbtFilePath)));

										CompoundTag entityCompoundTag = TagParser.parseTag(rawNBT);
										Optional<Entity> optionalNewEntity = EntityType.create(entityCompoundTag, serverLevel);
										if (optionalNewEntity.isPresent()) {
											if (n != 1) {
												logger.info(logPrefix + "Unable to parse the {}.txt entitydata file. Attempting automatic fix. (1)", signContent);
											}

											newEntity = optionalNewEntity.get();
											n = -1;
										}
									} catch (Exception ex) {
										logger.info(logPrefix + "Unable to parse the {}.txt entitydata file. Attempting automatic fix. (2)", signContent);
										try {
											attemptEntityDataFileFix(nbtFilePath, rawNBT);
										}
										catch (IOException ignored) { }
									}

									n-=1;
								}
							}
						}
						else {
							continue;
						}

						if (newEntity != null) {
							newEntity.getTags().add(Reference.MOD_ID + ".protected");
							newEntity.setPos(blockPos.getX()+0.5, blockPos.getY(), blockPos.getZ()+0.5);
							serverLevel.addFreshEntity(newEntity);
							serverLevel.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 3);
						}
					}
				}

				if (ConfigHandler.spawnNonSignEntitiesFromSupportedSchematics) {
					if (!parsedSchematicObject.entities.isEmpty()) {
						for (Pair<BlockPos, Entity> entityPair : parsedSchematicObject.entities) {
							Entity newEntity = entityPair.getSecond();
							markAsProtected(newEntity);
							for (Entity passenger : newEntity.getIndirectPassengers()) {
								markAsProtected(passenger);
							}
							serverLevel.addFreshEntityWithPassengers(newEntity);
						}
					}
				}

				minecraftServer.execute(() -> {
					BlockPos spawnPos = getNearestFreeSpace(serverLevel, getStructureSpawnPos(finalStructurePos));
					if (arrivingPlayer == null) {
						serverLevel.setDefaultSpawnPos(spawnPos, serverLevel.getSharedSpawnAngle());
						teleportPlayersAwaitingStructure(spawnPos);
					}
					else {
						arrivingPlayer.teleportTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
					}
				});
			});
		});

		return structurePos;
	}

	private static BlockPos getStructurePos(ServerLevel serverLevel, BlockPos originPos) {
		BlockPos structurePos = originPos;

		if (ConfigHandler.shouldUseStructurePosition) {
			structurePos = new BlockPos(ConfigHandler.generatedStructureXPosition, ConfigHandler.generatedStructureYPosition, ConfigHandler.generatedStructureZPosition);
		}

		if (ConfigHandler.ignoreTreesDuringStructurePlacement && (!ConfigHandler.shouldUseStructurePosition || ConfigHandler.generatedStructureYPosition == 0) && !serverLevel.dimensionType().hasCeiling()) {
			structurePos = getSpawnPos(serverLevel, BlockPosFunctions.getSurfaceBlockPos(serverLevel, structurePos.getX(), structurePos.getZ(), true), false, false);
		}

		if (ConfigHandler.shouldUseStructureOffset) {
			structurePos = structurePos.offset(ConfigHandler.generatedStructureXOffset, ConfigHandler.generatedStructureYOffset, ConfigHandler.generatedStructureZOffset).immutable();
		}
		return structurePos;
	}

	private static ParsedSchematicObject parseSchematic(File schematicFile, ServerLevel serverLevel, BlockPos structurePos, boolean automaticCenter) {
		ParsedSchematicObject parsedSchematicObject;
		try (FileInputStream fileInputStream = new FileInputStream(schematicFile)){
			parsedSchematicObject = ParseSchematicFile.getParsedSchematicObject(fileInputStream, serverLevel, structurePos, 0, false, automaticCenter);
		}
		catch (Exception ex) {
			logger.info(logPrefix + "Exception while attempting to parse schematic file.");
			ex.printStackTrace();
			return null;
		}

		if (parsedSchematicObject == null || !parsedSchematicObject.parsedCorrectly) {
			logger.info(logPrefix + "The starter structure object was not parsed correctly.");
			return null;
		}
		return parsedSchematicObject;
	}

	public static boolean generatesAtWorldSpawn() {
		return getStructureDimensionName().equals("minecraft:overworld");
	}

	public static boolean shouldGenerateForArrivingPlayer(ServerLevel serverLevel) {
		if (!ConfigHandler.shouldGenerateStructure || generatesAtWorldSpawn()) {
			return false;
		}
		return WorldFunctions.getWorldDimensionName(serverLevel).equals(getStructureDimensionName()) && !hasGeneratedStructure(serverLevel);
	}

	private static String getStructureDimensionName() {
		ResourceLocation dimensionIdentifier = ResourceLocation.tryParse(ConfigHandler.structureDimension.strip());
		if (dimensionIdentifier == null) {
			return "minecraft:overworld";
		}
		return dimensionIdentifier.toString();
	}

	private static boolean shouldFitStructureToTerrain(ServerLevel serverLevel) {
		if (!ConfigHandler.fitStructureToTerrain || serverLevel.dimensionType().hasCeiling()) {
			return false;
		}
		return !ConfigHandler.shouldUseStructurePosition || ConfigHandler.generatedStructureYPosition == 0;
	}

	private static BlockPos getTerrainFittedPos(ServerLevel serverLevel, File schematicFile, BlockPos structurePos, boolean automaticCenter) {
		ParsedSchematicObject parsedSchematicObject = parseSchematic(schematicFile, serverLevel, structurePos, automaticCenter);
		if (parsedSchematicObject == null || parsedSchematicObject.blocks.isEmpty()) {
			return structurePos;
		}

		int heightChange = getFootprintSurfaceY(serverLevel, parsedSchematicObject) - getSurfaceY(serverLevel, structurePos);
		return structurePos.above(heightChange);
	}

	public static int getFootprintSurfaceY(ServerLevel serverLevel, ParsedSchematicObject parsedSchematicObject) {
		List<Integer> surfaceHeights = new ArrayList<>();
		for (BlockPos columnPos : getFootprintColumns(parsedSchematicObject)) {
			surfaceHeights.add(getSurfaceY(serverLevel, columnPos));
		}

		Collections.sort(surfaceHeights);
		return surfaceHeights.get(surfaceHeights.size() / 2);
	}

	private static Set<BlockPos> getFootprintColumns(ParsedSchematicObject parsedSchematicObject) {
		Set<BlockPos> allColumns = new HashSet<>();
		Set<BlockPos> buildingColumns = new HashSet<>();
		for (Pair<BlockPos, BlockState> blockPair : parsedSchematicObject.blocks) {
			BlockPos columnPos = new BlockPos(blockPair.getFirst().getX(), 0, blockPair.getFirst().getZ());
			allColumns.add(columnPos);
			if (!blockPair.getSecond().isAir()) {
				buildingColumns.add(columnPos);
			}
		}

		if (buildingColumns.isEmpty()) {
			return allColumns;
		}
		return buildingColumns;
	}

	private static int getSurfaceY(ServerLevel serverLevel, BlockPos pos) {
		return BlockPosFunctions.getSurfaceBlockPos(serverLevel, pos.getX(), pos.getZ(), ConfigHandler.ignoreTreesDuringStructurePlacement).getY();
	}

	public static void fillGapsBelowStructure(ServerLevel serverLevel, List<BlockPos> placedPositions) {
		if (placedPositions.isEmpty()) {
			return;
		}

		int bottomY = placedPositions.get(0).getY();
		for (BlockPos placedPos : placedPositions) {
			bottomY = Math.min(bottomY, placedPos.getY());
		}

		for (BlockPos placedPos : placedPositions) {
			if (placedPos.getY() == bottomY && !serverLevel.getBlockState(placedPos).isAir()) {
				fillGapBelow(serverLevel, placedPos.below());
			}
		}
	}

	private static void fillGapBelow(ServerLevel serverLevel, BlockPos topPos) {
		List<BlockPos> gapPositions = new ArrayList<>();
		BlockPos pos = topPos;
		while (isGap(serverLevel, pos)) {
			if (gapPositions.size() >= maxGapFillDepth) {
				return;
			}

			gapPositions.add(pos);
			pos = pos.below();
		}

		BlockState fillState = getGapFillState(serverLevel, pos);
		for (BlockPos gapPos : gapPositions) {
			serverLevel.setBlock(gapPos, fillState, Block.UPDATE_ALL);
		}
	}

	private static boolean isGap(ServerLevel serverLevel, BlockPos pos) {
		BlockState blockState = serverLevel.getBlockState(pos);
		return blockState.canBeReplaced() || CompareBlockFunctions.isTreeLeaf(blockState.getBlock());
	}

	private static BlockState getGapFillState(ServerLevel serverLevel, BlockPos groundPos) {
		BlockState groundState = serverLevel.getBlockState(groundPos);
		if (groundState.is(BlockTags.DIRT) || groundState.hasBlockEntity() || !groundState.isCollisionShapeFullBlock(serverLevel, groundPos)) {
			return Blocks.DIRT.defaultBlockState();
		}
		return groundState;
	}

	public static void removeFloatingTreeParts(ServerLevel serverLevel, List<BlockPos> placedPositions) {
		Set<BlockPos> structurePositions = new HashSet<>(placedPositions);
		Set<BlockPos> checkedPositions = new HashSet<>();

		for (BlockPos placedPos : placedPositions) {
			for (Direction direction : Direction.values()) {
				BlockPos neighbourPos = placedPos.relative(direction);
				if (structurePositions.contains(neighbourPos) || checkedPositions.contains(neighbourPos) || !isTreeBlock(serverLevel, neighbourPos)) {
					continue;
				}

				List<BlockPos> treePart = getConnectedTreePart(serverLevel, neighbourPos, structurePositions);
				checkedPositions.addAll(treePart);
				if (treePart.size() < maxTreePartSize && !restsOnGround(serverLevel, treePart, structurePositions)) {
					for (BlockPos treePos : treePart) {
						serverLevel.setBlock(treePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
					}
				}
			}
		}
	}

	private static List<BlockPos> getConnectedTreePart(ServerLevel serverLevel, BlockPos startPos, Set<BlockPos> structurePositions) {
		List<BlockPos> treePart = new ArrayList<>();
		Set<BlockPos> visitedPositions = new HashSet<>();
		Deque<BlockPos> positionsToVisit = new ArrayDeque<>();
		positionsToVisit.add(startPos);
		visitedPositions.add(startPos);

		while (!positionsToVisit.isEmpty() && treePart.size() < maxTreePartSize) {
			BlockPos treePos = positionsToVisit.poll();
			treePart.add(treePos);

			for (BlockPos aroundPos : BlockPos.betweenClosed(treePos.offset(-1, -1, -1), treePos.offset(1, 1, 1))) {
				BlockPos nextPos = aroundPos.immutable();
				if (!visitedPositions.contains(nextPos) && !structurePositions.contains(nextPos) && isTreeBlock(serverLevel, nextPos)) {
					visitedPositions.add(nextPos);
					positionsToVisit.add(nextPos);
				}
			}
		}
		return treePart;
	}

	private static boolean restsOnGround(ServerLevel serverLevel, List<BlockPos> treePart, Set<BlockPos> structurePositions) {
		for (BlockPos treePos : treePart) {
			BlockPos belowPos = treePos.below();
			if (!structurePositions.contains(belowPos) && !isTreeBlock(serverLevel, belowPos) && !serverLevel.getBlockState(belowPos).canBeReplaced()) {
				return true;
			}
		}
		return false;
	}

	private static boolean isTreeBlock(ServerLevel serverLevel, BlockPos pos) {
		Block block = serverLevel.getBlockState(pos).getBlock();
		return CompareBlockFunctions.isTreeLog(block) || CompareBlockFunctions.isTreeLeaf(block);
	}

	private static void markAsProtected(Entity entity) {
		entity.getTags().add(Reference.MOD_ID + ".protected");
	}

	public static boolean isAwaitingStructure() {
		return isAwaitingStructure;
	}

	public static void setAwaitingStructure(boolean awaitingStructure) {
		isAwaitingStructure = awaitingStructure;
		if (!awaitingStructure) {
			playersAwaitingStructure.clear();
		}
	}

	public static void addPlayerAwaitingStructure(ServerPlayer serverPlayer) {
		playersAwaitingStructure.add(serverPlayer);
	}

	private static void teleportPlayersAwaitingStructure(BlockPos spawnPos) {
		for (ServerPlayer serverPlayer : playersAwaitingStructure) {
			if (!serverPlayer.isRemoved()) {
				serverPlayer.teleportTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
			}
		}
		setAwaitingStructure(false);
	}

	public static BlockPos getStructureSpawnPos(BlockPos structurePos) {
		if (ConfigHandler.shouldUseSpawnCoordOffsets) {
			return structurePos.offset(ConfigHandler.spawnXCoordOffset, ConfigHandler.spawnYCoordOffset, ConfigHandler.spawnZCoordOffset).immutable();
		}
		return structurePos.immutable();
	}

	public static List<BlockPos> placeStructureBlocks(ServerLevel serverLevel, ParsedSchematicObject parsedSchematicObject) {
		List<BlockPos> placedPositions = new ArrayList<>();
		Set<BlockPos> blockEntityPositions = new HashSet<>(parsedSchematicObject.blockEntityPositions);

		logger.info(logPrefix + "Generating starter structure with " + parsedSchematicObject.blocks.size() + " blocks.");
		for (Pair<BlockPos, BlockState> blockPair : parsedSchematicObject.blocks) {
			BlockPos blockPos = blockPair.getFirst();
			BlockState blockState = blockPair.getSecond();
			if (isIgnoredStructureBlock(blockState.getBlock())) {
				continue;
			}

			serverLevel.setBlock(blockPos, blockState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
			if (blockEntityPositions.contains(blockPos)) {
				parsedSchematicObject.placeBlockEntityInWorld(serverLevel, blockPos);
			}

			placedPositions.add(blockPos);
		}

		for (BlockPos placedPos : placedPositions) {
			updateNeighbours(serverLevel, placedPos);
		}

		return placedPositions;
	}

	private static boolean isIgnoredStructureBlock(Block block) {
		if (block instanceof JigsawBlock) {
			return ConfigHandler.generationIgnoreJigsawAndStructureBlocks && !ConfigHandler.generateJigsawStructures;
		}
		if (block instanceof StructureBlock || block instanceof StructureVoidBlock) {
			return ConfigHandler.generationIgnoreJigsawAndStructureBlocks;
		}
		return false;
	}

	private static void updateNeighbours(ServerLevel serverLevel, BlockPos pos) {
		BlockState blockState = serverLevel.getBlockState(pos);
		serverLevel.updateNeighborsAt(pos, blockState.getBlock());
		blockState.updateNeighbourShapes(serverLevel, pos, Block.UPDATE_ALL);
	}

	private static void generateJigsawStructures(ServerLevel serverLevel, ParsedSchematicObject parsedSchematicObject) {
		for (Pair<BlockPos, BlockEntity> blockEntityPair : parsedSchematicObject.getBlockEntities(serverLevel)) {
			if (blockEntityPair.getSecond() instanceof JigsawBlockEntity jigsawBlockEntity) {
				generateJigsawStructure(serverLevel, jigsawBlockEntity);
			}
		}
	}

	public static void generateJigsawStructure(ServerLevel serverLevel, JigsawBlockEntity jigsawBlockEntity) {
		boolean keepJigsawBlocks = !ConfigHandler.generationIgnoreJigsawAndStructureBlocks;
		BlockPos jigsawPos = jigsawBlockEntity.getBlockPos();

		if (!jigsawBlockEntity.getPool().equals(Pools.EMPTY)) {
			try {
				jigsawBlockEntity.generate(serverLevel, ConfigHandler.jigsawGenerationLevels, keepJigsawBlocks);
			}
			catch (Exception ex) {
				logger.info(logPrefix + "Unable to generate the jigsaw structure of the jigsaw block at " + jigsawPos.toShortString() + ".");
			}
		}

		if (!keepJigsawBlocks) {
			serverLevel.setBlock(jigsawPos, getJigsawFinalState(serverLevel, jigsawBlockEntity), Block.UPDATE_ALL);
		}
	}

	private static BlockState getJigsawFinalState(ServerLevel serverLevel, JigsawBlockEntity jigsawBlockEntity) {
		BlockState finalState;
		try {
			finalState = BlockStateParser.parseForBlock(serverLevel.holderLookup(Registries.BLOCK), jigsawBlockEntity.getFinalState(), true).blockState();
		}
		catch (CommandSyntaxException ignored) {
			return Blocks.AIR.defaultBlockState();
		}

		if (finalState.is(Blocks.STRUCTURE_VOID)) {
			return Blocks.AIR.defaultBlockState();
		}
		return finalState;
	}

	public static BlockPos getNearestFreeSpace(ServerLevel serverLevel, BlockPos pos) {
		if (hasRoomToStand(serverLevel, pos)) {
			return pos;
		}

		BlockPos nearestPos = null;
		for (BlockPos aroundPos : BlockPos.betweenClosed(pos.offset(-10, -10, -10), pos.offset(10, 10, 10))) {
			if (hasRoomToStand(serverLevel, aroundPos) && (nearestPos == null || isNearerSpawnPos(aroundPos, nearestPos, pos))) {
				nearestPos = aroundPos.immutable();
			}
		}

		if (nearestPos == null) {
			return pos;
		}
		return nearestPos;
	}

	private static boolean isNearerSpawnPos(BlockPos candidatePos, BlockPos nearestPos, BlockPos targetPos) {
		double candidateDistance = candidatePos.distSqr(targetPos);
		double nearestDistance = nearestPos.distSqr(targetPos);
		if (candidateDistance == nearestDistance) {
			return candidatePos.getY() > nearestPos.getY();
		}
		return candidateDistance < nearestDistance;
	}

	private static boolean hasRoomToStand(Level level, BlockPos pos) {
		return isPassable(level, pos) && isPassable(level, pos.above());
	}

	private static boolean isPassable(Level level, BlockPos pos) {
		BlockState blockState = level.getBlockState(pos);
		return blockState.getCollisionShape(level, pos).isEmpty() && blockState.getFluidState().isEmpty();
	}

	private static void attemptEntityDataFileFix(String nbtFilePath, String rawNBT) throws IOException {
		String[] rawSplit = rawNBT.split("\\{", 2);
		if (rawSplit.length > 1) {
			String prefix = rawSplit[0];
			String newRawNBT = rawSplit[1];

			String idValue = "";
			String[] prefixSplit = prefix.strip().split("\\s+");
			for (int i = 0; i < prefixSplit.length; i++) {
				String word = prefixSplit[i];
				if (word.endsWith("summon") && i + 1 < prefixSplit.length) {
					idValue = "id:\"" + prefixSplit[i + 1] + "\",";
					break;
				}
				if (word.contains(":")) {
					idValue = "id:\"" + word + "\",";
				}
			}

			if (!idValue.isEmpty()) {
				newRawNBT = "{" + idValue + newRawNBT;
				Files.write(Path.of(nbtFilePath), newRawNBT.getBytes());
			}
		}
	}

	public static boolean hasBonusChestInChunk(ServerLevel serverLevel, BlockPos pos) {
		for (BlockEntity blockEntity : serverLevel.getChunkAt(pos).getBlockEntities().values()) {
			if (blockEntity instanceof RandomizableContainerBlockEntity) {
				String lootTable = blockEntity.saveWithoutMetadata().getString(RandomizableContainerBlockEntity.LOOT_TABLE_TAG);
				if (lootTable.equals(BuiltInLootTables.SPAWN_BONUS_CHEST.toString())) {
					return true;
				}
			}
		}
		return false;
	}

	private static String getWorldDataPath(ServerLevel serverLevel) {
		return WorldFunctions.getWorldPath(serverLevel) + File.separator + "data" + File.separator + Reference.MOD_ID;
	}

	public static boolean hasGeneratedStructure(ServerLevel serverLevel) {
		String worldDataPath = getWorldDataPath(serverLevel);
		return new File(worldDataPath + File.separator + "generated.txt").isFile() || new File(worldDataPath + File.separator + "protection").isDirectory();
	}

	private static void writeGeneratedMarker(ServerLevel serverLevel, String schematicName) {
		File worldDataDir = new File(getWorldDataPath(serverLevel));
		if (!worldDataDir.isDirectory()) {
			worldDataDir.mkdirs();
		}

		try {
			FileWriter writer = new FileWriter(worldDataDir + File.separator + "generated.txt");
			writer.write(schematicName + "\n");
			writer.close();
		}
		catch (IOException ignored) { }
	}

	private static void writeProtectedList(ServerLevel serverLevel, List<BlockPos> protectedList) {
		protectedMap.put(serverLevel.dimension(), protectedList);

		String protectedPath = WorldFunctions.getWorldPath(serverLevel) + File.separator + "data" + File.separator + Reference.MOD_ID + File.separator + "protection" + File.separator + DimensionFunctions.getSimpleDimensionString(serverLevel);
		File protectedPathDir = new File(protectedPath);

		if (!protectedPathDir.isDirectory()) {
			protectedPathDir.mkdirs();
		}

		try {
			FileWriter writer = new FileWriter(protectedPath + File.separator + "blocks.txt");

			for (BlockPos protectedPos : protectedList) {
				String coordinateString = protectedPos.getX() + "," + protectedPos.getY() + "," + protectedPos.getZ();
				writer.write(coordinateString + "\n");
			}

			writer.close();
		}
		catch (IOException ignored) { }
	}

	public static void readProtectedList(ServerLevel serverLevel) {
		String protectedPath = WorldFunctions.getWorldPath(serverLevel) + File.separator + "data" + File.separator + Reference.MOD_ID + File.separator + "protection" + File.separator + DimensionFunctions.getSimpleDimensionString(serverLevel);
		File protectedPathDir = new File(protectedPath);

		if (!protectedPathDir.isDirectory()) {
			return;
		}

		String protectedFilePath = protectedPath + File.separator + "blocks.txt";
		if (!(new File(protectedFilePath)).isFile()) {
			return;
		}

		try {
			String rawProtectedList = new String(Files.readAllBytes(Paths.get(protectedFilePath)));

			List<BlockPos> newProtectedList = new ArrayList<>();
			for (String coordinateString : rawProtectedList.split("\n")) {
				String[] csspl = coordinateString.split(",");
				if (csspl.length == 3) {
					try {
						newProtectedList.add(new BlockPos(Integer.parseInt(csspl[0]), Integer.parseInt(csspl[1]), Integer.parseInt(csspl[2])));
					}
					catch (NumberFormatException ignored) { }
				}
			}

			if (!newProtectedList.isEmpty()) {
				protectedMap.put(serverLevel.dimension(), newProtectedList);
			}
		} catch (IOException ignored) { }
	}

	public static void processEntityMovementOnJoin(Entity entity) {
		if (entity.getTags().contains(Reference.MOD_ID + ".protected")) {
			if (ConfigHandler.preventSpawnedEntityMovement) {
				if (entity instanceof LivingEntity) {
					((LivingEntity)entity).getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0D);
				}
			}
		}
	}

	public static BlockPos getSpawnPos(ServerLevel serverLevel, BlockPos blockPos, boolean withOffset, boolean onSurface) {
		int x = blockPos.getX();
		int y = blockPos.getY();
		int z = blockPos.getZ();

		if (ConfigHandler.shouldUseSpawnCoordinates) {
			x = ConfigHandler.spawnXCoordinate;
			y = ConfigHandler.spawnYCoordinate;
			z = ConfigHandler.spawnZCoordinate;

			if (ConfigHandler.spawnYCoordinate != 0) {
				onSurface = false;
			}
		}
		if (ConfigHandler.shouldUseSpawnCoordOffsets && withOffset) {
			x += ConfigHandler.spawnXCoordOffset;
			y += ConfigHandler.spawnYCoordOffset;
			z += ConfigHandler.spawnZCoordOffset;
		}

		if (onSurface) {
			y = BlockPosFunctions.getSurfaceBlockPos(serverLevel, x, z, ConfigHandler.ignoreTreesDuringStructurePlacement).getY();
		}

		if (y < serverLevel.getMinBuildHeight()) {
			y = serverLevel.getMinBuildHeight() + 1;
		}
		else if (y > serverLevel.getMaxBuildHeight()) {
			y = serverLevel.getMaxBuildHeight() - 1;
		}

		return new BlockPos(x, y, z);
	}
}
