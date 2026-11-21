package com.hazeg7.sourcecraft.mixin;

import com.hazeg7.sourcecraft.SourceMovementBridge;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
	@Redirect(
		method = "aiStep",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;jumpFromGround()V")
	)
	private void sourcecraft$suppressVanillaJumpDelay(LivingEntity instance) {
		if (!SourceMovementBridge.shouldSuppressVanillaJump(instance)) {
			instance.jumpFromGround();
		}
	}
}
