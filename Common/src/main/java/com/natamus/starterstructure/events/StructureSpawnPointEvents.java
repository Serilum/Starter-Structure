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
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class StructureSpawnPointEvents {
	public static void onPlayerRespawn(ServerPlayer oldPlayer, ServerPlayer serverPlayer, boolean alive) {
		if (ConfigHandler.forceExactSpawn) {
			ServerLevel serverLevel = (ServerLevel)serverPlayer.level();

			BlockPos respawnlocation = serverLevel.getSharedSpawnPos();
			Vec3 respawnvec = new Vec3(respawnlocation.getX()+0.5, respawnlocation.getY(), respawnlocation.getZ()+0.5);

			BlockPos bedpos = serverPlayer.getRespawnPosition();
			if (bedpos != null) {
				Optional<Vec3> optionalbed = Player.findRespawnPositionAndUseSpawnBlock(serverLevel, bedpos, 1.0f, false, false);
				if (optionalbed.isPresent()) {
					return;
				}
			}

			serverPlayer.teleportTo(respawnvec.x, respawnvec.y, respawnvec.z);
		}
	}

	public static void onEntityJoin(Level level, Entity entity) {
		if (level.isClientSide) {
			return;
		}

		Util.processEntityMovementOnJoin(entity);

		if (!ConfigHandler.forceExactSpawn) {
			return;
		}

		if (!(entity instanceof Player)) {
			return;
		}

		Player player = (Player)entity;
		if (!PlayerFunctions.isJoiningWorldForTheFirstTime(player, Reference.MOD_ID, false)) {
			return;
		}

		ServerLevel serverLevel = (ServerLevel)level;

		BlockPos ppos = player.blockPosition();
		BlockPos wspos = serverLevel.getSharedSpawnPos();
		if (new BlockPos(ppos.getX(), wspos.getY(), ppos.getZ()).closerThan(wspos, 50)) {
			player.teleportTo(wspos.getX()+0.5, wspos.getY(), wspos.getZ()+0.5);
		}
	}
}
