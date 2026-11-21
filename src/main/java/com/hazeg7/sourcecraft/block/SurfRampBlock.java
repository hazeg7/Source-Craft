package com.hazeg7.sourcecraft.block;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Custom CS:S surf ramp block available in 30°, 45°, and 60° slope angles.
 * Uses an 8-slice VoxelShape for snag-free collision while providing exact analytical
 * surface normals for Source Engine ClipVelocity surfing.
 */
public class SurfRampBlock extends HorizontalDirectionalBlock {
	private final int slopeDegrees;
	private final Map<Direction, VoxelShape> directionalShapes = new EnumMap<>(Direction.class);

	public SurfRampBlock(int slopeDegrees, BlockBehaviour.Properties properties) {
		super(properties);
		this.slopeDegrees = slopeDegrees;
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
		buildDirectionalShapes();
	}

	public int slopeDegrees() {
		return slopeDegrees;
	}

	/**
	 * Outward unit surface normal used by {@code SourceMovementEngine.clipVelocity}.
	 */
	public Vec3 surfNormal(BlockState state) {
		Direction dir = state.getValue(FACING);
		double rad = Math.toRadians(slopeDegrees);
		double horiz = Math.sin(rad);
		double up = Math.cos(rad);
		return new Vec3(dir.getStepX() * horiz, up, dir.getStepZ() * horiz).normalize();
	}

	private void buildDirectionalShapes() {
		int maxH = switch (slopeDegrees) {
			case 30 -> 10;
			case 60 -> 16;
			default -> 16;
		};
		int runPx = switch (slopeDegrees) {
			case 30 -> 16;
			case 60 -> 10;
			default -> 16;
		};
		int slices = 8;

		for (Direction dir : Direction.Plane.HORIZONTAL) {
			VoxelShape combined = Shapes.empty();
			for (int i = 0; i < slices; i++) {
				// High back edge is opposite FACING; low edge drops toward FACING
				double frac0 = (double) i / slices;
				double frac1 = (double) (i + 1) / slices;
				double p0 = frac0 * runPx;
				double p1 = frac1 * runPx;
				double h = Math.max(1.0, maxH * (1.0 - frac0 * 0.88));

				VoxelShape slice = switch (dir) {
					case SOUTH -> Block.box(0.0, 0.0, p0, 16.0, h, p1);
					case NORTH -> Block.box(0.0, 0.0, 16.0 - p1, 16.0, h, 16.0 - p0);
					case EAST -> Block.box(p0, 0.0, 0.0, p1, h, 16.0);
					case WEST -> Block.box(16.0 - p1, 0.0, 0.0, 16.0 - p0, h, 16.0);
					default -> Block.box(0.0, 0.0, 0.0, 16.0, h, 16.0);
				};
				combined = Shapes.or(combined, slice);
			}
			directionalShapes.put(dir, combined);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return directionalShapes.getOrDefault(state.getValue(FACING), Shapes.block());
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return directionalShapes.getOrDefault(state.getValue(FACING), Shapes.block());
	}

	@Override
	protected boolean useShapeForLightOcclusion(BlockState state) {
		return true;
	}
}
