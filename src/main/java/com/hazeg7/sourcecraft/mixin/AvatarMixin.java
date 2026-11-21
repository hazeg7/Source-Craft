package com.hazeg7.sourcecraft.mixin;

import com.hazeg7.sourcecraft.SourceMovementBridge;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Avatar.class)
public class AvatarMixin {
	@Unique
	private static final EntityDimensions SOURCECRAFT_CROUCH_DIMENSIONS =
		EntityDimensions.scalable(0.6f, 1.35f).withEyeHeight(1.15f);

	@Inject(method = "getDefaultDimensions", at = @At("RETURN"), cancellable = true)
	private void sourcecraft$adjustCrouchHull(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
		if (pose == Pose.CROUCHING && (Object) this instanceof Player player) {
			if (SourceMovementBridge.isCrouchHullActive(player)) {
				cir.setReturnValue(SOURCECRAFT_CROUCH_DIMENSIONS);
			}
		}
	}
}
