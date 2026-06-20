package com.rekindled.embers.compat.aeronautics;

import com.rekindled.embers.Embers;
import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.simibubi.create.api.stress.BlockStressValues;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AeronauticsCompat {
	private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Embers.MODID);
	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Embers.MODID);
	private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
			DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Embers.MODID);

	public static final DeferredBlock<CaminiteTurngearBlock> CAMINITE_TURNGEAR = BLOCKS.register("caminite_turngear",
			() -> new CaminiteTurngearBlock(BlockBehaviour.Properties.of()
					.mapColor(MapColor.TERRACOTTA_ORANGE)
					.strength(2.5F)
					.requiresCorrectToolForDrops()
					.noOcclusion()));
	public static final DeferredBlock<EmberSightstoneBlock> EMBER_SIGHTSTONE = BLOCKS.register("ember_sightstone",
			() -> new EmberSightstoneBlock(BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_ORANGE)
					.strength(1.0F)
					.requiresCorrectToolForDrops()
					.noCollission()
					.noOcclusion()
					.lightLevel(state -> 7)));

	public static final DeferredItem<BlockItem> CAMINITE_TURNGEAR_ITEM = ITEMS.registerSimpleBlockItem(CAMINITE_TURNGEAR);
	public static final DeferredItem<BlockItem> EMBER_SIGHTSTONE_ITEM = ITEMS.registerSimpleBlockItem(EMBER_SIGHTSTONE);

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CaminiteTurngearBlockEntity>> CAMINITE_TURNGEAR_ENTITY =
			BLOCK_ENTITIES.register("caminite_turngear",
					() -> BlockEntityType.Builder.of(CaminiteTurngearBlockEntity::new, CAMINITE_TURNGEAR.get()).build(null));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EmberSightstoneBlockEntity>> EMBER_SIGHTSTONE_ENTITY =
			BLOCK_ENTITIES.register("ember_sightstone",
					() -> BlockEntityType.Builder.of(EmberSightstoneBlockEntity::new, EMBER_SIGHTSTONE.get()).build(null));

	private AeronauticsCompat() {
	}

	public static void init(IEventBus modEventBus) {
		BLOCKS.register(modEventBus);
		ITEMS.register(modEventBus);
		BLOCK_ENTITIES.register(modEventBus);
		modEventBus.addListener(AeronauticsCompat::commonSetup);
		modEventBus.addListener(AeronauticsCompat::addCreativeTabItems);
		modEventBus.addListener(AeronauticsCompat::registerCapabilities);
		if (FMLEnvironment.dist.isClient()) {
			AeronauticsCompatClient.init(modEventBus);
		}
	}

	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(EmbersCapabilities.EMBER_BLOCK_CAPABILITY, CAMINITE_TURNGEAR_ENTITY.get(),
				(blockEntity, side) -> blockEntity.getEmberCapability());
	}

	private static void commonSetup(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> BlockStressValues.IMPACTS.register(CAMINITE_TURNGEAR.get(), () -> 8.0D));
	}

	private static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey().equals(RegistryManager.EMBERS_TAB.getKey())) {
			event.accept(CAMINITE_TURNGEAR_ITEM.get());
			event.accept(EMBER_SIGHTSTONE_ITEM.get());
		}
	}
}
