package com.natamus.starterstructure.forge.events;

import com.natamus.collective.functions.WorldFunctions;
import com.natamus.starterstructure.events.StructureProtectionEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class ForgeStructureProtectionEvents {
	@SubscribeEvent
	public void onBlockBreak(BlockEvent.BreakEvent e) {
		Level level = WorldFunctions.getWorldIfInstanceOfAndNotRemote(e.getLevel());
		if (level == null) {
			return;
		}

		if (!StructureProtectionEvents.onBlockBreak(level, e.getPlayer(), e.getPos(), e.getState(), null)) {
			e.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onBlockPlace(BlockEvent.EntityPlaceEvent e) {
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
	public void onPistonMove(PistonEvent.Pre e) {
		Level level = WorldFunctions.getWorldIfInstanceOfAndNotRemote(e.getLevel());
		if (level == null) {
			return;
		}

		if (!StructureProtectionEvents.onPistonMove(level, e.getPos(), e.getDirection(), e.getPistonMoveType().isExtend)) {
			e.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onTNTExplode(ExplosionEvent.Detonate e) {
		StructureProtectionEvents.onTNTExplode(e.getLevel(), null, e.getExplosion());
	}

	@SubscribeEvent
	public void onLivingAttack(LivingAttackEvent e) {
		LivingEntity livingEntity = e.getEntity();
		if (!StructureProtectionEvents.onLivingAttack(livingEntity.level(), livingEntity, e.getSource(), e.getAmount())) {
			e.setCanceled(true);
		}
	}
}
