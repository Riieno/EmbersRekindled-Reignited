package com.rekindled.embers.blockentity;

import java.util.List;
import java.util.Random;

import org.joml.Vector3f;

import com.rekindled.embers.ConfigManager;
import com.rekindled.embers.Embers;
import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.api.tile.IExtraCapabilityInformation;
import com.rekindled.embers.particle.VaporParticleOptions;
import com.rekindled.embers.util.Misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import com.rekindled.embers.compat.legacy.capabilities.Capability;
import com.rekindled.embers.compat.legacy.capabilities.ForgeCapabilities;
import com.rekindled.embers.compat.legacy.LazyOptional;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class MelterTopBlockEntity extends OpenTankBlockEntity implements IExtraCapabilityInformation {

	public double angle = 0;
	int ticksExisted = 0;
	public float renderOffset;
	int previousFluid;

	public ItemStackHandler inventory = new ItemStackHandler(1) {
		@Override
		protected void onContentsChanged(int slot) {
			MelterTopBlockEntity.this.setChanged();
		}
	};
	public LazyOptional<IItemHandler> holder = LazyOptional.of(() -> inventory);
	private final IFluidHandler outputHandler = new IFluidHandler() {
		@Override
		public int getTanks() {
			return tank.getTanks();
		}

		@Override
		public FluidStack getFluidInTank(int tank) {
			return MelterTopBlockEntity.this.tank.getFluidInTank(tank);
		}

		@Override
		public int getTankCapacity(int tank) {
			return MelterTopBlockEntity.this.tank.getTankCapacity(tank);
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
			return MelterTopBlockEntity.this.tank.drain(resource, action);
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return MelterTopBlockEntity.this.tank.drain(maxDrain, action);
		}
	};
	private final LazyOptional<IFluidHandler> fluidOutputHolder = LazyOptional.of(() -> outputHandler);

	public MelterTopBlockEntity(BlockPos pPos, BlockState pBlockState) {
		super(RegistryManager.MELTER_TOP_ENTITY.get(), pPos, pBlockState);
		tank = new FluidTank(ConfigManager.MELTER_CAPACITY.get()) {
			@Override
			public void onContentsChanged() {
				MelterTopBlockEntity.this.setChanged();
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				if (Misc.isGaseousFluid(resource)) {
					MelterTopBlockEntity.this.setEscapedFluid(resource);
					return resource.getAmount();
				}
				int filled = super.fill(resource, action);
				return filled;
			}
		};
	}

	@Override
	public void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.loadAdditional(nbt, registries);
		inventory.deserializeNBT(registries, nbt.getCompound("inventory"));
	}

	@Override
	public void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.saveAdditional(nbt, registries);
		nbt.put("inventory", inventory.serializeNBT(registries));
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag nbt = super.getUpdateTag(registries);
		nbt.put("inventory", inventory.serializeNBT(registries));
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

	public static void serverTick(Level level, BlockPos pos, BlockState state, MelterTopBlockEntity blockEntity) {
		blockEntity.ticksExisted ++;
		if (blockEntity.ticksExisted % 10 == 0){

			List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos.getX(),pos.getY(),pos.getZ(),pos.getX()+1,pos.getY()+0.5,pos.getZ()+1));
			for (int i = 0; i < items.size(); i ++){
				ItemStack stack = blockEntity.inventory.insertItem(0, items.get(i).getItem(), false);
				if (!stack.isEmpty()){
					items.get(i).setItem(stack);
				} else {
					items.get(i).remove(RemovalReason.DISCARDED);
				}
			}
		}
	}

	public static void clientTick(Level level, BlockPos pos, BlockState state, MelterTopBlockEntity blockEntity) {
		blockEntity.angle++;

		//I know I'm supposed to use onLoad for stuff on the first tick but the tank isn't synced to the client yet when that happens
		//also I have the angle thing anyways
		if (blockEntity.angle == 1)
			blockEntity.previousFluid = blockEntity.tank.getFluidAmount();
		if (blockEntity.tank.getFluidAmount() != blockEntity.previousFluid) {
			blockEntity.renderOffset = blockEntity.renderOffset + blockEntity.tank.getFluidAmount() - blockEntity.previousFluid;
			blockEntity.previousFluid = blockEntity.tank.getFluidAmount();
		}

		if (blockEntity.shouldEmitParticles())
			blockEntity.updateEscapeParticles();
	}

	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if (!this.isRemoved() && cap == ForgeCapabilities.FLUID_HANDLER) {
			return ForgeCapabilities.FLUID_HANDLER.orEmpty(cap, fluidOutputHolder);
		}
		if (!this.isRemoved() && cap == ForgeCapabilities.ITEM_HANDLER) {
			return ForgeCapabilities.ITEM_HANDLER.orEmpty(cap, holder);
		}
		if (!this.isRemoved() && cap == EmbersCapabilities.EMBER_CAPABILITY && level != null && level.getBlockEntity(worldPosition.below()) instanceof MelterBottomBlockEntity bottom) {
			return bottom.getCapability(cap, side);
		}
		return super.getCapability(cap, side);
	}

	public void invalidateCaps() {
		super.invalidateCaps();
		holder.invalidate();
		fluidOutputHolder.invalidate();
	}

	@SuppressWarnings("resource")
	@Override
	protected void updateEscapeParticles() {
		Vector3f color = IClientFluidTypeExtensions.of(lastEscaped.getFluid().getFluidType()).modifyFogColor(Minecraft.getInstance().gameRenderer.getMainCamera(), 0, (ClientLevel) this.level, 6, 0, new Vector3f(1, 1, 1));
		Random random = new Random();
		for (int i = 0; i < 3; i++) {
			float xOffset = 0.5f + (random.nextFloat() - 0.5f) * 2 * 0.2f;
			float yOffset = 0.9f;
			float zOffset = 0.5f + (random.nextFloat() - 0.5f) * 2 * 0.2f;
			level.addParticle(new VaporParticleOptions(color, 2.0f), worldPosition.getX() + xOffset, worldPosition.getY() + yOffset, worldPosition.getZ() + zOffset, 0, 1 / 5f, 0);
		}
	}

	@Override
	public boolean hasCapabilityDescription(Capability<?> capability) {
		return capability == EmbersCapabilities.EMBER_CAPABILITY || capability == ForgeCapabilities.FLUID_HANDLER || capability == ForgeCapabilities.ITEM_HANDLER;
	}

	@Override
	public void addCapabilityDescription(List<Component> strings, Capability<?> capability, Direction facing) {
		if (capability == EmbersCapabilities.EMBER_CAPABILITY)
			strings.add(IExtraCapabilityInformation.formatCapability(EnumIOType.INPUT, Embers.MODID + ".tooltip.goggles.ember", null));
		if (capability == ForgeCapabilities.ITEM_HANDLER)
			strings.add(IExtraCapabilityInformation.formatCapability(EnumIOType.INPUT, Embers.MODID + ".tooltip.goggles.item", null));
		if (capability == ForgeCapabilities.FLUID_HANDLER)
			strings.add(IExtraCapabilityInformation.formatCapability(EnumIOType.OUTPUT, Embers.MODID + ".tooltip.goggles.fluid", Component.translatable(Embers.MODID + ".tooltip.goggles.fluid.metal")));
	}
}
