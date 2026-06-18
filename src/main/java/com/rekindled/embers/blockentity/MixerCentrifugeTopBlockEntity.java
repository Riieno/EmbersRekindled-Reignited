package com.rekindled.embers.blockentity;

import java.util.List;

import com.rekindled.embers.Embers;
import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.api.power.IEmberCapability;
import com.rekindled.embers.api.tile.IExtraCapabilityInformation;
import com.rekindled.embers.api.tile.IUpgradeable;
import com.rekindled.embers.power.DefaultEmberCapability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.rekindled.embers.compat.legacy.capabilities.Capability;
import com.rekindled.embers.compat.legacy.capabilities.ForgeCapabilities;
import com.rekindled.embers.compat.legacy.LazyOptional;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class MixerCentrifugeTopBlockEntity extends FluidHandlerBlockEntity implements IExtraCapabilityInformation, IUpgradeable {

	public static int capacity = FluidType.BUCKET_VOLUME * 8;
	public boolean loaded = false;
	public float renderOffset;
	int previousFluid;

	public IEmberCapability capability = new DefaultEmberCapability() {
		@Override
		public void onContentsChanged() {
			super.onContentsChanged();
			MixerCentrifugeTopBlockEntity.this.setChanged();
		}
	};
	private final IFluidHandler outputHandler = new IFluidHandler() {
		@Override
		public int getTanks() {
			return tank.getTanks();
		}

		@Override
		public FluidStack getFluidInTank(int tank) {
			return MixerCentrifugeTopBlockEntity.this.tank.getFluidInTank(tank);
		}

		@Override
		public int getTankCapacity(int tank) {
			return MixerCentrifugeTopBlockEntity.this.tank.getTankCapacity(tank);
		}

		@Override
		public boolean isFluidValid(int tank, FluidStack stack) {
			return false;
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			return 0;
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			return MixerCentrifugeTopBlockEntity.this.tank.drain(resource, action);
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return MixerCentrifugeTopBlockEntity.this.tank.drain(maxDrain, action);
		}
	};
	private final LazyOptional<IFluidHandler> fluidOutputHolder = LazyOptional.of(() -> outputHandler);

	public MixerCentrifugeTopBlockEntity(BlockPos pPos, BlockState pBlockState) {
		super(RegistryManager.MIXER_CENTRIFUGE_TOP_ENTITY.get(), pPos, pBlockState);
		tank = new FluidTank(capacity) {
			@Override
			public void onContentsChanged() {
				MixerCentrifugeTopBlockEntity.this.setChanged();
			}
		};
		capability.setEmberCapacity(8000);
		capability.setEmber(0);
	}

	@Override
	public void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.loadAdditional(nbt, registries);
		capability.deserializeNBT(nbt);
	}

	@Override
	public void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.saveAdditional(nbt, registries);
		capability.writeToNBT(nbt);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag nbt = super.getUpdateTag(registries);
        tank.writeToNBT(registries, nbt);
		return nbt;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	public int getCapacity(){
		return tank.getCapacity();
	}

	public FluidStack getFluidStack() {
		return tank.getFluid();
	}

	public FluidTank getTank() {
		return tank;
	}

	public static void clientTick(Level level, BlockPos pos, BlockState state, MixerCentrifugeTopBlockEntity blockEntity) {
		//I know I'm supposed to use onLoad for stuff on the first tick but the tank isn't synced to the client yet when that happens
		if (!blockEntity.loaded) {
			blockEntity.previousFluid = blockEntity.tank.getFluidAmount();
			blockEntity.loaded = true;
		}
		if (blockEntity.tank.getFluidAmount() != blockEntity.previousFluid) {
			blockEntity.renderOffset = blockEntity.renderOffset + blockEntity.tank.getFluidAmount() - blockEntity.previousFluid;
			blockEntity.previousFluid = blockEntity.tank.getFluidAmount();
		}
	}

	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if (!this.isRemoved() && cap == ForgeCapabilities.FLUID_HANDLER) {
			return ForgeCapabilities.FLUID_HANDLER.orEmpty(cap, fluidOutputHolder);
		}
		if (!this.isRemoved() && cap == EmbersCapabilities.EMBER_CAPABILITY) {
			return capability.getCapability(cap, side);
		}
		return super.getCapability(cap, side);
	}

	public void invalidateCaps() {
		super.invalidateCaps();
		capability.invalidate();
		fluidOutputHolder.invalidate();
	}

	@Override
	public void setChanged() {
		super.setChanged();
		if (level instanceof ServerLevel serverLevel) {
			serverLevel.getChunkSource().blockChanged(worldPosition);
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
		}
	}

	@Override
	public boolean hasCapabilityDescription(Capability<?> capability) {
		return capability == EmbersCapabilities.EMBER_CAPABILITY || capability == ForgeCapabilities.FLUID_HANDLER;
	}

	@Override
	public void addCapabilityDescription(List<Component> strings, Capability<?> capability, Direction facing) {
		if (capability == EmbersCapabilities.EMBER_CAPABILITY)
			strings.add(IExtraCapabilityInformation.formatCapability(EnumIOType.INPUT, Embers.MODID + ".tooltip.goggles.ember", null));
		if (capability == ForgeCapabilities.FLUID_HANDLER)
			strings.add(IExtraCapabilityInformation.formatCapability(EnumIOType.OUTPUT, Embers.MODID + ".tooltip.goggles.fluid", Component.translatable(Embers.MODID + ".tooltip.goggles.fluid.metal")));
	}

	@Override
	public boolean isSideUpgradeSlot(Direction face) {
		return true;
	}
}
