package com.rekindled.embers.blockentity;

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.rekindled.embers.ConfigManager;
import com.rekindled.embers.Embers;
import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.api.tile.IExtraCapabilityInformation;
import com.rekindled.embers.particle.VaporParticleOptions;
import com.rekindled.embers.util.EmbersColors;
import com.rekindled.embers.compat.sublevel.SubLevelCompat;
import com.rekindled.embers.util.SubLevelParticleUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.rekindled.embers.compat.legacy.capabilities.Capability;
import com.rekindled.embers.compat.legacy.capabilities.ForgeCapabilities;
import com.rekindled.embers.compat.legacy.LazyOptional;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public class FluidExtractorBlockEntity extends FluidPipeBlockEntityBase implements IExtraCapabilityInformation {

	Random random = new Random();
	IFluidHandler[] sideHandlers;
	boolean active;
	public static final int MAX_DRAIN = 120;

	public FluidExtractorBlockEntity(BlockPos pPos, BlockState pBlockState) {
		super(RegistryManager.FLUID_EXTRACTOR_ENTITY.get(), pPos, pBlockState);
	}

	@Override
	protected void initFluidTank() {
		super.initFluidTank();
		sideHandlers = new IFluidHandler[Direction.values().length];
		for (Direction facing : Direction.values()) {
			sideHandlers[facing.get3DDataValue()] = new IFluidHandler() {

				@Override
				public int fill(FluidStack resource, FluidAction action) {
					if (isExtractionActive())
						return 0;
					return PipeNetworkUtil.routeFluid(FluidExtractorBlockEntity.this, facing, resource, action);
				}

				@Nullable
				@Override
				public FluidStack drain(FluidStack resource, FluidAction action) {
					return tank.drain(resource, action);
				}

				@Nullable
				@Override
				public FluidStack drain(int maxDrain, FluidAction action) {
					return tank.drain(maxDrain, action);
				}

				@Override
				public int getTanks() {
					return tank.getTanks();
				}

				@Override
				public @NotNull FluidStack getFluidInTank(int tankNum) {
					return tank.getFluidInTank(tankNum);
				}

				@Override
				public int getTankCapacity(int tankNum) {
					return tank.getTankCapacity(tankNum);
				}

				@Override
				public boolean isFluidValid(int tankNum, @NotNull FluidStack stack) {
					return tank.isFluidValid(tankNum, stack);
				}
			};
		}
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, FluidExtractorBlockEntity blockEntity) {
		if (!blockEntity.loaded)
			blockEntity.initConnections();
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
		blockEntity.active = ConfigManager.isRedstoneControlActive(level, pos);
		boolean moved = blockEntity.routeBufferedFluid();
		if (blockEntity.active) {
			moved |= blockEntity.extractAndRoute();
		}
		blockEntity.updateRouteState(moved);
	}

	private boolean routeBufferedFluid() {
		FluidStack stored = tank.drain(MAX_PUSH, FluidAction.SIMULATE);
		if (stored.isEmpty()) {
			return false;
		}
		int moved = PipeNetworkUtil.routeFluid(this, null, stored, FluidAction.EXECUTE);
		if (moved > 0) {
			tank.drain(moved, FluidAction.EXECUTE);
		}
		return moved > 0;
	}

	private boolean isExtractionActive() {
		return level != null ? ConfigManager.isRedstoneControlActive(level, worldPosition) : active;
	}

	private boolean extractAndRoute() {
		for (Direction facing : Direction.values()) {
			if (!PipeNetworkUtil.canUseAdjacentSide(this, facing, FluidPipeBlockEntityBase.class)) {
				continue;
			}
			IFluidHandler handler = PipeNetworkUtil.getAdjacentFluidHandler(this, facing);
			if (handler == null) {
				continue;
			}
			FluidStack extracted = handler.drain(MAX_DRAIN, FluidAction.SIMULATE);
			if (extracted.isEmpty()) {
				continue;
			}
			int accepted = PipeNetworkUtil.routeFluid(this, facing, extracted, FluidAction.SIMULATE);
			if (accepted <= 0) {
				continue;
			}
			FluidStack drained = handler.drain(extracted.copyWithAmount(accepted), FluidAction.EXECUTE);
			if (drained.isEmpty()) {
				continue;
			}
			int moved = PipeNetworkUtil.routeFluid(this, facing, drained, FluidAction.EXECUTE);
			if (moved < drained.getAmount()) {
				tank.fill(drained.copyWithAmount(drained.getAmount() - moved), FluidAction.EXECUTE);
			}
			return moved > 0;
		}
		return false;
	}

	private void updateRouteState(boolean moved) {
		if (!moved && lastTransfer != null) {
			lastTransfer = null;
			syncTransfer = true;
			setChanged();
		}
		if (clogged && moved) {
			clogged = false;
			syncCloggedFlag = true;
			setChanged();
		}
	}

	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if (!this.isRemoved() && cap == ForgeCapabilities.FLUID_HANDLER) {
			if (side == null)
				return ForgeCapabilities.FLUID_HANDLER.orEmpty(cap, holder);
			else if (getConnection(side).transfer)
				return ForgeCapabilities.FLUID_HANDLER.orEmpty(cap, LazyOptional.of(() -> this.sideHandlers[side.get3DDataValue()]));
		}
		return LazyOptional.empty();
	}

	@Override
	public int getCapacity() {
		return 240;
	}

	@Override
	public void addOtherDescription(List<Component> strings, Direction facing) {
		strings.add(Component.translatable(Embers.MODID + ".tooltip.goggles.redstone_signal"));
	}
}
