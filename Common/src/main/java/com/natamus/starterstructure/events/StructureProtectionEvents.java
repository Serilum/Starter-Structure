package com.natamus.starterstructure.events;

import com.natamus.starterstructure.config.ConfigHandler;
import com.natamus.starterstructure.util.Reference;
import com.natamus.starterstructure.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class StructureProtectionEvents {
	public static boolean onBlockBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if (level.isClientSide) {
			return true;
		}

		if (ConfigHandler.playersInCreativeModeIgnoreProtection) {
			if (player.isCreative()) {
				return true;
			}
		}

		if (Util.protectedMap.containsKey(level.dimension())) {
			return !Util.protectedMap.get(level.dimension()).contains(pos);
		}
		return true;
	}

	public static boolean onBlockPlace(Level level, BlockPos blockPos, BlockState blockState, LivingEntity livingEntity, ItemStack itemStack) {
		if (ConfigHandler.playersInCreativeModeIgnoreProtection) {
			if (livingEntity instanceof Player) {
				if (((Player) livingEntity).isCreative()) {
					return true;
				}
			}
		}

		if (Util.protectedMap.containsKey(level.dimension())) {
			return !Util.protectedMap.get(level.dimension()).contains(blockPos);
		}
		return true;
	}

	public static boolean onPistonMove(Level level, BlockPos blockPos, Direction direction, boolean isExtending) {
		BlockPos faceOffsetPos = blockPos.relative(direction);
		BlockPos nextPos = faceOffsetPos.relative(direction);

		if (Util.protectedMap.containsKey(level.dimension())) {
			return !Util.protectedMap.get(level.dimension()).contains(faceOffsetPos) && !Util.protectedMap.get(level.dimension()).contains(nextPos);
		}
		return true;
	}

	public static void onTNTExplode(Level level, Entity sourceEntity, Explosion explosion) {
		if (level.isClientSide) {
			return;
		}

		if (explosion == null) {
			return;
		}

		boolean cancel = false;
		if (Util.protectedMap.containsKey(level.dimension())) {
			for (BlockPos affectedPos : explosion.getToBlow()) {
				if (Util.protectedMap.get(level.dimension()).contains(affectedPos)) {
					cancel = true;
					break;
				}
			}
		}

		if (cancel) {
			explosion.getToBlow().clear();
			explosion.getHitPlayers().clear();
		}
	}

	public static boolean onEntityAttack(Player player, Level level, Entity targetEntity) {
		if (ConfigHandler.playersInCreativeModeIgnoreEntityProtection) {
			if (player.isCreative()) {
				return true;
			}
		}

		if (ConfigHandler.protectSpawnedEntities) {
			if (targetEntity.getTags().contains(Reference.MOD_ID + ".protected")) {
				return false;
			}
		}

		if (targetEntity instanceof Painting || targetEntity instanceof ItemFrame) {
			if (Util.protectedMap.containsKey(level.dimension())) {
				return !Util.protectedMap.get(level.dimension()).contains(targetEntity.blockPosition());
			}
		}
		return true;
	}
}
