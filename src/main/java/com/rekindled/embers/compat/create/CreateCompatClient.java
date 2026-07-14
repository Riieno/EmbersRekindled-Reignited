package com.rekindled.embers.compat.create;

import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

final class CreateCompatClient {

	private CreateCompatClient() {
	}

	static void init(IEventBus modEventBus) {
		modEventBus.addListener(CreateCompatClient::clientSetup);
		modEventBus.addListener(CreateCompatClient::registerRenderers);
	}

	private static void clientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> SimpleBlockEntityVisualizer.builder(CreateCompat.EMBER_KINETIC_GENERATOR_ENTITY.get())
				.factory(EmberKineticGeneratorVisual::new)
				.apply());
		event.enqueueWork(() -> SimpleBlockEntityVisualizer.builder(CreateCompat.CREATE_POWERED_UPGRADE_ENTITY.get())
				.factory(CreatePoweredEmberUpgradeVisual::new)
				.apply());
		event.enqueueWork(() -> SimpleBlockEntityVisualizer.builder(CreateCompat.CREATE_POWERED_ACTUATOR_ENTITY.get())
				.factory(CreatePoweredActuatorVisual::new)
				.apply());
	}

	private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(CreateCompat.EMBER_KINETIC_GENERATOR_ENTITY.get(), EmberKineticGeneratorRenderer::new);
		event.registerBlockEntityRenderer(CreateCompat.CREATE_POWERED_UPGRADE_ENTITY.get(), CreatePoweredEmberUpgradeRenderer::new);
		event.registerBlockEntityRenderer(CreateCompat.CREATE_POWERED_ACTUATOR_ENTITY.get(), CreatePoweredActuatorRenderer::new);
	}
}
