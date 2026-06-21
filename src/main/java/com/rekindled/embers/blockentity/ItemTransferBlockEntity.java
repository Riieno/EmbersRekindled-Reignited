package com.rekindled.embers.blockentity;

import com.rekindled.embers.util.ItemData;

import java.util.Random;

import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.api.filter.FilterItem;
import com.rekindled.embers.api.filter.IFilter;
import com.rekindled.embers.api.item.IFilterItem;
import com.rekindled.embers.particle.VaporParticleOptions;
import com.rekindled.embers.util.EmbersColors;
import com.rekindled.embers.compat.sublevel.SubLevelCompat;
import com.rekindled.embers.util.SubLevelParticleUtil;
import com.rekindled.embers.util.FilterUtil;
import com.rekindled.embers.util.Misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import com.rekindled.embers.compat.legacy.capabilities.Capability;
import com.rekindled.embers.compat.legacy.capabilities.ForgeCapabilities;
import com.rekindled.embers.compat.legacy.LazyOptional;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ItemTransferBlockEntity extends ItemPipeBlockEntityBase {

	public static final int PRIORITY_TRANSFER = -10;
	public ItemStack filterItem = ItemStack.EMPTY;
	Random random = new Random();
	public boolean syncFilter = true;
	IItemHandler outputSide;
	IItemHandler[] sideHandlers;
	public LazyOptional<IItemHandler> outputHolder = LazyOptional.of(() -> outputSide);

	IFilter filter = FilterUtil.FILTER_ANY;

	public ItemTransferBlockEntity(BlockPos pPos, BlockState pBlockState) {
		super(RegistryManager.ITEM_TRANSFER_ENTITY.get(), pPos, pBlockState);
		syncConnections = false;
		saveConnections = false;
	}

	@Override
	protected void initInventory() {
		inventory = new ItemStackHandler(1) {
			@Override
			public int getSlotLimit(int slot) {
				return ItemTransferBlockEntity.this.getCapacity();
			}

			@Override
			protected void onContentsChanged(int slot) {
				ItemTransferBlockEntity.this.setChanged();
			}

			@Override
			public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
				if (ItemTransferBlockEntity.this.acceptsItem(stack))
					return super.insertItem(slot, stack, simulate);
				else
					return stack;
			}

			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				return ItemTransferBlockEntity.this.acceptsItem(stack);
			}
		};
		outputSide = Misc.makeRestrictedItemHandler(inventory, false, true);
		sideHandlers = new IItemHandler[Direction.values().length];
		for (Direction facing : Direction.values()) {
			sideHandlers[facing.get3DDataValue()] = new IItemHandler() {
				@Override
				public int getSlots() {
					return inventory.getSlots();
				}

				@Override
				public ItemStack getStackInSlot(int slot) {
					return inventory.getStackInSlot(slot);
				}

				@Override
				public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
					if (!ItemTransferBlockEntity.this.acceptsItem(stack)) {
						return stack;
					}
					return PipeNetworkUtil.routeItem(ItemTransferBlockEntity.this, facing, stack, simulate);
				}

				@Override
				public ItemStack extractItem(int slot, int amount, boolean simulate) {
					return inventory.extractItem(slot, amount, simulate);
				}

				@Override
				public int getSlotLimit(int slot) {
					return inventory.getSlotLimit(slot);
				}

				@Override
				public boolean isItemValid(int slot, ItemStack stack) {
					return ItemTransferBlockEntity.this.acceptsItem(stack);
				}
			};
		}
	}

	public boolean acceptsItem(ItemStack stack) {
		return filter.acceptsItem(stack);
	}

	@Override
	public void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.loadAdditional(nbt, registries);
		if (nbt.contains("filter")) {
			filterItem = ItemData.parse(nbt.getCompound("filter"));
		}
		setupFilter();
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
		nbt.put("filter", filterItem.saveOptional(registries));
	}

	public void setupFilter() {
		Item item = this.filterItem.getItem();
		if (item instanceof IFilterItem)
			filter = ((IFilterItem) item).getFilter(this.filterItem);
		else if(!this.filterItem.isEmpty())
			filter = new FilterItem(this.filterItem);
		else
			filter = FilterUtil.FILTER_ANY;
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, ItemTransferBlockEntity blockEntity) {
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
		ItemPipeBlockEntityBase.serverTick(level, pos, state, blockEntity);
	}

	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if (!this.isRemoved() && cap == ForgeCapabilities.ITEM_HANDLER) {
			if (side == null)
				return ForgeCapabilities.ITEM_HANDLER.orEmpty(cap, holder);
			if (level.getBlockState(this.getBlockPos()).hasProperty(BlockStateProperties.FACING)) {
				Direction facing = level.getBlockState(this.getBlockPos()).getValue(BlockStateProperties.FACING);
				if (side.getOpposite() == facing)
					return ForgeCapabilities.ITEM_HANDLER.orEmpty(cap, outputHolder);
				else if (side.getAxis() == facing.getAxis())
					return ForgeCapabilities.ITEM_HANDLER.orEmpty(cap, LazyOptional.of(() -> this.sideHandlers[side.get3DDataValue()]));
			}
		}
		return LazyOptional.empty();
	}

	@Override
	public int getCapacity() {
		return 4;
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
