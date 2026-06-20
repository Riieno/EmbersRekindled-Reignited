package com.rekindled.embers.compat.aeronautics;

import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;

final class AeronauticsCompatClient {
	private AeronauticsCompatClient() {
	}

	static void init(IEventBus modEventBus) {
		TurngearPartialModels.init();
		modEventBus.addListener(AeronauticsCompatClient::clientSetup);
		modEventBus.addListener(AeronauticsCompatClient::registerRenderers);
	}

	private static void clientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> SimpleBlockEntityVisualizer.builder(AeronauticsCompat.CAMINITE_TURNGEAR_ENTITY.get())
				.factory(CaminiteTurngearVisual::new)
				.apply());
		NeoForge.EVENT_BUS.addListener(TurngearClientInput::onClientTick);
		NeoForge.EVENT_BUS.addListener(TurngearClientInput::onInteraction);
	}

	private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(AeronauticsCompat.CAMINITE_TURNGEAR_ENTITY.get(), CaminiteTurngearRenderer::new);
	}
}
