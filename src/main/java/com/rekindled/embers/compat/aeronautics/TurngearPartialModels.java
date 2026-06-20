package com.rekindled.embers.compat.aeronautics;

import com.rekindled.embers.Embers;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

final class TurngearPartialModels {
	static final PartialModel COG = PartialModel.of(
			ResourceLocation.fromNamespaceAndPath(Embers.MODID, "block/caminite_turngear_cog"));

	private TurngearPartialModels() {
	}

	static void init() {
	}
}
