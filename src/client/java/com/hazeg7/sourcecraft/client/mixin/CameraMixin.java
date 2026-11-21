package com.hazeg7.sourcecraft.client.mixin;

import com.hazeg7.sourcecraft.client.SourceClientController;
import net.minecraft.client.Camera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Camera.class)
public class CameraMixin {
	@Shadow
	private Entity entity;
	@Shadow
	private boolean detached;

	@ModifyVariable(method = "setRotation", at = @At("HEAD"), ordinal = 1, argsOnly = true)
	private float sourcecraft$applyLandingViewKick(float pitch) {
		if (!detached && entity instanceof LocalPlayer) {
			return Math.max(-90.0f, Math.min(90.0f, pitch + SourceClientController.get().getLandingKickPitchDeg()));
		}
		return pitch;
	}

	@ModifyArg(
		method = "setRotation",
		at = @At(
			value = "INVOKE",
			target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;"
		),
		index = 2
	)
	private float sourcecraft$applyStrafeRoll(float rollRad) {
		if (!detached && entity instanceof LocalPlayer) {
			return rollRad + SourceClientController.get().getStrafeRollRadians();
		}
		return rollRad;
	}
}
