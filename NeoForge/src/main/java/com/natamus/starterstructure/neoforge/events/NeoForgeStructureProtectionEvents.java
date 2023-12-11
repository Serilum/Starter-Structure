package com.natamus.starterstructure.neoforge.events;

import com.natamus.collective.functions.WorldFunctions;
import com.natamus.starterstructure.events.StructureProtectionEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingAttackEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class NeoForgeStructureProtectionEvents {
	@SubscribeEvent
	public static void onBlockBreak(BlockEvent.BreakEvent e) {
		Level level = WorldFunctions.getWorldIfInstanceOfAndNotRemote(e.getLevel());
		if (level == null) {
			return;
		}

		if (!StructureProtectionEvents.onBlockBreak(level, e.getPlayer(), e.getPos(), e.getState(), null)) {
			e.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onBlockPlace(BlockEvent.EntityPlaceEvent e) {
		Level level = WorldFunctions.getWorldIfInstanceOfAndNotRemote(e.getLevel());
		if (level == null) {
			return;
		}

		Entity entity = e.getEntity();
		if (!(entity instanceof LivingEntity)) {
			return;
		}

		if (!StructureProtectionEvents.onBlockPlace(level, e.getPos(), e.getPlacedBlock(), (LivingEntity)entity, null)) {
			e.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onPistonMove(PistonEvent.Pre e) {
		Level level = WorldFunctions.getWorldIfInstanceOfAndNotRemote(e.getLevel());
		if (level == null) {
			return;
		}

		if (!StructureProtectionEvents.onPistonMove(level, e.getPos(), e.getDirection(), e.getPistonMoveType().isExtend)) {
			e.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onTNTExplode(ExplosionEvent.Detonate e) {
		StructureProtectionEvents.onTNTExplode(e.getLevel(), null, e.getExplosion());
	}

	@SubscribeEvent
	public static void onLivingAttack(LivingAttackEvent e) {
		LivingEntity livingEntity = e.getEntity();
		if (!StructureProtectionEvents.onLivingAttack(livingEntity.level(), livingEntity, e.getSource(), e.getAmount())) {
			e.setCanceled(true);
		}
	}
}
