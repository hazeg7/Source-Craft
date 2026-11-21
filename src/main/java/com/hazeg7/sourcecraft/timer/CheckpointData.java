package com.hazeg7.sourcecraft.timer;

/**
 * Snapshot of a player's position, view angles, and CS:S velocity for KZ checkpoints.
 */
public record CheckpointData(
	String dimensionId,
	double x,
	double y,
	double z,
	float yaw,
	float pitch,
	double vxBlocks,
	double vyBlocks,
	double vzBlocks,
	long createdAtMs
) {
}
