package com.hazeg7.sourcecraft.mixin;

import com.hazeg7.sourcecraft.network.SourceServerManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
	@Shadow
	public ServerPlayer player;

	@Inject(method = "shouldCheckPlayerMovement", at = @At("HEAD"), cancellable = true)
	private void sourcecraft$allowHighSpeedMovement(boolean elytra, CallbackInfoReturnable<Boolean> cir) {
		if (SourceServerManager.isActiveFor(this.player)) {
			cir.setReturnValue(false);
		}
	}

	@Redirect(
		method = "handlePlayerPositionChange",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/level/ServerPlayer;isInPostImpulseGraceTime()Z"
		)
	)
	private boolean sourcecraft$skipMovedWronglyCheck(ServerPlayer instance) {
		return SourceServerManager.isActiveFor(instance) || instance.isInPostImpulseGraceTime();
	}
}
