package com.natamus.starterstructure.events;

import com.natamus.collective.functions.FeatureFunctions;
import com.natamus.collective.functions.TaskFunctions;
import com.natamus.starterstructure.config.ConfigHandler;
import com.natamus.starterstructure.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.ServerLevelData;

public class StructureCreationEvents {
	public static InteractionResult onLevelSpawn(ServerLevel serverLevel, ServerLevelData serverLevelData) {
		WorldGenSettings worldOptions = null;
		try {
			worldOptions = serverLevel.getServer().getWorldData().worldGenSettings();
		}
		catch (NoSuchMethodError ignored) {}

		InteractionResult result = InteractionResult.PASS;
		if (ConfigHandler.shouldUseSpawnCoordinates || ConfigHandler.shouldUseSpawnCoordOffsets) {
			BlockPos curSpawnPos = serverLevel.getSharedSpawnPos();
			BlockPos spawnPos = Util.getSpawnPos(serverLevel, curSpawnPos, true);

			serverLevel.setDefaultSpawnPos(spawnPos, 1.0F);

			if (worldOptions != null) {
				if (worldOptions.generateBonusChest()) {
					FeatureFunctions.placeBonusChest(serverLevel, spawnPos);
				}
			}

			result = InteractionResult.SUCCESS;
		}

		TaskFunctions.enqueueCollectiveTask(serverLevel.getServer(), () -> {
			if (ConfigHandler.shouldGenerateStructure) {
				Util.generateSchematic(serverLevel);
			}
		}, 1);

		return result;
	}

	public static void onLevelLoad(ServerLevel serverLevel) {
		Util.readProtectedList(serverLevel);
	}
}
