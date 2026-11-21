package com.hazeg7.sourcecraft.client;

import com.hazeg7.sourcecraft.SourceCraftMod;
import com.hazeg7.sourcecraft.client.config.SourceClientConfig;
import com.hazeg7.sourcecraft.client.gui.SourceHudEditorScreen;
import com.hazeg7.sourcecraft.client.gui.SourceStudioScreen;
import com.hazeg7.sourcecraft.client.hud.SourceHudRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

public class SourceCraftClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		SourceClientConfig.get();
		SourceKeybinds.init();
		SourceClientController.get().init();

		ClientTickEvents.END_CLIENT_TICK.register(client -> SourceClientController.get().onEndClientTick(client));

		HudElementRegistry.attachElementAfter(
			VanillaHudElements.MISC_OVERLAYS,
			SourceCraftMod.id("hud"),
			SourceHudRenderer::renderInWorldHud
		);

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommands.literal("csshud").executes(ctx -> {
				SourceClientController.get().requestOpenScreen(() -> new SourceHudEditorScreen(null));
				return 1;
			}));
			dispatcher.register(ClientCommands.literal("cssgui").executes(ctx -> {
				SourceClientController.get().requestOpenScreen(() -> new SourceStudioScreen(null));
				return 1;
			}));
		});
	}
}
