package com.rekindled.embers;

import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.api.power.IEmberCapability;
import com.rekindled.embers.apiimpl.EmbersAPIImpl;
import com.rekindled.embers.compat.create.CreateCompat;
import com.rekindled.embers.compat.aeronautics.AeronauticsCompat;
import com.rekindled.embers.compat.createthrusters.ThrustersCompat;
import com.rekindled.embers.compat.curios.CuriosCompat;
import com.rekindled.embers.compat.legacy.capabilities.ForgeCapabilities;
import com.rekindled.embers.command.EmbersCommands;
import com.rekindled.embers.datagen.EmbersSounds;
import com.rekindled.embers.entity.AncientGolemEntity;
import com.rekindled.embers.network.PacketHandler;
import com.rekindled.embers.recipe.AugmentIngredient;
import com.rekindled.embers.recipe.HeatIngredient;
import com.rekindled.embers.research.ResearchManager;
import com.rekindled.embers.util.CapabilityCompat;
import com.rekindled.embers.util.CompatRegistryObject;
import com.rekindled.embers.util.DynamicMetalSeedBlacklistReloadListener;
import com.rekindled.embers.util.Misc;
import com.rekindled.embers.worldgen.EmbersLateWorldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(Embers.MODID)
public class Embers {

	public static final String MODID_OLD = "embersrekindled";
	public static final String MODID = "embers";

	public static final Logger LOGGER = LogUtils.getLogger();

	public Embers(IEventBus modEventBus, ModContainer modContainer) {
		modEventBus.addListener(this::commonSetup);
		modEventBus.addListener(this::registerCaps);
		modEventBus.addListener(this::entityAttributes);
		modEventBus.addListener(this::spawnPlacements);
		modEventBus.addListener(this::registerRecipeSerializers);
		modEventBus.addListener(PacketHandler::registerPayloads);

		EmbersAPIImpl.init();
		RegistryManager.BLOCKS.register(modEventBus);
		RegistryManager.ITEMS.register(modEventBus);
		RegistryManager.FLUIDTYPES.register(modEventBus);
		RegistryManager.FLUIDS.register(modEventBus);
		RegistryManager.ENTITY_TYPES.register(modEventBus);
		RegistryManager.BLOCK_ENTITY_TYPES_NEW.register(modEventBus);
		RegistryManager.BLOCK_ENTITY_TYPES_OLD.register(modEventBus);
		RegistryManager.CREATIVE_TABS.register(modEventBus);
		RegistryManager.PARTICLE_TYPES.register(modEventBus);
		RegistryManager.SOUND_EVENTS.register(modEventBus);
		RegistryManager.RECIPE_TYPES.register(modEventBus);
		RegistryManager.RECIPE_SERIALIZERS.register(modEventBus);
		RegistryManager.LOOT_MODIFIERS.register(modEventBus);
		RegistryManager.LOOT_CONDITION_TYPES.register(modEventBus);
		RegistryManager.MENU_TYPES.register(modEventBus);
		RegistryManager.STRUCTURE_TYPES.register(modEventBus);
		RegistryManager.STRUCTURE_PROCESSOR_TYPES.register(modEventBus);
		RegistryManager.ATTACHMENT_TYPES.register(modEventBus);
		EmbersSounds.init();

		ConfigManager.register(modContainer);
		registerClientConfigScreens(modEventBus, modContainer);

		if (ModList.get().isLoaded("curios")) {
			CuriosCompat.init();
		}
		if (ModList.get().isLoaded("create")) {
			CreateCompat.init(modEventBus);
		}
		if (ModList.get().isLoaded("create") && ModList.get().isLoaded("aeronautics")) {
			AeronauticsCompat.init(modEventBus);
		}
		if (ModList.get().isLoaded("create") && ModList.get().isLoaded("createthrusters")) {
			ThrustersCompat.init(modEventBus);
		}
	}

	private static void registerClientConfigScreens(IEventBus modEventBus, ModContainer modContainer) {
		if (FMLEnvironment.dist != Dist.CLIENT) {
			return;
		}
		try {
			Class<?> registrar = Class.forName("com.rekindled.embers.client.EmbersConfigScreens");
			registrar.getMethod("register", IEventBus.class, ModContainer.class).invoke(null, modEventBus, modContainer);
		} catch (ReflectiveOperationException exception) {
			Throwable cause = exception instanceof InvocationTargetException invocation && invocation.getCause() != null ? invocation.getCause() : exception;
			LOGGER.error("Failed to register Embers config screens", cause);
		}
	}

	public void commonSetup(final FMLCommonSetupEvent event) {
		RegistryManager.init(event);
		NeoForge.EVENT_BUS.addListener(ResearchManager::onClone);
		ResearchManager.initResearches();
		NeoForge.EVENT_BUS.addListener(EmbersEvents::onJoin);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOW, EmbersEvents::onEntityDamaged);
		NeoForge.EVENT_BUS.addListener(EmbersEvents::onBlockBreak);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOW, EmbersEvents::onProjectileFired);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOW, EmbersEvents::onArrowLoose);
		NeoForge.EVENT_BUS.addListener(EmbersEvents::onAnvilUpdate);
		NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TagsUpdatedEvent.class, e -> Misc.tagItems.clear());
		NeoForge.EVENT_BUS.addListener(DynamicMetalSeedBlacklistReloadListener::onAddReloadListeners);
		NeoForge.EVENT_BUS.addListener(EmbersEvents::onLevelLoad);
		NeoForge.EVENT_BUS.addListener(EmbersEvents::onServerTick);
		NeoForge.EVENT_BUS.addListener(EmbersEvents::onExplosion);
		NeoForge.EVENT_BUS.addListener(EmbersEvents::onTagsReload);
		NeoForge.EVENT_BUS.addListener(EmbersCommands::register);
		NeoForge.EVENT_BUS.addListener(EmbersLateWorldgen::onChunkLoad);
		NeoForge.EVENT_BUS.addListener(EmbersLateWorldgen::onServerTick);
		NeoForge.EVENT_BUS.addListener(EmbersLateWorldgen::onPotentialSpawns);
	}

	public void registerCaps(RegisterCapabilitiesEvent event) {
		registerLegacyBlockEntityCapabilities(event);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, RegistryManager.EMBER_ENERGY_CONVERTER_ENTITY.get(),
				(blockEntity, side) -> blockEntity.getEnergyStorage(side));
	}

	private static void registerLegacyBlockEntityCapabilities(RegisterCapabilitiesEvent event) {
		for (CompatRegistryObject<BlockEntityType<?>> entry : RegistryManager.BLOCK_ENTITY_TYPES_NEW.getEntries()) {
			registerLegacyBlockEntityCapabilities(event, entry.get());
		}
		for (CompatRegistryObject<BlockEntityType<?>> entry : RegistryManager.BLOCK_ENTITY_TYPES_OLD.getEntries()) {
			registerLegacyBlockEntityCapabilities(event, entry.get());
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends BlockEntity> void registerLegacyBlockEntityCapabilities(RegisterCapabilitiesEvent event, BlockEntityType<?> type) {
		BlockEntityType<T> blockEntityType = (BlockEntityType<T>) type;
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, blockEntityType, (blockEntity, side) -> {
			IItemHandler handler = CapabilityCompat.getCapability(blockEntity, ForgeCapabilities.ITEM_HANDLER, side).orElse(null);
			return handler;
		});
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, blockEntityType, (blockEntity, side) -> {
			IFluidHandler handler = CapabilityCompat.getCapability(blockEntity, ForgeCapabilities.FLUID_HANDLER, side).orElse(null);
			return handler;
		});
		event.registerBlockEntity(EmbersCapabilities.EMBER_BLOCK_CAPABILITY, blockEntityType, (blockEntity, side) -> {
			IEmberCapability handler = CapabilityCompat.getCapability(blockEntity, EmbersCapabilities.EMBER_CAPABILITY, side).orElse(null);
			return handler;
		});
	}

	public void entityAttributes(EntityAttributeCreationEvent event) {
		event.put(RegistryManager.ANCIENT_GOLEM.get(), AncientGolemEntity.createAttributes().build());
	}

	public void spawnPlacements(RegisterSpawnPlacementsEvent event) {
		event.register(RegistryManager.ANCIENT_GOLEM.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.AND);
	}

	public void registerRecipeSerializers(RegisterEvent event) {
		if (event.getRegistryKey().equals(NeoForgeRegistries.Keys.INGREDIENT_TYPES)) {
			event.register(NeoForgeRegistries.Keys.INGREDIENT_TYPES, ResourceLocation.fromNamespaceAndPath(MODID, "has_heat"), () -> HeatIngredient.TYPE);
			event.register(NeoForgeRegistries.Keys.INGREDIENT_TYPES, ResourceLocation.fromNamespaceAndPath(MODID, "has_augment"), () -> AugmentIngredient.TYPE);
		}
	}
}
