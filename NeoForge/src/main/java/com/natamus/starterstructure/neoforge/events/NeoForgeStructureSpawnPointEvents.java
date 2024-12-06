package com.natamus.starterstructure.neoforge.events;

import com.natamus.starterstructure.events.StructureSpawnPointEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber
public class NeoForgeStructureSpawnPointEvents {
	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent e) {
		Player player = e.getEntity();
		Level world = player.level();
		if (world.isClientSide) {
			return;
		}

		StructureSpawnPointEvents.onPlayerRespawn(null, (ServerPlayer)player, true);
	}

	@SubscribeEvent
	public static void onEntityJoin(EntityJoinLevelEvent e) {
		StructureSpawnPointEvents.onEntityJoin(e.getLevel(), e.getEntity());
	}
}
