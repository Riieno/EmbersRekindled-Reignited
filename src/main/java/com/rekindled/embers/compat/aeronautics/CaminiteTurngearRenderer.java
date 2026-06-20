package com.rekindled.embers.compat.aeronautics;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class CaminiteTurngearRenderer extends KineticBlockEntityRenderer<CaminiteTurngearBlockEntity> {
	public CaminiteTurngearRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected BlockState getRenderedBlockState(CaminiteTurngearBlockEntity blockEntity) {
		return shaft(blockEntity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_AXIS));
	}

	@Override
	protected SuperByteBuffer getRotatedModel(CaminiteTurngearBlockEntity blockEntity, BlockState state) {
		return CachedBuffers.partial((PartialModel) TurngearPartialModels.COG, blockEntity.getBlockState());
	}

	@Override
	protected void renderSafe(CaminiteTurngearBlockEntity blockEntity, float partialTicks, PoseStack poseStack,
			MultiBufferSource bufferSource, int light, int overlay) {
		if (VisualizationManager.supportsVisualization(blockEntity.getLevel())) {
			return;
		}
		BlockState renderedState = getRenderedBlockState(blockEntity);
		SuperByteBuffer cog = getRotatedModel(blockEntity, renderedState);
		float angle = getAngleForBe(blockEntity, blockEntity.getBlockPos(), getRotationAxisOf(blockEntity));
		kineticRotationTransform(cog, blockEntity, net.minecraft.core.Direction.Axis.Y, angle, light)
				.renderInto(poseStack, bufferSource.getBuffer(getRenderType(blockEntity, renderedState)));
	}
}
