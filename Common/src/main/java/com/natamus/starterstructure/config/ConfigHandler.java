package com.natamus.starterstructure.config;

import com.natamus.collective.config.DuskConfig;
import com.natamus.starterstructure.util.Reference;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ConfigHandler extends DuskConfig {
	public static HashMap<String, List<String>> configMetaData = new HashMap<String, List<String>>();

	@Entry public static boolean shouldGenerateStructure = true;
	@Entry public static boolean forceExactSpawn = true;
	@Entry public static boolean ignoreTreesDuringStructurePlacement = true;
	@Entry public static boolean generationIgnoreJigsawAndStructureBlocks = true;
	@Entry public static boolean protectStructureBlocks = true;
	@Entry public static boolean protectSpawnedEntities = true;
	@Entry public static boolean playersInCreativeModeIgnoreProtection = true;
	@Entry public static boolean playersInCreativeModeIgnoreEntityProtection = false;
	@Entry public static boolean preventSpawnedEntityMovement = false;
	@Entry public static boolean spawnNonSignEntitiesFromSupportedSchematics = true;

	@Entry public static boolean shouldUseStructurePosition = false;
	@Entry(min = -10000000, max = 10000000) public static int generatedStructureXPosition = 0;
	@Entry(min = -1000, max = 1000) public static int generatedStructureYPosition = 0;
	@Entry(min = -10000000, max = 10000000) public static int generatedStructureZPosition = 0;

	@Entry public static boolean shouldUseStructureOffset = false;
	@Entry(min = -1000, max = 1000) public static int generatedStructureXOffset = 0;
	@Entry(min = -1000, max = 1000) public static int generatedStructureYOffset = 0;
	@Entry(min = -1000, max = 1000) public static int generatedStructureZOffset = 0;

	@Entry public static boolean shouldUseSpawnCoordinates = false;
	@Entry(min = -10000000, max = 10000000) public static int spawnXCoordinate = 0;
	@Entry(min = -1000, max = 1000) public static int spawnYCoordinate = 0;
	@Entry(min = -10000000, max = 10000000) public static int spawnZCoordinate = 0;

	@Entry public static boolean shouldUseSpawnCoordOffsets = false;
	@Entry(min = -1000, max = 1000) public static int spawnXCoordOffset = 0;
	@Entry(min = -1000, max = 1000) public static int spawnYCoordOffset = 0;
	@Entry(min = -1000, max = 1000) public static int spawnZCoordOffset = 0;

	public static void initConfig() {
		configMetaData.put("shouldGenerateStructure", Arrays.asList(
			"Whether a schematic that's located in './config/starterstructure/schematics/...' should be generated."
		));
		configMetaData.put("forceExactSpawn", Arrays.asList(
			"Usually player spawn points are in a randomized area. With this enabled, players will always spawn at the set coordinates (at the nearest air pocket)."
		));
		configMetaData.put("ignoreTreesDuringStructurePlacement", Arrays.asList(
			"Prevents structures from being placed on top of trees. Any leaf and log blocks will be ignored during placement."
		));
		configMetaData.put("generationIgnoreJigsawAndStructureBlocks", Arrays.asList(
			"Some schematic files might contain jigsaw or structure blocks. These are by default ignored during structure generation."
		));
		configMetaData.put("protectStructureBlocks", Arrays.asList(
			"Whether the blocks from the generated structure should be protected from breaking/griefing."
		));
		configMetaData.put("protectSpawnedEntities", Arrays.asList(
			"Whether entities spawned inside the generated structure should be protected from damage."
		));
		configMetaData.put("playersInCreativeModeIgnoreProtection", Arrays.asList(
			"If enabled, players that are in creative mode will be able to break and place the structure blocks."
		));
		configMetaData.put("playersInCreativeModeIgnoreEntityProtection", Arrays.asList(
			"If enabled, players that are in creative mode will be able to damage protected entities which spawned in structures."
		));
		configMetaData.put("preventSpawnedEntityMovement", Arrays.asList(
			"If spawned entities inside the generated structure should not be allowed to move away from the block they spawned on. Disabled by default."
		));
		configMetaData.put("spawnNonSignEntitiesFromSupportedSchematics", Arrays.asList(
			"If entities from (structure block) schematic files should be spawned when found. These are entities not created with signs."
		));

		configMetaData.put("shouldUseStructurePosition", Arrays.asList(
			"If the generatedStructurePosition config options should be used."
		));
		configMetaData.put("generatedStructureXPosition", Arrays.asList(
			"The exact x position for the generated structure. Used when shouldUseStructurePosition is enabled."
		));
		configMetaData.put("generatedStructureYPosition", Arrays.asList(
			"The exact y position for the generated structure. Used when shouldUseStructurePosition is enabled."
		));
		configMetaData.put("generatedStructureZPosition", Arrays.asList(
			"The exact z position for the generated structure. Used when shouldUseStructurePosition is enabled."
		));

		configMetaData.put("shouldUseStructureOffset", Arrays.asList(
			"If the generatedStructureOffset config options should be used."
		));
		configMetaData.put("generatedStructureXOffset", Arrays.asList(
			"The x offset for the generated structure. Used when shouldUseStructureOffset is enabled."
		));
		configMetaData.put("generatedStructureYOffset", Arrays.asList(
			"The y offset for the generated structure. Can for example be set to -1 if you notice a building always spawns one block too high. Used when shouldUseStructureOffset is enabled."
		));
		configMetaData.put("generatedStructureZOffset", Arrays.asList(
			"The z offset for the generated structure. Used when shouldUseStructureOffset is enabled."
		));

		configMetaData.put("shouldUseSpawnCoordinates", Arrays.asList(
			"If the spawnCoordinate config options should be used."
		));
		configMetaData.put("spawnXCoordinate", Arrays.asList(
			"The new X coordinate of the spawn when shouldUseSpawnCoordinates is enabled."
		));
		configMetaData.put("spawnYCoordinate", Arrays.asList(
			"The new Y coordinate of the spawn when shouldUseSpawnCoordinates is enabled."
		));
		configMetaData.put("spawnZCoordinate", Arrays.asList(
			"The new Z coordinate of the spawn when shouldUseSpawnCoordinates is enabled."
		));

		configMetaData.put("shouldUseSpawnCoordOffsets", Arrays.asList(
			"If the spawnCoordOffset config options should be used."
		));
		configMetaData.put("spawnXCoordOffset", Arrays.asList(
			"The X coordinate offset of the spawn when shouldUseSpawnCoordOffsets is enabled."
		));
		configMetaData.put("spawnYCoordOffset", Arrays.asList(
			"The Y coordinate offset of the spawn when shouldUseSpawnCoordOffsets is enabled."
		));
		configMetaData.put("spawnZCoordOffset", Arrays.asList(
			"The Z coordinate offset of the spawn when shouldUseSpawnCoordOffsets is enabled."
		));

		DuskConfig.init(Reference.NAME, Reference.MOD_ID, ConfigHandler.class);
	}
}