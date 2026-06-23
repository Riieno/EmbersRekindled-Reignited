package com.rekindled.embers.util;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import com.rekindled.embers.ConfigManager;
import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.api.power.IEmberCapability;
import com.rekindled.embers.api.tile.IExtraDialInformation;
import com.rekindled.embers.block.EmberDialBlock;
import com.rekindled.embers.block.FluidDialBlock;
import com.rekindled.embers.block.ItemDialBlock;
import com.rekindled.embers.block.MechEdgeBlockBase;
import com.rekindled.embers.block.MechEdgeBlockBase.MechEdge;
import com.rekindled.embers.blockentity.CopperChargerBlockEntity;
import com.rekindled.embers.blockentity.CrystalSeedBlockEntity;
import com.rekindled.embers.blockentity.MechanicalCoreBlockEntity;
import com.rekindled.embers.blockentity.MechanicalCoreBlockEntity.BlockEntityDirection;
import com.rekindled.embers.blockentity.MixerCentrifugeBottomBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

public final class ComparatorSignalUtil {
	private static final Map<BlockEntity, Integer> LAST_SIGNALS = new WeakHashMap<>();

	private ComparatorSignalUtil() {
	}

	public static int getSignal(Level level, BlockPos pos) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof MechanicalCoreBlockEntity core) {
			BlockEntityDirection target = core.getAttachedMultiblock(ConfigManager.MAX_PROXY_DISTANCE.get());
			return target == null ? 0 : getSignal(target.blockEntity, target.direction);
		}
		return getSignal(blockEntity, null);
	}

	public static void notifyOutputChanged(BlockEntity source) {
		if (!(source.getLevel() instanceof ServerLevel level)) {
			return;
		}
		BlockPos sourcePos = source.getBlockPos();
		BlockState sourceState = source.getBlockState();
		boolean sourceSignalChanged = false;
		if (sourceState.hasAnalogOutputSignal()) {
			sourceSignalChanged = notifyIfChanged(level, source);
		}
		if (sourceSignalChanged) {
			notifyMultiblockEdges(level, sourcePos);
		}

		ArrayDeque<BlockPos> pending = new ArrayDeque<>();
		Set<BlockPos> visited = new HashSet<>();
		pending.add(sourcePos);
		visited.add(sourcePos);
		int remaining = Math.max(1, ConfigManager.MAX_PROXY_DISTANCE.get());
		while (!pending.isEmpty() && remaining-- > 0) {
			BlockPos current = pending.removeFirst();
			for (Direction direction : Direction.values()) {
				BlockPos adjacent = current.relative(direction);
				if (!visited.add(adjacent)) {
					continue;
				}
				BlockEntity blockEntity = level.getBlockEntity(adjacent);
				if (blockEntity instanceof MechanicalCoreBlockEntity) {
					notifyIfChanged(level, blockEntity);
					pending.addLast(adjacent);
				}
			}
		}
	}

	private static boolean notifyIfChanged(ServerLevel level, BlockEntity blockEntity) {
		int signal = getSignal(level, blockEntity.getBlockPos());
		Integer previous = LAST_SIGNALS.put(blockEntity, signal);
		if (previous == null || previous.intValue() != signal) {
			level.updateNeighbourForOutputSignal(blockEntity.getBlockPos(), blockEntity.getBlockState().getBlock());
			return true;
		}
		return false;
	}

	private static void notifyMultiblockEdges(ServerLevel level, BlockPos centerPos) {
		for (MechEdge edge : MechEdge.values()) {
			BlockPos edgePos = centerPos.subtract(edge.centerPos);
			BlockState edgeState = level.getBlockState(edgePos);
			if (edgeState.getBlock() instanceof MechEdgeBlockBase
					&& edgeState.hasProperty(MechEdgeBlockBase.EDGE)
					&& edgeState.getValue(MechEdgeBlockBase.EDGE) == edge) {
				level.updateNeighbourForOutputSignal(edgePos, edgeState.getBlock());
			}
		}
	}

	private static int getSignal(@Nullable BlockEntity blockEntity, @Nullable Direction side) {
		if (blockEntity == null || blockEntity.isRemoved()) {
			return 0;
		}
		if (blockEntity instanceof CrystalSeedBlockEntity seed) {
			int level = CrystalSeedBlockEntity.getLevel(seed.xp);
			int currentLevelExperience = seed.getRequiredExperienceForLevel(level);
			int nextLevelExperience = seed.getRequiredExperienceForLevel(level + 1);
			return scale(seed.xp - currentLevelExperience, nextLevelExperience - currentLevelExperience);
		}
		if (blockEntity instanceof CopperChargerBlockEntity charger) {
			ItemStack stack = charger.inventory.getStackInSlot(0);
			IEmberCapability itemEmber = CapabilityCompat.getCapability(stack, EmbersCapabilities.EMBER_CAPABILITY, null).orElse(null);
			return itemEmber == null ? 0 : scale(itemEmber.getEmber(), itemEmber.getEmberCapacity());
		}
		if (side == null && blockEntity instanceof MixerCentrifugeBottomBlockEntity mixer) {
			long amount = 0;
			long capacity = 0;
			for (IFluidHandler tank : mixer.getTanks()) {
				amount += tank.getFluidInTank(0).getAmount();
				capacity += Math.max(0, tank.getTankCapacity(0));
			}
			return scale(amount, capacity);
		}

		IFluidHandler fluids = findFluidHandler(blockEntity, side);
		if (fluids != null && fluids.getTanks() > 0) {
			long amount = 0;
			long capacity = 0;
			for (int tank = 0; tank < fluids.getTanks(); tank++) {
				amount += fluids.getFluidInTank(tank).getAmount();
				capacity += Math.max(0, fluids.getTankCapacity(tank));
			}
			return customize(blockEntity, side, scale(amount, capacity), FluidDialBlock.DIAL_TYPE);
		}

		IItemHandler items = findItemHandler(blockEntity, side);
		if (items != null && items.getSlots() > 0) {
			long amount = 0;
			long capacity = 0;
			for (int slot = 0; slot < items.getSlots(); slot++) {
				ItemStack stack = items.getStackInSlot(slot);
				amount += stack.getCount();
				capacity += Math.max(0, Math.min(items.getSlotLimit(slot), stack.isEmpty() ? items.getSlotLimit(slot) : stack.getMaxStackSize()));
			}
			return customize(blockEntity, side, scale(amount, capacity), ItemDialBlock.DIAL_TYPE);
		}

		IEmberCapability ember = findEmberCapability(blockEntity, side);
		if (ember != null && ember.getEmberCapacity() > 0) {
			return customize(blockEntity, side, scale(ember.getEmber(), ember.getEmberCapacity()), EmberDialBlock.DIAL_TYPE);
		}

		IEnergyStorage energy = findEnergyStorage(blockEntity, side);
		if (energy != null && energy.getMaxEnergyStored() > 0) {
			return scale(energy.getEnergyStored(), energy.getMaxEnergyStored());
		}
		return 0;
	}

	private static int customize(BlockEntity blockEntity, @Nullable Direction side, int data, String dialType) {
		if (side != null && blockEntity instanceof IExtraDialInformation extraInformation) {
			return Math.max(0, Math.min(15, extraInformation.getComparatorData(side, data, dialType)));
		}
		return data;
	}

	private static IFluidHandler findFluidHandler(BlockEntity blockEntity, @Nullable Direction side) {
		return findBlockCapability(blockEntity, Capabilities.FluidHandler.BLOCK, side);
	}

	private static IItemHandler findItemHandler(BlockEntity blockEntity, @Nullable Direction side) {
		return findBlockCapability(blockEntity, Capabilities.ItemHandler.BLOCK, side);
	}

	private static IEmberCapability findEmberCapability(BlockEntity blockEntity, @Nullable Direction side) {
		return findBlockCapability(blockEntity, EmbersCapabilities.EMBER_BLOCK_CAPABILITY, side);
	}

	private static <T> T findBlockCapability(BlockEntity blockEntity, BlockCapability<T, Direction> capability, @Nullable Direction side) {
		Level level = blockEntity.getLevel();
		if (level == null) {
			return null;
		}
		if (side != null) {
			T value = level.getCapability(capability, blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, side);
			return value != null ? value : level.getCapability(capability, blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, null);
		}
		T value = level.getCapability(capability, blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, null);
		if (value != null) {
			return value;
		}
		for (Direction direction : Direction.values()) {
			value = level.getCapability(capability, blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, direction);
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	private static IEnergyStorage findEnergyStorage(BlockEntity blockEntity, @Nullable Direction side) {
		return findBlockCapability(blockEntity, Capabilities.EnergyStorage.BLOCK, side);
	}

	private static int scale(double amount, double capacity) {
		if (!Double.isFinite(amount) || !Double.isFinite(capacity) || amount <= 0 || capacity <= 0) {
			return 0;
		}
		if (amount >= capacity) {
			return 15;
		}
		return 1 + (int) Math.floor(14.0D * amount / capacity);
	}
}
