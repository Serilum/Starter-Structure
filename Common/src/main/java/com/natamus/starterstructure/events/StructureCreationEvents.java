package com.natamus.starterstructure.events;

import com.natamus.collective.functions.FeatureFunctions;
import com.natamus.collective.functions.TaskFunctions;
import com.natamus.starterstructure.config.ConfigHandler;
import com.natamus.starterstructure.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.ServerLevelData;

public class StructureCreationEvents {
	public static InteractionResult onLevelSpawn(ServerLevel serverLevel, ServerLevelData serverLevelData) {
		TaskFunctions.enqueueCollectiveTask(serverLevel.getServer(), () -> {
			if (ConfigHandler.shouldGenerateStructure) {
				BlockPos spawnPos = null;

				BlockPos structurePos = Util.generateSchematic(serverLevel);
				if (structurePos != null) {
					spawnPos = structurePos.immutable();
					if (ConfigHandler.shouldUseSpawnCoordOffsets) {
						spawnPos = spawnPos.offset(ConfigHandler.spawnXCoordOffset, ConfigHandler.spawnYCoordOffset, ConfigHandler.spawnZCoordOffset).immutable();
					}

					serverLevel.setDefaultSpawnPos(spawnPos, 1.0F);
				}

				if (spawnPos != null) {
					try {
						WorldOptions worldOptions = serverLevel.getServer().getWorldData().worldGenOptions();

						if (worldOptions.generateBonusChest()) {
							FeatureFunctions.placeBonusChest(serverLevel, spawnPos);
						}
					}
					catch (NoSuchMethodError ignored) {}
				}
			}
		}, 1);

		if (ConfigHandler.shouldUseSpawnCoordinates || ConfigHandler.shouldUseSpawnCoordOffsets || ConfigHandler.shouldUseStructureOffset) {
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	public static void onLevelLoad(ServerLevel serverLevel) {
		Util.readProtectedList(serverLevel);
	}
}
