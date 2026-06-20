package com.rekindled.embers.compat.aeronautics;

import com.rekindled.embers.block.EmberFunnelBlock;
import com.rekindled.embers.block.EmbersEntityBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class EmberSightstoneBlock extends EmbersEntityBlock {
	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	private static final VoxelShape SHAPE = Block.box(4, 4, 4, 12, 12, 12);

	public EmberSightstoneBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace());
		return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockPos support = pos.relative(state.getValue(FACING).getOpposite());
		return level.getBlockState(support).getBlock() instanceof EmberFunnelBlock;
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
			BlockPos pos, BlockPos neighborPos) {
		if (direction == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos)) {
			return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
		return SHAPE;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new EmberSightstoneBlockEntity(pos, state);
	}
}
