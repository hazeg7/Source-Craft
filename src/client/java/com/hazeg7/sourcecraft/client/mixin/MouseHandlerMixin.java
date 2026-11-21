package com.hazeg7.sourcecraft.client.mixin;

import com.hazeg7.sourcecraft.client.SourceClientController;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void sourcecraft$handleScrollJump(long window, double horizontal, double vertical, CallbackInfo ci) {
		if (SourceClientController.get().onMouseScroll(vertical)) {
			ci.cancel();
		}
	}

	@Inject(method = "turnPlayer", at = @At("TAIL"))
	private void sourcecraft$captureSubTickYaw(double movementTime, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			SourceClientController.get().recordSubTickYaw(mc.player.getYRot());
		}
	}
}
