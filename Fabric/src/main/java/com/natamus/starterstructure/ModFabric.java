package com.natamus.starterstructure;

import com.natamus.collective.check.RegisterMod;
import com.natamus.collective.check.ShouldLoadCheck;
import com.natamus.collective.fabric.callbacks.*;
import com.natamus.starterstructure.events.StructureCreationEvents;
import com.natamus.starterstructure.events.StructureProtectionEvents;
import com.natamus.starterstructure.events.StructureSpawnPointEvents;
import com.natamus.starterstructure.util.Reference;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ServerLevelData;

public class ModFabric implements ModInitializer {
	
	@Override
	public void onInitialize() {
		if (!ShouldLoadCheck.shouldLoad(Reference.MOD_ID)) {
			return;
		}

		setGlobalConstants();
		ModCommon.init();

		loadEvents();

		RegisterMod.register(Reference.NAME, Reference.MOD_ID, Reference.VERSION, Reference.ACCEPTED_VERSIONS);
	}

	private void loadEvents() {
		// StructureCreationEvents
		CollectiveMinecraftServerEvents.WORLD_SET_SPAWN.register((ServerLevel serverLevel, ServerLevelData serverLevelData) -> {
			StructureCreationEvents.onLevelSpawn(serverLevel, serverLevelData);
		});

		ServerWorldEvents.LOAD.register((MinecraftServer server, ServerLevel serverLevel) -> {
			StructureCreationEvents.onLevelLoad(serverLevel);
		});

		// StructureProtectionEvents
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, entity) -> {
			return StructureProtectionEvents.onBlockBreak(level, player, pos, state, entity);
		});

		CollectiveBlockEvents.BLOCK_PLACE.register((Level level, BlockPos blockPos, BlockState blockState, LivingEntity livingEntity, ItemStack itemStack) -> {
			return StructureProtectionEvents.onBlockPlace(level, blockPos, blockState, livingEntity, itemStack);
		});

		CollectivePistonEvents.PRE_PISTON_ACTIVATE.register((Level level, BlockPos blockPos, Direction direction, boolean isExtending) -> {
			return StructureProtectionEvents.onPistonMove(level, blockPos, direction, isExtending);
		});

		CollectiveExplosionEvents.EXPLOSION_DETONATE.register((Level level, Entity sourceEntity, Explosion explosion) -> {
			StructureProtectionEvents.onTNTExplode(level, sourceEntity, explosion);
		});

		CollectiveEntityEvents.ON_LIVING_ATTACK.register((Level level, Entity entity, DamageSource damageSource, float damageAmount) -> {
			return StructureProtectionEvents.onLivingAttack(level, entity, damageSource, damageAmount);
		});

		// StructureSpawnPointEvents
		ServerPlayerEvents.AFTER_RESPAWN.register((ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) -> {
			StructureSpawnPointEvents.onPlayerRespawn(oldPlayer, newPlayer, alive);
		});
		ServerEntityEvents.ENTITY_LOAD.register((Entity entity, ServerLevel serverLevel) -> {
			StructureSpawnPointEvents.onEntityJoin(serverLevel, entity);
		});
	}

	private static void setGlobalConstants() {

	}
}
