package com.hazeg7.sourcecraft.client.mixin;

import com.hazeg7.sourcecraft.client.SourceClientController;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin {
	@Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
	private void sourcecraft$applyCssFov(boolean isFirstPerson, float fovScale, CallbackInfoReturnable<Float> cir) {
		AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
		SourceClientController controller = SourceClientController.get();
		if (controller.shouldOverrideFov(self, isFirstPerson)) {
			cir.setReturnValue(controller.computeFovModifier(cir.getReturnValue(), isFirstPerson, fovScale));
		}
	}
}
