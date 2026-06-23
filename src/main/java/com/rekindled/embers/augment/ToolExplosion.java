package com.rekindled.embers.augment;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ToolExplosion extends Explosion {

	private final Level level;
	private final ItemStack tool;

	public ToolExplosion(ItemStack tool, Level level, @Nullable Entity source, double x, double y, double z, float radius, boolean fire, BlockInteraction interaction, List<BlockPos> positions) {
		super(level, source, x, y, z, radius, fire, interaction, positions);
		this.level = level;
		this.tool = tool;
	}

	@Override
	public void explode() {
		// The affected blocks are selected by the augment, so no radial block search is needed.
	}

	@Override
	public void finalizeExplosion(boolean spawnParticles) {
		if (!level.isClientSide) {
			for (BlockPos pos : List.copyOf(getToBlow())) {
				BlockState state = level.getBlockState(pos);
				if (state.isAir() || state.getDestroySpeed(level, pos) < 0)
					continue;
				BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
				Block.dropResources(state, level, pos, blockEntity, getDirectSourceEntity(), tool);
				level.removeBlock(pos, false);
			}
			clearToBlow();
		}
		super.finalizeExplosion(spawnParticles);
	}
}
