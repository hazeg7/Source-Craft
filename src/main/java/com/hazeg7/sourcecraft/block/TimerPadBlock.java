package com.hazeg7.sourcecraft.block;

import com.hazeg7.sourcecraft.timer.TimerManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Walk-over KZ/Surf/Bhop timer pad for start zones, finish zones, and course checkpoints.
 */
public class TimerPadBlock extends Block {
	public enum PadKind {
		START,
		END,
		CHECKPOINT
	}

	private static final VoxelShape PAD_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);
	private final PadKind kind;

	public TimerPadBlock(PadKind kind, BlockBehaviour.Properties properties) {
		super(properties);
		this.kind = kind;
	}

	public PadKind kind() {
		return kind;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return PAD_SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return PAD_SHAPE;
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean flag) {
		handlePadContact(level, pos, entity);
	}

	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
		handlePadContact(level, pos, entity);
	}

	private void handlePadContact(Level level, BlockPos pos, Entity entity) {
		if (!level.isClientSide() && entity instanceof ServerPlayer player) {
			TimerManager.get().onPlayerTouchPad(player, pos, kind);
		}
	}
}
