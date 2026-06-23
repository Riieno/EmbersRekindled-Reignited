package com.rekindled.embers.block;

import com.mojang.serialization.MapCodec;
import com.rekindled.embers.util.ComparatorSignalUtil;

import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.context.BlockPlaceContext;

public abstract class EmbersEntityBlock extends BaseEntityBlock {

	protected EmbersEntityBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return MapCodec.unit(this);
	}

	public InteractionResult useLegacy(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		return InteractionResult.PASS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return useLegacy(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		return switch (useLegacy(state, level, pos, player, hand, hit)) {
			case SUCCESS, SUCCESS_NO_ITEM_USED -> ItemInteractionResult.SUCCESS;
			case CONSUME -> ItemInteractionResult.CONSUME;
			case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL;
			case FAIL -> ItemInteractionResult.FAIL;
			case PASS -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		};
	}

	protected static Direction getPlacementFacing(BlockPlaceContext context) {
		Direction facing = context.getClickedFace();
		if (context.getPlayer() != null && context.getPlayer().isSecondaryUseActive()) {
			facing = facing.getOpposite();
		}
		return facing;
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return ComparatorSignalUtil.getSignal(level, pos);
	}
}
