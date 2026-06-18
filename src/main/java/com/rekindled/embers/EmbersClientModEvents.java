package com.rekindled.embers;

import java.io.IOException;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.rekindled.embers.augment.CasterOrbAugmentClient;
import com.rekindled.embers.augment.ShiftingScalesAugment;
import com.rekindled.embers.augment.ShiftingScalesAugmentClient;
import com.rekindled.embers.augment.WindingGearsAugment;
import com.rekindled.embers.augment.WindingGearsAugmentClient;
import com.rekindled.embers.blockentity.render.AlchemyPedestalBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.AlchemyPedestalTopBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.AlchemyTabletBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.AtmosphericBellowsBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.AutomaticHammerBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.BinBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.CatalyticPlugBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.CinderPlinthBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.CopperChargerBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.CrystalCellBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.CrystalSeedBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.DawnstoneAnvilBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.EmberBoreBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.EntropicEnumeratorBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.ExcavationBucketsBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.FieldChartBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.FluidTransferBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.FluidVesselBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.GeologicSeparatorBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.InfernoForgeTopBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.ItemTransferBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.MechanicalPumpBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.MelterTopBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.MithrilBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.MixerCentrifugeBottomBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.MixerCentrifugeTopBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.MnemonicInscriberBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.ReservoirBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.StampBaseBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.StamperBlockEntityRenderer;
import com.rekindled.embers.compat.create.CreateCompat;
import com.rekindled.embers.compat.createthrusters.ThrustersCompat;
import com.rekindled.embers.compat.curios.CuriosCompatClient;
import com.rekindled.embers.entity.render.AncientGolemRenderer;
import com.rekindled.embers.entity.render.EmberPacketRenderer;
import com.rekindled.embers.entity.render.EmberProjectileRenderer;
import com.rekindled.embers.entity.render.GlimmerProjectileRenderer;
import com.rekindled.embers.fluidtypes.EmbersFluidType;
import com.rekindled.embers.gui.SlateScreen;
import com.rekindled.embers.item.AlchemicalNoteItemClientExtensions;
import com.rekindled.embers.item.EmberStorageItem;
import com.rekindled.embers.item.TyrfingItem;
import com.rekindled.embers.model.AncientGolemModel;
import com.rekindled.embers.model.AshenArmorModel;
import com.rekindled.embers.particle.AlchemyCircleParticle;
import com.rekindled.embers.particle.GlowParticle;
import com.rekindled.embers.particle.SmokeParticle;
import com.rekindled.embers.particle.SparkParticle;
import com.rekindled.embers.particle.StarParticle;
import com.rekindled.embers.particle.TyrfingParticle;
import com.rekindled.embers.particle.VaporParticle;
import com.rekindled.embers.particle.XRayGlowParticle;
import com.rekindled.embers.render.EmbersRenderTypes;
import com.rekindled.embers.render.PipeModel;
import com.rekindled.embers.util.DecimalFormats;
import com.rekindled.embers.util.EmbersColors;
import com.rekindled.embers.util.GlowingTextTooltip;
import com.rekindled.embers.util.GlowingTextTooltip.GlowingTextClientTooltip;
import com.rekindled.embers.util.HeatBarTooltip;
import com.rekindled.embers.util.HeatBarTooltip.HeatBarClientTooltip;
import com.rekindled.embers.util.ItemData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterNamedRenderTypesEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(modid = Embers.MODID, value = Dist.CLIENT)
public final class EmbersClientModEvents {

	private EmbersClientModEvents() {
	}

	@SubscribeEvent
	public static void clientSetup(FMLClientSetupEvent event) {
		NeoForge.EVENT_BUS.addListener(EmbersClientEvents::onLevelLoad);
		NeoForge.EVENT_BUS.addListener(EmbersClientEvents::onClientTick);
		NeoForge.EVENT_BUS.addListener(EmbersClientEvents::onMovementInput);
		NeoForge.EVENT_BUS.addListener(EmbersClientEvents::onBlockHighlight);
		NeoForge.EVENT_BUS.addListener(EmbersClientEvents::onLevelRender);
		NeoForge.EVENT_BUS.addListener(EmbersClientEvents::onTooltip);
		NeoForge.EVENT_BUS.addListener(EmbersClientEvents::onWorldRender);
		CasterOrbAugmentClient.init();
		ShiftingScalesAugmentClient.init();
		WindingGearsAugmentClient.init();
		ItemBlockRenderTypes.setRenderLayer(RegistryManager.STEAM.FLUID.get(), RenderType.translucent());
		ItemBlockRenderTypes.setRenderLayer(RegistryManager.STEAM.FLUID_FLOW.get(), RenderType.translucent());
		ItemBlockRenderTypes.setRenderLayer(RegistryManager.DWARVEN_OIL.FLUID.get(), RenderType.translucent());
		ItemBlockRenderTypes.setRenderLayer(RegistryManager.DWARVEN_OIL.FLUID_FLOW.get(), RenderType.translucent());
		ItemBlockRenderTypes.setRenderLayer(RegistryManager.DWARVEN_GAS.FLUID.get(), RenderType.translucent());
		ItemBlockRenderTypes.setRenderLayer(RegistryManager.DWARVEN_GAS.FLUID_FLOW.get(), RenderType.translucent());
		event.enqueueWork(() -> ItemProperties.register(RegistryManager.INFLICTOR_GEM.get(), ResourceLocation.fromNamespaceAndPath(Embers.MODID, "charged"), (stack, level, entity, seed) -> {
			return ItemData.getOrCreateTag(stack).contains("type") ? 1.0F : 0.0F;
		}));
	}

	@SubscribeEvent
	public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
		for (RegistryManager.FluidStuff fluid : RegistryManager.fluidList) {
			if (fluid.TYPE.get() instanceof EmbersFluidType fluidType) {
				event.registerFluidType(fluidType.createClientExtensions(), fluidType);
			}
		}
		event.registerItem(AshenArmorModel.ARMOR_MODEL_GETTER, RegistryManager.ASHEN_GOGGLES.get(), RegistryManager.ASHEN_CLOAK.get(), RegistryManager.ASHEN_LEGGINGS.get(), RegistryManager.ASHEN_BOOTS.get());
		if (ModList.get().isLoaded("create")) {
			event.registerItem(AshenArmorModel.ARMOR_MODEL_GETTER, CreateCompat.ENGINEERS_ASHEN_GOGGLES.get());
		}
		if (ModList.get().isLoaded("create") && ModList.get().isLoaded("createthrusters")) {
			event.registerItem(AshenArmorModel.ARMOR_MODEL_GETTER, ThrustersCompat.PHYSICS_ASHEN_GOGGLES.get());
		}
		event.registerItem(new AlchemicalNoteItemClientExtensions(), RegistryManager.ALCHEMICAL_NOTE.get());
	}

	@SubscribeEvent
	public static void registerScreens(RegisterMenuScreensEvent event) {
		event.register(RegistryManager.SLATE_MENU.get(), SlateScreen::new);
	}

	@SubscribeEvent
	public static void overlayRegister(RegisterGuiLayersEvent event) {
		event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "embers_ingame_overlay"), (graphics, delta) -> EmbersClientEvents.renderIngameOverlay(Minecraft.getInstance(), graphics, delta.getGameTimeDeltaPartialTick(false), graphics.guiWidth(), graphics.guiHeight()));
		event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "shifting_scales_particles"), (graphics, delta) -> ShiftingScalesAugmentClient.renderIngameOverlay(Minecraft.getInstance(), graphics, delta.getGameTimeDeltaPartialTick(false), graphics.guiWidth(), graphics.guiHeight()));
		event.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, ResourceLocation.fromNamespaceAndPath(Embers.MODID, "shifting_scales_hearts"), (graphics, delta) -> ShiftingScalesAugmentClient.renderHeartsOverlay(Minecraft.getInstance(), graphics, delta.getGameTimeDeltaPartialTick(false), graphics.guiWidth(), graphics.guiHeight()));
		event.registerBelow(VanillaGuiLayers.JUMP_METER, ResourceLocation.fromNamespaceAndPath(Embers.MODID, "winding_gears_spring_bottom"), (graphics, delta) -> WindingGearsAugmentClient.renderSpringUnderlay(Minecraft.getInstance(), graphics, delta.getGameTimeDeltaPartialTick(false), graphics.guiWidth(), graphics.guiHeight()));
		event.registerAbove(VanillaGuiLayers.EXPERIENCE_BAR, ResourceLocation.fromNamespaceAndPath(Embers.MODID, "winding_gears_spring_top"), (graphics, delta) -> WindingGearsAugmentClient.renderSpringOverlay(Minecraft.getInstance(), graphics, delta.getGameTimeDeltaPartialTick(false), graphics.guiWidth(), graphics.guiHeight()));
	}

	@SubscribeEvent
	static void addResourceListener(RegisterClientReloadListenersEvent event) {
		event.registerReloadListener(new DecimalFormats());
		event.registerReloadListener(new EmbersColors());
	}

	@SubscribeEvent
	static void addParticleProvider(RegisterParticleProvidersEvent event) {
		event.registerSprite(RegistryManager.GLOW_PARTICLE.get(), new GlowParticle.Provider());
		event.registerSprite(RegistryManager.STAR_PARTICLE.get(), new StarParticle.Provider());
		event.registerSprite(RegistryManager.SPARK_PARTICLE.get(), new SparkParticle.Provider());
		event.registerSprite(RegistryManager.SMOKE_PARTICLE.get(), new SmokeParticle.Provider());
		event.registerSprite(RegistryManager.VAPOR_PARTICLE.get(), new VaporParticle.Provider());
		event.registerSprite(RegistryManager.ALCHEMY_CIRCLE_PARTICLE.get(), new AlchemyCircleParticle.Provider());
		event.registerSprite(RegistryManager.TYRFING_PARTICLE.get(), new TyrfingParticle.Provider());
		event.registerSprite(RegistryManager.XRAY_GLOW_PARTICLE.get(), new XRayGlowParticle.Provider());
	}

	@SubscribeEvent
	static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(RegistryManager.EMBER_PACKET.get(), EmberPacketRenderer::new);
		event.registerEntityRenderer(RegistryManager.EMBER_PROJECTILE.get(), EmberProjectileRenderer::new);
		event.registerEntityRenderer(RegistryManager.GLIMMER_PROJECTILE.get(), GlimmerProjectileRenderer::new);
		event.registerEntityRenderer(RegistryManager.ANCIENT_GOLEM.get(), AncientGolemRenderer::new);

		event.registerBlockEntityRenderer(RegistryManager.EMBER_BORE_ENTITY.get(), EmberBoreBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.MELTER_TOP_ENTITY.get(), MelterTopBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.FLUID_VESSEL_ENTITY.get(), FluidVesselBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.STAMPER_ENTITY.get(), StamperBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.STAMP_BASE_ENTITY.get(), StampBaseBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.BIN_ENTITY.get(), BinBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.MIXER_CENTRIFUGE_BOTTOM_ENTITY.get(), MixerCentrifugeBottomBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.MIXER_CENTRIFUGE_TOP_ENTITY.get(), MixerCentrifugeTopBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.RESERVOIR_ENTITY.get(), ReservoirBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.CRYSTAL_CELL_ENTITY.get(), CrystalCellBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.GEOLOGIC_SEPARATOR_ENTITY.get(), GeologicSeparatorBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.COPPER_CHARGER_ENTITY.get(), CopperChargerBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.ITEM_TRANSFER_ENTITY.get(), ItemTransferBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.FLUID_TRANSFER_ENTITY.get(), FluidTransferBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.ALCHEMY_PEDESTAL_TOP_ENTITY.get(), AlchemyPedestalTopBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.ALCHEMY_PEDESTAL_ENTITY.get(), AlchemyPedestalBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.ALCHEMY_TABLET_ENTITY.get(), AlchemyTabletBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.MECHANICAL_PUMP_BOTTOM_ENTITY.get(), MechanicalPumpBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.CATALYTIC_PLUG_ENTITY.get(), CatalyticPlugBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.COPPER_CRYSTAL_SEED.BLOCKENTITY.get(), CrystalSeedBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.IRON_CRYSTAL_SEED.BLOCKENTITY.get(), CrystalSeedBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.GOLD_CRYSTAL_SEED.BLOCKENTITY.get(), CrystalSeedBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.LEAD_CRYSTAL_SEED.BLOCKENTITY.get(), CrystalSeedBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.SILVER_CRYSTAL_SEED.BLOCKENTITY.get(), CrystalSeedBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.NICKEL_CRYSTAL_SEED.BLOCKENTITY.get(), CrystalSeedBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.TIN_CRYSTAL_SEED.BLOCKENTITY.get(), CrystalSeedBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.ALUMINUM_CRYSTAL_SEED.BLOCKENTITY.get(), CrystalSeedBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.ZINC_CRYSTAL_SEED.BLOCKENTITY.get(), CrystalSeedBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.PLATINUM_CRYSTAL_SEED.BLOCKENTITY.get(), CrystalSeedBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.URANIUM_CRYSTAL_SEED.BLOCKENTITY.get(), CrystalSeedBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.DAWNSTONE_CRYSTAL_SEED.BLOCKENTITY.get(), CrystalSeedBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.FIELD_CHART_ENTITY.get(), FieldChartBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.CINDER_PLINTH_ENTITY.get(), CinderPlinthBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.DAWNSTONE_ANVIL_ENTITY.get(), DawnstoneAnvilBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.AUTOMATIC_HAMMER_ENTITY.get(), AutomaticHammerBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.INFERNO_FORGE_TOP_ENTITY.get(), InfernoForgeTopBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.MNEMONIC_INSCRIBER_ENTITY.get(), MnemonicInscriberBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.ATMOSPHERIC_BELLOWS_ENTITY.get(), AtmosphericBellowsBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.ENTROPIC_ENUMERATOR_ENTITY.get(), EntropicEnumeratorBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.MITHRIL_BLOCK_ENTITY.get(), MithrilBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(RegistryManager.EXCAVATION_BUCKETS_ENTITY.get(), ExcavationBucketsBlockEntityRenderer::new);
	}

	@SubscribeEvent
	static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(AncientGolemRenderer.LAYER_LOCATION, AncientGolemModel::createLayer);
		event.registerLayerDefinition(AshenArmorModel.ASHEN_ARMOR_HEAD, () -> LayerDefinition.create(AshenArmorModel.createHeadMesh(), 64, 64));
		event.registerLayerDefinition(AshenArmorModel.ASHEN_ARMOR_CHEST, () -> LayerDefinition.create(AshenArmorModel.createChestMesh(), 64, 64));
		event.registerLayerDefinition(AshenArmorModel.ASHEN_ARMOR_LEGS, () -> LayerDefinition.create(AshenArmorModel.createLegsMesh(), 64, 64));
		event.registerLayerDefinition(AshenArmorModel.ASHEN_ARMOR_FEET, () -> LayerDefinition.create(AshenArmorModel.createFeetMesh(), 64, 64));
	}

	@SubscribeEvent
	static void registerItemColorHandlers(RegisterColorHandlersEvent.Item event) {
		ItemColor emberContainerColor = new EmberStorageItem.ColorHandler();
		if (ModList.get().isLoaded("curios")) {
			CuriosCompatClient.registerColorHandler(event, emberContainerColor);
		}
		event.register(emberContainerColor, RegistryManager.EMBER_JAR.get(), RegistryManager.EMBER_CARTRIDGE.get());
		event.register(new TyrfingItem.ColorHandler(), RegistryManager.TYRFING.get());
	}

	@SubscribeEvent
	static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
		event.register(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "pipe"), PipeModel.Loader.INSTANCE);
	}

	@SubscribeEvent
	static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
		event.register(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "block/atmospheric_bellows_top")));
		event.register(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "block/atmospheric_bellows_leather")));
		event.register(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "block/stamper_arm")));
		event.register(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "block/inferno_forge_hatch")));
	}

	@SubscribeEvent
	static void onModelBakeCompleted(ModelEvent.BakingCompleted event) {
		EmbersClientEvents.afterModelBake(event);
	}

	@SubscribeEvent
	static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
		event.register(GlowingTextTooltip.class, GlowingTextClientTooltip::new);
		event.register(HeatBarTooltip.class, HeatBarClientTooltip::new);
	}

	@SubscribeEvent
	public static void shaderRegistry(RegisterShadersEvent event) throws IOException {
		event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Embers.MODID, "position_tex_color_additive"), DefaultVertexFormat.POSITION_TEX_COLOR), shaderInstance -> EmbersRenderTypes.additiveShader = shaderInstance);
		event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Embers.MODID, "particle_ember"), DefaultVertexFormat.PARTICLE), shaderInstance -> EmbersRenderTypes.emberParticleShader = shaderInstance);
		event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Embers.MODID, "particle_ember_fab"), DefaultVertexFormat.PARTICLE), shaderInstance -> EmbersRenderTypes.emberParticleFabShader = shaderInstance);
		event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Embers.MODID, "particle_translucent"), DefaultVertexFormat.PARTICLE), shaderInstance -> EmbersRenderTypes.translucentParticleShader = shaderInstance);
		event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Embers.MODID, "rendertype_entity_solid_mithril"), DefaultVertexFormat.NEW_ENTITY), shaderInstance -> EmbersRenderTypes.mithrilShader = shaderInstance);
	}

	@SubscribeEvent
	public static void renderTypeRegistry(RegisterNamedRenderTypesEvent event) {
		event.register(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "mithril"), RenderType.solid(), EmbersRenderTypes.MITHRIL);
	}
}
