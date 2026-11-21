package com.hazeg7.sourcecraft.timer;

import net.minecraft.core.BlockPos;

/**
 * Axis-aligned cuboid trigger zone for KZ / Bhop / Surf courses.
 */
public record TimerZone(
	int id,
	String dimensionId,
	ZoneType type,
	int stageIndex,
	double minX,
	double minY,
	double minZ,
	double maxX,
	double maxY,
	double maxZ
) {
	public enum ZoneType {
		START,
		END,
		STAGE
	}

	public static TimerZone fromCorners(int id, String dimensionId, ZoneType type, int stageIndex, BlockPos a, BlockPos b) {
		double x0 = Math.min(a.getX(), b.getX());
		double y0 = Math.min(a.getY(), b.getY());
		double z0 = Math.min(a.getZ(), b.getZ());
		double x1 = Math.max(a.getX(), b.getX()) + 1.0;
		double y1 = Math.max(a.getY(), b.getY()) + 2.0;
		double z1 = Math.max(a.getZ(), b.getZ()) + 1.0;
		return new TimerZone(id, dimensionId, type, stageIndex, x0, y0, z0, x1, y1, z1);
	}

	public boolean contains(String dim, double px, double py, double pz) {
		return dimensionId.equals(dim)
			&& px >= minX && px <= maxX
			&& py >= minY - 0.1 && py <= maxY
			&& pz >= minZ && pz <= maxZ;
	}

	public double centerX() {
		return (minX + maxX) * 0.5;
	}

	public double spawnY() {
		return minY;
	}

	public double centerZ() {
		return (minZ + maxZ) * 0.5;
	}
}
