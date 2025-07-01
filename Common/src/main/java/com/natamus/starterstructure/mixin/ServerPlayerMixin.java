package com.natamus.starterstructure.mixin;

import com.natamus.starterstructure.events.StructureProtectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerPlayer.class, priority = 1001)
public class ServerPlayerMixin {
	@Inject(method = "attack", at = @At("HEAD"), cancellable = true)
	public void onPlayerInteractEntity(Entity targetEntity, CallbackInfo ci) {
		ServerPlayer serverPlayer = (ServerPlayer)(Object)this;

		if (!StructureProtectionEvents.onEntityAttack(serverPlayer, serverPlayer.level(), targetEntity)) {
			ci.cancel();
		}
	}
}
