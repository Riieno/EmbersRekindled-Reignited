package com.rekindled.embers.blockentity;

import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

public abstract class OpenTankBlockEntity extends FluidHandlerBlockEntity {

	FluidStack lastEscaped = null;
	long lastEscapedTickServer;
	long lastEscapedTickClient;

	public OpenTankBlockEntity(@NotNull BlockEntityType<?> blockEntityType, BlockPos pos, BlockState state) {
		super(blockEntityType, pos, state);
	}

	@Override
	public void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.loadAdditional(nbt, registries);
		if (nbt.contains("lastEscaped")) {
			lastEscaped = FluidStack.parseOptional(registries, nbt.getCompound("lastEscaped"));
			lastEscapedTickServer = nbt.getLong("lastEscapedTick");
		}
	}

	@Override
	public void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.saveAdditional(nbt, registries);
		if (lastEscaped != null) {
			nbt.put("lastEscaped", lastEscaped.save(registries));
			nbt.putLong("lastEscapedTick", lastEscapedTickServer);
		}
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag nbt = super.getUpdateTag(registries);
		tank.writeToNBT(registries, nbt);
		if (lastEscaped != null) {
			nbt.put("lastEscaped", lastEscaped.save(registries));
			nbt.putLong("lastEscapedTick", lastEscapedTickServer);
		}
		return nbt;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void setChanged() {
		super.setChanged();
		if (level instanceof ServerLevel serverLevel) {
			serverLevel.getChunkSource().blockChanged(worldPosition);
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
		}
	}

	public void setEscapedFluid(FluidStack stack) {
		if (stack != null && !stack.isEmpty()) {
			lastEscaped = stack.copy();
			lastEscapedTickServer = level.getLevelData().getGameTime();

			this.setChanged();
		}
	}

	protected boolean shouldEmitParticles() {
		if (lastEscaped == null || lastEscaped.isEmpty())
			return false;
		if (lastEscapedTickClient < lastEscapedTickServer) {
			lastEscapedTickClient = lastEscapedTickServer;
			return true;
		}
		long dTime = level.getLevelData().getGameTime() - lastEscapedTickClient;
		if (dTime < lastEscaped.getAmount() + 5)
			return true;
		return false;
	}

	protected abstract void updateEscapeParticles();
}
