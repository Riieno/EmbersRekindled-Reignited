package com.rekindled.embers.blockentity;


import java.util.Random;

import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.particle.VaporParticleOptions;
import com.rekindled.embers.util.EmbersColors;
import com.rekindled.embers.compat.sublevel.SubLevelCompat;
import com.rekindled.embers.util.SubLevelParticleUtil;
import com.rekindled.embers.util.Misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import com.rekindled.embers.compat.legacy.capabilities.Capability;
import com.rekindled.embers.compat.legacy.capabilities.ForgeCapabilities;
import com.rekindled.embers.compat.legacy.LazyOptional;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class FluidTransferBlockEntity extends FluidPipeBlockEntityBase {

	public static final int PRIORITY_TRANSFER = -10;
	public FluidStack filterFluid = FluidStack.EMPTY;
	Random random = new Random();
	public boolean syncFilter = true;
	IFluidHandler outputSide;
	IFluidHandler[] sideHandlers;
	public LazyOptional<IFluidHandler> outputHolder = LazyOptional.of(() -> outputSide);

	public FluidTransferBlockEntity(BlockPos pPos, BlockState pBlockState) {
		super(RegistryManager.FLUID_TRANSFER_ENTITY.get(), pPos, pBlockState);
		syncConnections = false;
		saveConnections = false;
	}

	@Override
	protected void initFluidTank() {
		tank = new FluidTank(getCapacity()) {
			@Override
			protected void onContentsChanged() {
				FluidTransferBlockEntity.this.setChanged();
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				if (!filterFluid.isEmpty()) {
					if(resource != null) {
						if (filterFluid.isComponentsPatchEmpty() ? FluidStack.isSameFluid(resource, filterFluid) : FluidStack.isSameFluidSameComponents(resource, filterFluid)) {
							return super.fill(resource, action);
						}
					}
					return 0;
				}
				return super.fill(resource, action);
			}
		};
		outputSide = Misc.makeRestrictedFluidHandler(tank, false, true);
		sideHandlers = new IFluidHandler[Direction.values().length];
		for (Direction facing : Direction.values()) {
			sideHandlers[facing.get3DDataValue()] = new IFluidHandler() {
				@Override
				public int fill(FluidStack resource, FluidAction action) {
					if (!filterFluid.isEmpty()) {
						if (resource == null) {
							return 0;
						}
						boolean matches = filterFluid.isComponentsPatchEmpty()
								? FluidStack.isSameFluid(resource, filterFluid)
								: FluidStack.isSameFluidSameComponents(resource, filterFluid);
						if (!matches) {
							return 0;
						}
					}
					return PipeNetworkUtil.routeFluid(FluidTransferBlockEntity.this, facing, resource, action);
				}

				@Override
				public FluidStack drain(FluidStack resource, FluidAction action) {
					return tank.drain(resource, action);
				}

				@Override
				public FluidStack drain(int maxDrain, FluidAction action) {
					return tank.drain(maxDrain, action);
				}

				@Override
				public int getTanks() {
					return tank.getTanks();
				}

				@Override
				public FluidStack getFluidInTank(int tank) {
					return FluidTransferBlockEntity.this.tank.getFluidInTank(tank);
				}

				@Override
				public int getTankCapacity(int tank) {
					return FluidTransferBlockEntity.this.tank.getTankCapacity(tank);
				}

				@Override
				public boolean isFluidValid(int tank, FluidStack stack) {
					return FluidTransferBlockEntity.this.tank.isFluidValid(tank, stack);
				}
			};
		}
	}

	@Override
	public void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.loadAdditional(nbt, registries);
		if (nbt.contains("filter")) {
			filterFluid = FluidStack.parseOptional(registries, nbt.getCompound("filter"));
		}
	}

	@Override
	protected boolean requiresSync() {
		return syncFilter || super.requiresSync();
	}

	@Override
	protected void resetSync() {
		super.resetSync();
		syncFilter = false;
	}

	@Override
	public void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.saveAdditional(nbt, registries);
		writeFilter(nbt, registries);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag nbt = super.getUpdateTag(registries);
		if (syncFilter)
			writeFilter(nbt, registries);
		return nbt;
	}

	private void writeFilter(CompoundTag nbt, HolderLookup.Provider registries) {
		nbt.put("filter", filterFluid.saveOptional(registries));
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, FluidTransferBlockEntity blockEntity) {
		if (level instanceof ServerLevel && blockEntity.clogged && blockEntity.isAnySideUnclogged()) {
			Random posRand = new Random(pos.asLong());
			double angleA = posRand.nextDouble() * Math.PI * 2;
			double angleB = posRand.nextDouble() * Math.PI * 2;
			float xOffset = (float) (Math.cos(angleA) * Math.cos(angleB));
			float yOffset = (float) (Math.sin(angleA) * Math.cos(angleB));
			float zOffset = (float) Math.sin(angleB);
			float speed = 0.1f;
			float vx = xOffset * speed + posRand.nextFloat() * speed * 0.3f;
			float vy = yOffset * speed + posRand.nextFloat() * speed * 0.3f;
			float vz = zOffset * speed + posRand.nextFloat() * speed * 0.3f;
			SubLevelParticleUtil.send(blockEntity, new VaporParticleOptions(EmbersColors.VAPOR_ID, SubLevelCompat.toPhysicalDirection(blockEntity, new Vec3(vx, vy, vz)), 1.0f), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0, 0, 0, 1.0);
		}
		FluidPipeBlockEntityBase.serverTick(level, pos, state, blockEntity);
	}

	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if (!this.isRemoved() && cap == ForgeCapabilities.FLUID_HANDLER) {
			if (side == null)
				return ForgeCapabilities.FLUID_HANDLER.orEmpty(cap, holder);
			if (level.getBlockState(this.getBlockPos()).hasProperty(BlockStateProperties.FACING)) {
				Direction facing = level.getBlockState(this.getBlockPos()).getValue(BlockStateProperties.FACING);
				if (side.getOpposite() == facing)
					return ForgeCapabilities.FLUID_HANDLER.orEmpty(cap, outputHolder);
				else if (side.getAxis() == facing.getAxis())
					return ForgeCapabilities.FLUID_HANDLER.orEmpty(cap, LazyOptional.of(() -> this.sideHandlers[side.get3DDataValue()]));
			}
		}
		return LazyOptional.empty();
	}

	@Override
	public int getCapacity() {
		return 240;
	}

	@Override
	public int getPriority(Direction facing) {
		return PRIORITY_TRANSFER;
	}

	@Override
	public PipeConnection getConnection(Direction facing) {
		BlockState state = level.getBlockState(this.getBlockPos());
		return state.hasProperty(BlockStateProperties.FACING) && state.getValue(BlockStateProperties.FACING).getAxis() == facing.getAxis() ? PipeConnection.PIPE : PipeConnection.NONE;
	}

	@Override
	protected boolean isFrom(Direction facing) {
		return level.getBlockState(this.getBlockPos()).getValue(BlockStateProperties.FACING) == facing;
	}

	public void invalidateCaps() {
		
		outputHolder.invalidate();
	}
}
