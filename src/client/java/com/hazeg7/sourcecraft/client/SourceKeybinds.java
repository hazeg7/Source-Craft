package com.hazeg7.sourcecraft.client;

import com.hazeg7.sourcecraft.SourceCraftMod;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

/**
 * Registers SourceCraft keybindings under the dedicated sourcecraft:general category.
 */
public final class SourceKeybinds {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(SourceCraftMod.id("general"));

	public static final KeyMapping OPEN_STUDIO = register("key.sourcecraft.open_studio", InputConstants.KEY_M);
	public static final KeyMapping TOGGLE_ENGINE = register("key.sourcecraft.toggle_engine", InputConstants.KEY_G);
	public static final KeyMapping TOGGLE_AUTOBHOP = register("key.sourcecraft.toggle_autobhop", InputConstants.KEY_B);
	public static final KeyMapping CYCLE_PRESET = register("key.sourcecraft.cycle_preset", InputConstants.UNKNOWN.getValue());
	public static final KeyMapping SLOW_WALK = register("key.sourcecraft.slow_walk", InputConstants.KEY_LALT);
	public static final KeyMapping SAVE_CHECKPOINT = register("key.sourcecraft.save_cp", InputConstants.KEY_C);
	public static final KeyMapping TP_CHECKPOINT = register("key.sourcecraft.tp_cp", InputConstants.KEY_R);
	public static final KeyMapping RESTART_RUN = register("key.sourcecraft.restart_run", InputConstants.UNKNOWN.getValue());
	public static final KeyMapping CYCLE_HUD = register("key.sourcecraft.cycle_hud", InputConstants.UNKNOWN.getValue());

	public record BindingEntry(String label, KeyMapping mapping) {
	}

	private static final List<BindingEntry> ALL = List.of(
		new BindingEntry("Open SourceCraft Studio", OPEN_STUDIO),
		new BindingEntry("Toggle CS:S Physics", TOGGLE_ENGINE),
		new BindingEntry("Toggle Auto-Bhop", TOGGLE_AUTOBHOP),
		new BindingEntry("Cycle Preset", CYCLE_PRESET),
		new BindingEntry("Slow Walk (130 u/s)", SLOW_WALK),
		new BindingEntry("Save Checkpoint", SAVE_CHECKPOINT),
		new BindingEntry("Teleport Checkpoint", TP_CHECKPOINT),
		new BindingEntry("Restart Run", RESTART_RUN),
		new BindingEntry("Cycle HUD Mode", CYCLE_HUD)
	);

	private SourceKeybinds() {
	}

	private static KeyMapping register(String id, int key) {
		return KeyMappingHelper.registerKeyMapping(
			new KeyMapping(id, InputConstants.Type.KEYBOARD, key, CATEGORY)
		);
	}

	public static void init() {
		// Triggers static keybind registration.
	}

	public static List<BindingEntry> allBindings() {
		return ALL;
	}
}
