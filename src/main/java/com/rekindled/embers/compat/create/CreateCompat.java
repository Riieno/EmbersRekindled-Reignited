package com.rekindled.embers.compat.create;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import com.rekindled.embers.Embers;
import com.rekindled.embers.ConfigManager;
import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.api.EmbersAPI;
import com.rekindled.embers.block.ChamberBlockBase;
import com.rekindled.embers.block.ChamberBlockBase.ChamberConnection;
import com.rekindled.embers.block.MechEdgeBlockBase;
import com.rekindled.embers.compat.createthrusters.ThrustersCompat;
import com.rekindled.embers.item.AshenArmorItem;
import com.rekindled.embers.item.MixedGogglesItem;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.rekindled.embers.api.capabilities.EmbersCapabilities;

public final class CreateCompat {
	private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Embers.MODID);
	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Embers.MODID);
	private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
			DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Embers.MODID);
	private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
			DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Embers.MODID);

	public static final DeferredBlock<EmberKineticGeneratorBlock> EMBER_KINETIC_GENERATOR = BLOCKS.register(
			"ember_kinetic_generator",
			() -> new EmberKineticGeneratorBlock(BlockBehaviour.Properties.of()
					.mapColor(MapColor.TERRACOTTA_YELLOW)
					.strength(1.6f)
					.requiresCorrectToolForDrops()
					.noOcclusion()));
	public static final DeferredItem<BlockItem> EMBER_KINETIC_GENERATOR_ITEM = ITEMS.registerSimpleBlockItem(EMBER_KINETIC_GENERATOR);
	public static final DeferredItem<MixedGogglesItem> ENGINEERS_ASHEN_GOGGLES = ITEMS.register("engineers_ashen_goggles",
			() -> new MixedGogglesItem(new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(AshenArmorItem.DURABILITY_MULTIPLIER)), ConfigManager.ASHEN_GOGGLES_SLOTS));
	public static final Map<CreatePoweredUpgradeType, DeferredBlock<CreatePoweredEmberUpgradeBlock>> CREATE_POWERED_UPGRADES = registerCreatePoweredUpgradeBlocks();
	public static final Map<CreatePoweredUpgradeType, DeferredItem<CreatePoweredUpgradeItem>> CREATE_POWERED_UPGRADE_ITEMS = registerCreatePoweredUpgradeItems();
	public static final DeferredBlock<CreatePoweredActuatorBlock> CREATE_POWERED_ACTUATOR = BLOCKS.register(
			"create_powered_actuator", 
			() -> new CreatePoweredActuatorBlock(BlockBehaviour.Properties.of()
					.mapColor(MapColor.TERRACOTTA_YELLOW)
					.strength(1.6F)
					.requiresCorrectToolForDrops()
					.noOcclusion()));
	public static final DeferredItem<BlockItem> CREATE_POWERED_ACTUATOR_ITEM = ITEMS.registerSimpleBlockItem(CREATE_POWERED_ACTUATOR);
	public static final DeferredItem<TinkerWrenchItem> TINKER_WRENCH = ITEMS.register("tinker_wrench",
			() -> new TinkerWrenchItem(new Item.Properties()));
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EMBERS_CREATE_TAB = CREATIVE_TABS.register("create",
			() -> CreativeModeTab.builder()
					.title(Component.translatable("itemGroup.embers.create"))
					.icon(() -> new ItemStack(EMBER_KINETIC_GENERATOR_ITEM.get()))
					.displayItems(CreateCompat::buildCreateTabContents)
					.build());
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EmberKineticGeneratorBlockEntity>> EMBER_KINETIC_GENERATOR_ENTITY =
			BLOCK_ENTITIES.register("ember_kinetic_generator",
					() -> BlockEntityType.Builder.of(EmberKineticGeneratorBlockEntity::new, EMBER_KINETIC_GENERATOR.get()).build(null));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreatePoweredEmberUpgradeBlockEntity>> CREATE_POWERED_UPGRADE_ENTITY =
			BLOCK_ENTITIES.register("create_powered_ember_upgrade",
					() -> BlockEntityType.Builder.of(CreatePoweredEmberUpgradeBlockEntity::new, getCreatePoweredUpgradeBlocks()).build(null));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreatePoweredActuatorBlockEntity>> CREATE_POWERED_ACTUATOR_ENTITY =
			BLOCK_ENTITIES.register("create_powered_actuator",
					() -> BlockEntityType.Builder.of(CreatePoweredActuatorBlockEntity::new, CREATE_POWERED_ACTUATOR.get()).build(null));

	private CreateCompat() {
	}

	public static void init(IEventBus modEventBus) {
		BLOCKS.register(modEventBus);
		ITEMS.register(modEventBus);
		BLOCK_ENTITIES.register(modEventBus);
		CREATIVE_TABS.register(modEventBus);
		modEventBus.addListener(CreateCompat::commonSetup);
		modEventBus.addListener(CreateCompat::registerCreateArmInteractionPointTypes);
		modEventBus.addListener(CreateCompat::registerCapabilities);
		if (FMLEnvironment.dist.isClient()) {
			CreateCompatClient.init(modEventBus);
		}
	}

	private static void registerCreateArmInteractionPointTypes(RegisterEvent event) {
		DawnstoneAnvilArmInteractionPointType.register(event);
	}

	private static void commonSetup(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			BlockMovementChecks.registerMovementNecessaryCheck((state, level, pos) ->
					isEmbersBlock(state) ? BlockMovementChecks.CheckResult.SUCCESS : BlockMovementChecks.CheckResult.PASS);
			BlockMovementChecks.registerMovementAllowedCheck((state, level, pos) ->
					isEmbersBlock(state) ? BlockMovementChecks.CheckResult.SUCCESS : BlockMovementChecks.CheckResult.PASS);
			BlockMovementChecks.registerAttachedCheck(CreateCompat::isEmbersMultiblockAttached);
			MovementBehaviour.REGISTRY.register(RegistryManager.EMBER_RECEIVER.get(), EmberReceiverMovementBehaviour.INSTANCE);
			MovementBehaviour.REGISTRY.register(RegistryManager.EMBER_FUNNEL.get(), EmberReceiverMovementBehaviour.INSTANCE);
			MovementBehaviour.REGISTRY.register(RegistryManager.EMBER_ENERGY_CONVERTER.get(), EmberReceiverMovementBehaviour.INSTANCE);
			MovementBehaviour.REGISTRY.register(RegistryManager.EMBER_RELAY.get(), EmberReceiverMovementBehaviour.INSTANCE);
			MovementBehaviour.REGISTRY.register(RegistryManager.MIRROR_RELAY.get(), EmberReceiverMovementBehaviour.INSTANCE);
			MovementBehaviour.REGISTRY.register(RegistryManager.BEAM_SPLITTER.get(), EmberReceiverMovementBehaviour.INSTANCE);
			GogglesItem.addIsWearingPredicate(player -> {
				if (player.getItemBySlot(EquipmentSlot.HEAD).is(ENGINEERS_ASHEN_GOGGLES.get())) {
					return true;
				}
				return ModList.get().isLoaded("createthrusters")
						&& ThrustersCompat.isPhysicsAshenGoggles(player.getItemBySlot(EquipmentSlot.HEAD));
			});
			EmbersAPI.registerWearableLens(Ingredient.of(ENGINEERS_ASHEN_GOGGLES.get()));
			EmbersAPI.registerEmberResonance(Ingredient.of(ENGINEERS_ASHEN_GOGGLES.get()), 2.0);
			for (Map.Entry<CreatePoweredUpgradeType, DeferredBlock<CreatePoweredEmberUpgradeBlock>> entry : CREATE_POWERED_UPGRADES.entrySet()) {
				BlockStressValues.IMPACTS.register(entry.getValue().get(), entry.getKey()::stressImpactPerRpm);
			}
			BlockStressValues.IMPACTS.register(CREATE_POWERED_ACTUATOR.get(), CREATE_POWERED_ACTUATOR.get()::stressImpactPerRpm);
		});
	}

	private static boolean isEmbersBlock(net.minecraft.world.level.block.state.BlockState state) {
		return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals(Embers.MODID);
	}

	public static @Nullable BlockEntity findMovingEmberReceiver(Level level, BlockPos originalPosition) {
		return EmberReceiverMovementBehaviour.findByOriginalPosition(level, originalPosition);
	}

	public static @Nullable BlockEntity findMovingEmberReceiver(Level level, Vec3 physicalPosition) {
		return EmberReceiverMovementBehaviour.findAtPhysicalPosition(level, physicalPosition);
	}

	public static @Nullable Vec3 getMovingEmberReceiverPosition(Level level, BlockPos originalPosition) {
		return EmberReceiverMovementBehaviour.getPhysicalPosition(level, originalPosition);
	}

	private static BlockMovementChecks.CheckResult isEmbersMultiblockAttached(BlockState state, net.minecraft.world.level.Level level,
			BlockPos pos, Direction direction) {
		BlockPos adjacentPos = pos.relative(direction);
		BlockState adjacent = level.getBlockState(adjacentPos);
		if (!isEmbersBlock(state) && !isEmbersBlock(adjacent)) {
			return BlockMovementChecks.CheckResult.PASS;
		}
		if (state.getBlock() instanceof MechEdgeBlockBase edge
				&& pos.offset(state.getValue(MechEdgeBlockBase.EDGE).centerPos).equals(adjacentPos)
				&& adjacent.is(edge.getCenterBlock())) {
			return BlockMovementChecks.CheckResult.SUCCESS;
		}
		if (adjacent.getBlock() instanceof MechEdgeBlockBase edge
				&& adjacentPos.offset(adjacent.getValue(MechEdgeBlockBase.EDGE).centerPos).equals(pos)
				&& state.is(edge.getCenterBlock())) {
			return BlockMovementChecks.CheckResult.SUCCESS;
		}
		if (direction.getAxis() == Direction.Axis.Y && state.is(adjacent.getBlock())
				&& state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
				&& adjacent.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
				&& state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != adjacent.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
			return BlockMovementChecks.CheckResult.SUCCESS;
		}
		if (direction.getAxis() == Direction.Axis.Y && state.getBlock() instanceof ChamberBlockBase
				&& state.is(adjacent.getBlock()) && state.hasProperty(ChamberBlockBase.CONNECTION)
				&& adjacent.hasProperty(ChamberBlockBase.CONNECTION)
				&& (state.getValue(ChamberBlockBase.CONNECTION) == ChamberConnection.BOTTOM)
						!= (adjacent.getValue(ChamberBlockBase.CONNECTION) == ChamberConnection.BOTTOM)) {
			return BlockMovementChecks.CheckResult.SUCCESS;
		}
		return BlockMovementChecks.CheckResult.PASS;
	}

	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(EmbersCapabilities.EMBER_BLOCK_CAPABILITY, EMBER_KINETIC_GENERATOR_ENTITY.get(),
				(blockEntity, side) -> blockEntity.getEmberCapability());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CREATE_POWERED_UPGRADE_ENTITY.get(),
				(blockEntity, side) -> {
					IItemHandler handler = blockEntity.getItemHandler(side);
					return handler;
				});
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CREATE_POWERED_UPGRADE_ENTITY.get(),
				(blockEntity, side) -> {
					IFluidHandler handler = blockEntity.getFluidHandler(side);
					return handler;
				});
		BuiltInRegistries.BLOCK_ENTITY_TYPE
				.getOptional(ResourceLocation.fromNamespaceAndPath("create", "blaze_heater"))
				.ifPresent(type -> event.registerBlockEntity(EmbersCapabilities.EMBER_BLOCK_CAPABILITY, type,
						(blockEntity, side) -> blockEntity instanceof EmberFueledBlazeBurner emberBurner
								? emberBurner.embers$getEmberFuel()
								: null));
	}

	private static void buildCreateTabContents(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
		output.accept(EMBER_KINETIC_GENERATOR_ITEM.get());
		output.accept(CREATE_POWERED_ACTUATOR_ITEM.get());
		output.accept(ENGINEERS_ASHEN_GOGGLES.get());
		output.accept(TINKER_WRENCH.get());
		if (experimentalMechanicsEnabled()) {
			for (DeferredItem<CreatePoweredUpgradeItem> item : CREATE_POWERED_UPGRADE_ITEMS.values()) {
				output.accept(item.get());
			}
		}
	}

	public static boolean experimentalMechanicsEnabled() {
		return ConfigManager.ENABLE_EXPERIMENTAL_CREATE_MECHANICS != null
				&& ConfigManager.ENABLE_EXPERIMENTAL_CREATE_MECHANICS.get()
				&& ModList.get().isLoaded("create");
	}

	private static Map<CreatePoweredUpgradeType, DeferredBlock<CreatePoweredEmberUpgradeBlock>> registerCreatePoweredUpgradeBlocks() {
		EnumMap<CreatePoweredUpgradeType, DeferredBlock<CreatePoweredEmberUpgradeBlock>> blocks = new EnumMap<>(CreatePoweredUpgradeType.class);
		for (CreatePoweredUpgradeType type : CreatePoweredUpgradeType.values()) {
			blocks.put(type, BLOCKS.register(type.poweredPath(),
					() -> new CreatePoweredEmberUpgradeBlock(type, BlockBehaviour.Properties.of()
							.mapColor(MapColor.TERRACOTTA_ORANGE)
							.strength(1.8F)
							.requiresCorrectToolForDrops()
							.noOcclusion())));
		}
		return Collections.unmodifiableMap(blocks);
	}

	private static Map<CreatePoweredUpgradeType, DeferredItem<CreatePoweredUpgradeItem>> registerCreatePoweredUpgradeItems() {
		EnumMap<CreatePoweredUpgradeType, DeferredItem<CreatePoweredUpgradeItem>> items = new EnumMap<>(CreatePoweredUpgradeType.class);
		for (Map.Entry<CreatePoweredUpgradeType, DeferredBlock<CreatePoweredEmberUpgradeBlock>> entry : CREATE_POWERED_UPGRADES.entrySet()) {
			items.put(entry.getKey(), ITEMS.register(entry.getKey().poweredPath(),
					() -> new CreatePoweredUpgradeItem(entry.getValue().get(), new Item.Properties())));
		}
		return Collections.unmodifiableMap(items);
	}

	private static Block[] getCreatePoweredUpgradeBlocks() {
		return CREATE_POWERED_UPGRADES.values().stream()
				.map(DeferredBlock::get)
				.toArray(Block[]::new);
	}
}
