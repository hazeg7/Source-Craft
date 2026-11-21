package com.hazeg7.sourcecraft.mixin;

import com.hazeg7.sourcecraft.SourceMovementBridge;
import com.hazeg7.sourcecraft.network.SourceServerManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerMixin {
	@Inject(method = "travel", at = @At("HEAD"), cancellable = true)
	private void sourcecraft$handleCssTravel(Vec3 travelVector, CallbackInfo ci) {
		Player self = (Player) (Object) this;
		if (SourceMovementBridge.handleTravel(self, travelVector)) {
			ci.cancel();
		}
	}

	@Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
	private void sourcecraft$handleCssFallDamage(
		double fallDistance,
		float damageMultiplier,
		DamageSource source,
		CallbackInfoReturnable<Boolean> cir
	) {
		Player self = (Player) (Object) this;
		if (self instanceof ServerPlayer sp) {
			if (SourceServerManager.consumeFallDamageExemption(sp)) {
				cir.setReturnValue(false);
			}
		} else if (self.level().isClientSide() && SourceMovementBridge.shouldCancelClientFallDamage(self)) {
			cir.setReturnValue(false);
		}
	}
}
