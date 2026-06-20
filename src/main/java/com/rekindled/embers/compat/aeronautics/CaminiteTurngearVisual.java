package com.rekindled.embers.compat.aeronautics;

import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import net.minecraft.core.Direction;

public class CaminiteTurngearVisual extends SingleAxisRotatingVisual<CaminiteTurngearBlockEntity> {
	public CaminiteTurngearVisual(VisualizationContext context, CaminiteTurngearBlockEntity blockEntity, float partialTick) {
		super(context, blockEntity, partialTick, Models.partial(TurngearPartialModels.COG));
		rotatingModel.setup(blockEntity, Direction.Axis.Y).setChanged();
	}

	@Override
	protected Direction.Axis rotationAxis() {
		return Direction.Axis.Y;
	}

	@Override
	public void update(float partialTick) {
		rotatingModel.setup(blockEntity, Direction.Axis.Y).setChanged();
	}
}
