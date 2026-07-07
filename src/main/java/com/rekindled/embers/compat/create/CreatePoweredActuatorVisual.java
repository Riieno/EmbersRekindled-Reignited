package com.rekindled.embers.compat.create;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import net.minecraft.core.Direction;

public class CreatePoweredActuatorVisual extends OrientedRotatingVisual<CreatePoweredActuatorBlockEntity> {
    
    private static final float SHAFT_OUTSET = 1.0F / 16.0F;

	public CreatePoweredActuatorVisual(VisualizationContext context, CreatePoweredActuatorBlockEntity blockEntity, float partialTick) {
		super(context, blockEntity, partialTick, Direction.SOUTH,
				blockEntity.getBlockState().getValue(CreatePoweredActuatorBlock.FACING),
				Models.partial(AllPartialModels.SHAFT_HALF));
		Direction facing = blockEntity.getBlockState().getValue(CreatePoweredActuatorBlock.FACING);
		rotatingModel.nudge(facing.getStepX() * SHAFT_OUTSET, facing.getStepY() * SHAFT_OUTSET, facing.getStepZ() * SHAFT_OUTSET);
		rotatingModel.setChanged();
	}

}
