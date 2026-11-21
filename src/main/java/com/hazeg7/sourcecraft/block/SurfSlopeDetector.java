package com.hazeg7.sourcecraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Probes blocks surrounding the player for custom surf ramps, vanilla surfable slopes
 * (stairs, snow layers, slabs), ladder outward normals, and edgebug overhang opportunities.
 */
public final class SurfSlopeDetector {
	public record SlopeProbe(boolean touchingSurf, Vec3 surfNormal, boolean isCustomRamp) {
		public static final SlopeProbe NONE = new SlopeProbe(false, Vec3.ZERO, false);
	}

	private SurfSlopeDetector() {
	}

	public static SlopeProbe detectSurfSlope(Player player, boolean surfEnabled, double horizSpeedHu) {
		if (!surfEnabled) {
			return SlopeProbe.NONE;
		}
		Level level = player.level();
		AABB box = player.getBoundingBox().inflate(0.18, 0.22, 0.18);
		int minX = (int) Math.floor(box.minX);
		int minY = (int) Math.floor(box.minY - 0.15);
		int minZ = (int) Math.floor(box.minZ);
		int maxX = (int) Math.floor(box.maxX);
		int maxY = (int) Math.floor(box.maxY - 0.1);
		int maxZ = (int) Math.floor(box.maxZ);

		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		Vec3 bestNormal = null;
		boolean foundCustom = false;

		for (int y = minY; y <= maxY; y++) {
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					cursor.set(x, y, z);
					BlockState bs = level.getBlockState(cursor);
					if (bs.getBlock() instanceof SurfRampBlock ramp) {
						bestNormal = ramp.surfNormal(bs);
						foundCustom = true;
						break;
					}
					if (!player.onGround() || horizSpeedHu > 265.0) {
						Vec3 vanillaNormal = vanillaSlopeNormal(level, cursor, bs);
						if (vanillaNormal != null && bestNormal == null) {
							bestNormal = vanillaNormal;
						}
					}
				}
				if (foundCustom) {
					break;
				}
			}
			if (foundCustom) {
				break;
			}
		}

		if (bestNormal == null) {
			return SlopeProbe.NONE;
		}
		return new SlopeProbe(true, bestNormal, foundCustom);
	}

	private static Vec3 vanillaSlopeNormal(Level level, BlockPos pos, BlockState bs) {
		if (bs.getBlock() instanceof StairBlock) {
			if (bs.hasProperty(StairBlock.HALF) && bs.getValue(StairBlock.HALF) == Half.TOP) {
				return null;
			}
			Direction dir = bs.getValue(StairBlock.FACING).getOpposite();
			double s = 0.70710678;
			return new Vec3(dir.getStepX() * s, s, dir.getStepZ() * s).normalize();
		}
		if (bs.getBlock() instanceof SnowLayerBlock || bs.getBlock() instanceof SlabBlock) {
			double hCenter = surfaceHeight(bs);
			double hEast = surfaceHeight(level.getBlockState(pos.east()));
			double hWest = surfaceHeight(level.getBlockState(pos.west()));
			double hNorth = surfaceHeight(level.getBlockState(pos.north()));
			double hSouth = surfaceHeight(level.getBlockState(pos.south()));
			double gradX = ((hWest - hCenter) + (hCenter - hEast)) * 0.5;
			double gradZ = ((hNorth - hCenter) + (hCenter - hSouth)) * 0.5;
			if (Math.hypot(gradX, gradZ) < 0.12) {
				return null; // Flat slab / snow floor — do not treat as surf slope
			}
			return new Vec3(gradX, 1.0, gradZ).normalize();
		}
		return null;
	}

	private static double surfaceHeight(BlockState bs) {
		if (bs.getBlock() instanceof SnowLayerBlock) {
			return bs.getValue(SnowLayerBlock.LAYERS) / 8.0;
		}
		if (bs.getBlock() instanceof SlabBlock && bs.hasProperty(SlabBlock.TYPE)) {
			return switch (bs.getValue(SlabBlock.TYPE)) {
				case BOTTOM -> 0.5;
				case TOP, DOUBLE -> 1.0;
			};
		}
		return bs.isAir() ? 0.0 : 1.0;
	}

	public static Vec3 detectLadderNormal(Player player) {
		Level level = player.level();
		BlockPos base = player.blockPosition();
		for (int dy = 0; dy <= 1; dy++) {
			BlockState bs = level.getBlockState(base.above(dy));
			if (bs.getBlock() instanceof LadderBlock && bs.hasProperty(LadderBlock.FACING)) {
				Direction out = bs.getValue(LadderBlock.FACING);
				return new Vec3(out.getStepX(), 0.0, out.getStepZ()).normalize();
			}
		}
		Vec3 look = player.getLookAngle();
		double len = Math.hypot(look.x, look.z);
		if (len > 1e-4) {
			return new Vec3(-look.x / len, 0.0, -look.z / len);
		}
		return new Vec3(0.0, 0.0, 1.0);
	}

	/**
	 * Detects CS:S edgebug geometry: falling fast with the outer margin of the player's AABB
	 * grazing a solid block edge while center of feet remains unobstructed directly underneath.
	 */
	public static boolean isEdgebugCandidate(Player player, double velYBlocksPerTick) {
		if (player.onGround() || velYBlocksPerTick >= -0.42) {
			return false;
		}
		Level level = player.level();
		AABB feet = player.getBoundingBox();
		double probeY = feet.minY - 0.14;
		int yBlock = (int) Math.floor(probeY);
		int centerX = (int) Math.floor(player.getX());
		int centerZ = (int) Math.floor(player.getZ());

		BlockPos centerBelow = new BlockPos(centerX, yBlock, centerZ);
		if (!level.getBlockState(centerBelow).getCollisionShape(level, centerBelow).isEmpty()) {
			return false;
		}

		// Edge overhang: one corner of an expanded AABB hits a solid block edge
		AABB edgeBox = new AABB(feet.minX - 0.08, probeY - 0.08, feet.minZ - 0.08, feet.maxX + 0.08, feet.minY + 0.04, feet.maxZ + 0.08);
		int minX = (int) Math.floor(edgeBox.minX);
		int maxX = (int) Math.floor(edgeBox.maxX);
		int minZ = (int) Math.floor(edgeBox.minZ);
		int maxZ = (int) Math.floor(edgeBox.maxZ);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				if (x == centerX && z == centerZ) {
					continue;
				}
				pos.set(x, yBlock, z);
				if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Detects jumpbug opportunity: falling closely above solid ground while uncrouching.
	 */
	public static boolean isJumpbugCandidate(Player player, double velYBlocksPerTick) {
		if (player.onGround() || velYBlocksPerTick >= -0.25) {
			return false;
		}
		Level level = player.level();
		AABB feet = player.getBoundingBox();
		BlockPos below = BlockPos.containing(player.getX(), feet.minY - 0.12, player.getZ());
		return !level.getBlockState(below).getCollisionShape(level, below).isEmpty();
	}
}
