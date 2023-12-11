package com.natamus.starterstructure.forge.events;

import com.natamus.starterstructure.events.StructureSpawnPointEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class ForgeStructureSpawnPointEvents {
	@SubscribeEvent
	public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent e) {
		Player player = e.getEntity();
		Level world = player.level;
		if (world.isClientSide) {
			return;
		}

		StructureSpawnPointEvents.onPlayerRespawn(null, (ServerPlayer)player, true);
	}

	@SubscribeEvent
	public void onEntityJoin(EntityJoinLevelEvent e) {
		StructureSpawnPointEvents.onEntityJoin(e.getLevel(), e.getEntity());
	}
}
