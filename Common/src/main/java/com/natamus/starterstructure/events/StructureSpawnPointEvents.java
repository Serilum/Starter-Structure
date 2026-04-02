package com.natamus.starterstructure.events;

import com.natamus.collective.functions.PlayerFunctions;
import com.natamus.starterstructure.config.ConfigHandler;
import com.natamus.starterstructure.util.Reference;
import com.natamus.starterstructure.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData.RespawnData;
import net.minecraft.world.phys.Vec3;

public class StructureSpawnPointEvents {
	public static void onPlayerRespawn(ServerPlayer oldPlayer, ServerPlayer serverPlayer, boolean alive) {
		if (ConfigHandler.forceExactSpawn) {
			ServerLevel serverLevel = (ServerLevel)serverPlayer.level();

			RespawnData respawnData = serverLevel.getRespawnData();

			BlockPos respawnlocation = respawnData.pos();
			Vec3 respawnvec = new Vec3(respawnlocation.getX()+0.5, respawnlocation.getY(), respawnlocation.getZ()+0.5);

			ServerPlayer.RespawnConfig respawnConfig = serverPlayer.getRespawnConfig();
			if (respawnConfig != null) {
				BlockPos bedpos = respawnConfig.respawnData().pos();
				if (bedpos != null) {
					TeleportTransition optionalbed = serverPlayer.findRespawnPositionAndUseSpawnBlock(true, TeleportTransition.DO_NOTHING);
					if (!optionalbed.missingRespawnBlock()) {
						return;
					}
				}
			}

			serverPlayer.teleportTo(respawnvec.x, respawnvec.y, respawnvec.z);
		}
	}

	public static void onEntityJoin(Level level, Entity entity) {
		if (level.isClientSide()) {
			return;
		}

		Util.processEntityMovementOnJoin(entity);

		if (!ConfigHandler.forceExactSpawn) {
			return;
		}

		if (!(entity instanceof Player player)) {
			return;
		}

        if (!PlayerFunctions.isJoiningWorldForTheFirstTime(player, Reference.MOD_ID, false)) {
			return;
		}

		ServerLevel serverLevel = (ServerLevel)level;

		RespawnData respawnData = serverLevel.getRespawnData();

		BlockPos ppos = player.blockPosition();
		BlockPos wspos = respawnData.pos();
		if (new BlockPos(ppos.getX(), wspos.getY(), ppos.getZ()).closerThan(wspos, 50)) {
			player.teleportTo(wspos.getX()+0.5, wspos.getY(), wspos.getZ()+0.5);
		}
	}
}
