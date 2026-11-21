package com.hazeg7.sourcecraft.block;

import com.hazeg7.sourcecraft.network.SourceServerManager;
import com.hazeg7.sourcecraft.network.VelocitySyncS2CPayload;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Directional surf/bhop booster pad launching players forward & upward in Hammer Units/s.
 */
public class BoostPadBlock extends HorizontalDirectionalBlock {
	private static final VoxelShape PAD_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);
	private static final Map<UUID, Long> LAST_BOOST_TICK = new ConcurrentHashMap<>();

	public BoostPadBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
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
		triggerBoost(state, level, pos, entity);
	}

	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
		triggerBoost(state, level, pos, entity);
	}

	private static void triggerBoost(BlockState state, Level level, BlockPos pos, Entity entity) {
		if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
			return;
		}
		long gameTick = level.getGameTime();
		Long last = LAST_BOOST_TICK.get(player.getUUID());
		if (last != null && gameTick - last < 12L) {
			return;
		}
		LAST_BOOST_TICK.put(player.getUUID(), gameTick);

		Direction dir = state.getValue(FACING);
		double huPerBlock = SourceServerManager.getCvars().svHuPerBlock;
		double scale = 1.0 / (huPerBlock * 20.0);
		double boostHorizHu = 950.0;
		double boostVertHu = 420.0;

		double vxBlocks = dir.getStepX() * boostHorizHu * scale;
		double vyBlocks = boostVertHu * scale;
		double vzBlocks = dir.getStepZ() * boostHorizHu * scale;

		player.setDeltaMovement(vxBlocks, vyBlocks, vzBlocks);
		player.resetFallDistance();
		SourceServerManager.recordPlayerImpulse(player.getUUID());

		if (ServerPlayNetworking.canSend(player, VelocitySyncS2CPayload.TYPE)) {
			ServerPlayNetworking.send(player, new VelocitySyncS2CPayload(vxBlocks, vyBlocks, vzBlocks, "BOOST PAD!"));
		}
		if (level instanceof ServerLevel sl) {
			sl.playSound(null, pos, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.BLOCKS, 0.7f, 1.35f);
			sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5, 10, 0.25, 0.1, 0.25, 0.08);
		}
	}
}
