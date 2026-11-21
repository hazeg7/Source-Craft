package com.hazeg7.sourcecraft;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Clean environment-safe bridge allowing common Mixins to dispatch local-player physics to the client controller.
 */
public final class SourceMovementBridge {
	public interface ClientMovementHandler {
		boolean shouldSuppressVanillaJump(LivingEntity entity);

		boolean handleTravel(Player player, Vec3 travelVector);

		boolean isCrouchHullActive(Player player);

		boolean shouldCancelClientFallDamage(Player player);
	}

	private static volatile ClientMovementHandler clientHandler;

	private SourceMovementBridge() {
	}

	public static void setClientHandler(ClientMovementHandler handler) {
		clientHandler = handler;
	}

	public static boolean shouldSuppressVanillaJump(LivingEntity entity) {
		ClientMovementHandler h = clientHandler;
		return h != null && h.shouldSuppressVanillaJump(entity);
	}

	public static boolean handleTravel(Player player, Vec3 travelVector) {
		ClientMovementHandler h = clientHandler;
		return h != null && h.handleTravel(player, travelVector);
	}

	public static boolean isCrouchHullActive(Player player) {
		ClientMovementHandler h = clientHandler;
		return h != null && h.isCrouchHullActive(player);
	}

	public static boolean shouldCancelClientFallDamage(Player player) {
		ClientMovementHandler h = clientHandler;
		return h != null && h.shouldCancelClientFallDamage(player);
	}
}
