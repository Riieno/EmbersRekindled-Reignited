package com.rekindled.embers.compat.create;

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

import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
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
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
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
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EmberKineticGeneratorBlockEntity>> EMBER_KINETIC_GENERATOR_ENTITY =
			BLOCK_ENTITIES.register("ember_kinetic_generator",
					() -> BlockEntityType.Builder.of(EmberKineticGeneratorBlockEntity::new, EMBER_KINETIC_GENERATOR.get()).build(null));

	private CreateCompat() {
	}

	public static void init(IEventBus modEventBus) {
		BLOCKS.register(modEventBus);
		ITEMS.register(modEventBus);
		BLOCK_ENTITIES.register(modEventBus);
		modEventBus.addListener(CreateCompat::commonSetup);
		modEventBus.addListener(CreateCompat::addCreativeTabItem);
		modEventBus.addListener(CreateCompat::registerCapabilities);
		if (FMLEnvironment.dist.isClient()) {
			CreateCompatClient.init(modEventBus);
		}
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
		BuiltInRegistries.BLOCK_ENTITY_TYPE
				.getOptional(ResourceLocation.fromNamespaceAndPath("create", "blaze_heater"))
				.ifPresent(type -> event.registerBlockEntity(EmbersCapabilities.EMBER_BLOCK_CAPABILITY, type,
						(blockEntity, side) -> blockEntity instanceof EmberFueledBlazeBurner emberBurner
								? emberBurner.embers$getEmberFuel()
								: null));
	}

	private static void addCreativeTabItem(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey().equals(RegistryManager.EMBERS_TAB.getKey())) {
			event.accept(EMBER_KINETIC_GENERATOR_ITEM.get());
			event.accept(ENGINEERS_ASHEN_GOGGLES.get());
		}
	}
}
