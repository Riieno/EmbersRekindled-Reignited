package com.rekindled.embers.compat.aeronautics;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;

import dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CaminiteTurngearBlock extends KineticBlock implements IBE<CaminiteTurngearBlockEntity>, IWrenchable,
		ICogWheel, BlockSubLevelCollisionShape {
	private static final VoxelShape Z_AXIS_SHAPE = Shapes.or(
			Block.box(1, 0, 1, 15, 4, 15),
			Block.box(1, 4, 1, 4, 13, 15),
			Block.box(12, 4, 1, 15, 13, 15),
			Block.box(4, 12, 1, 12, 14, 15),
			Block.box(5, 14, 5, 11, 16, 11));
	private static final VoxelShape X_AXIS_SHAPE = Shapes.or(
			Block.box(1, 0, 1, 15, 4, 15),
			Block.box(1, 4, 1, 15, 13, 4),
			Block.box(1, 4, 12, 15, 13, 15),
			Block.box(1, 12, 4, 15, 14, 12),
			Block.box(5, 14, 5, 11, 16, 11));

	public CaminiteTurngearBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_AXIS, Direction.Axis.Z));
	}

	@Override
	public Direction.Axis getRotationAxis(BlockState state) {
		return Direction.Axis.Y;
	}

	@Override
	public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
		return face.getAxis() == Direction.Axis.Y;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(BlockStateProperties.HORIZONTAL_AXIS);
	}

	@Override
	public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
		return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_AXIS,
				context.getHorizontalDirection().getAxis());
	}

	@Override
	public IRotate.SpeedLevel getMinimumRequiredSpeedLevel() {
		return IRotate.SpeedLevel.SLOW;
	}

	@Override
	public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(BlockStateProperties.HORIZONTAL_AXIS) == Direction.Axis.X ? X_AXIS_SHAPE : Z_AXIS_SHAPE;
	}

	@Override
	public VoxelShape getSubLevelCollisionShape(net.minecraft.world.level.BlockGetter level, BlockState state) {
		return Shapes.empty();
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hitResult) {
		CaminiteTurngearBlockEntity blockEntity = getBlockEntity(level, pos);
		if (blockEntity == null || player.isShiftKeyDown() || !blockEntity.isManualMode()) {
			return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
		}
		if (!level.isClientSide) {
			blockEntity.tryMount(player);
		}
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public Class<CaminiteTurngearBlockEntity> getBlockEntityClass() {
		return CaminiteTurngearBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends CaminiteTurngearBlockEntity> getBlockEntityType() {
		return AeronauticsCompat.CAMINITE_TURNGEAR_ENTITY.get();
	}
}
