package com.hazeg7.sourcecraft.physics;

import java.util.Locale;

/**
 * Maps Minecraft held item category / ID names to CS: Source weapon mobility speed multipliers (base 250 u/s).
 */
public final class ItemWeightTable {
	private ItemWeightTable() {
	}

	public static double maxSpeedForItemKey(String itemId, double baseMaxSpeed) {
		if (itemId == null || itemId.isEmpty()) {
			return baseMaxSpeed; // Empty hand / knife = 250 u/s (1.00x)
		}
		String k = itemId.toLowerCase(Locale.ROOT);
		// Heavy rifles / bows / crossbows / tridents -> ~215-225 u/s
		if (k.contains("crossbow") || k.contains("trident") || k.contains("mace")) {
			return baseMaxSpeed * (215.0 / 250.0);
		}
		if (k.contains("bow")) {
			return baseMaxSpeed * (225.0 / 250.0);
		}
		// Heavy melee / tools / shields
		if (k.contains("shield") || k.contains("netherite_axe") || k.contains("diamond_axe")) {
			return baseMaxSpeed * (225.0 / 250.0);
		}
		if (k.contains("_axe") || k.contains("pickaxe") || k.contains("shovel") || k.contains("hoe")) {
			return baseMaxSpeed * (235.0 / 250.0);
		}
		// Swords & daggers -> fast knife/pistol tier (245-250 u/s)
		if (k.contains("sword")) {
			return baseMaxSpeed * (245.0 / 250.0);
		}
		// Map wand / light items / empty -> full 250 u/s
		return baseMaxSpeed;
	}
}
