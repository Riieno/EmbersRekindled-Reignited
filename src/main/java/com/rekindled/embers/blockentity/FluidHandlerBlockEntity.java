package com.rekindled.embers.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.rekindled.embers.compat.legacy.LazyOptional;
import com.rekindled.embers.compat.legacy.capabilities.Capability;
import com.rekindled.embers.compat.legacy.capabilities.ForgeCapabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public abstract class FluidHandlerBlockEntity extends BlockEntity {

	public FluidTank tank = new FluidTank(0);
	private final LazyOptional<IFluidHandler> fluidHolder = LazyOptional.of(() -> tank);

	protected FluidHandlerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		tank.readFromNBT(registries, tag);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tank.writeToNBT(registries, tag);
	}

	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if (!this.isRemoved() && cap == ForgeCapabilities.FLUID_HANDLER) {
			return ForgeCapabilities.FLUID_HANDLER.orEmpty(cap, fluidHolder);
		}
		return LazyOptional.empty();
	}

	public void invalidateCaps() {
		fluidHolder.invalidate();
	}
}
