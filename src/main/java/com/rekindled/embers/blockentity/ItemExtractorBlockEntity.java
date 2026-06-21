package com.rekindled.embers.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;

import com.rekindled.embers.ConfigManager;
import com.rekindled.embers.Embers;
import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.api.filter.FilterAny;
import com.rekindled.embers.api.filter.IFilter;
import com.rekindled.embers.api.tile.IExtraCapabilityInformation;
import com.rekindled.embers.api.tile.IOrderDestination;
import com.rekindled.embers.api.tile.IOrderSource;
import com.rekindled.embers.api.tile.OrderStack;
import com.rekindled.embers.particle.VaporParticleOptions;
import com.rekindled.embers.util.EmbersColors;
import com.rekindled.embers.compat.sublevel.SubLevelCompat;
import com.rekindled.embers.util.SubLevelParticleUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.rekindled.embers.compat.legacy.capabilities.Capability;
import com.rekindled.embers.compat.legacy.capabilities.ForgeCapabilities;
import com.rekindled.embers.compat.legacy.LazyOptional;
import net.neoforged.neoforge.items.IItemHandler;

public class ItemExtractorBlockEntity extends ItemPipeBlockEntityBase implements IOrderDestination, IExtraCapabilityInformation {

	Random random = new Random();
	IItemHandler[] sideHandlers;
	boolean active;
	List<OrderStack> orders = new ArrayList<>();

	public ItemExtractorBlockEntity(BlockPos pPos, BlockState pBlockState) {
		super(RegistryManager.ITEM_EXTRACTOR_ENTITY.get(), pPos, pBlockState);
	}

	@Override
	protected void initInventory() {
		super.initInventory();
		sideHandlers = new IItemHandler[Direction.values().length];
		for (Direction facing : Direction.values()) {
			sideHandlers[facing.get3DDataValue()] = new IItemHandler() {
				@Override
				public int getSlots() {
					return inventory.getSlots();
				}

				@Nonnull
				@Override
				public ItemStack getStackInSlot(int slot) {
					return inventory.getStackInSlot(slot);
				}

				@Nonnull
				@Override
				public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
					if (isExtractionActive())
						return stack;
					return PipeNetworkUtil.routeItem(ItemExtractorBlockEntity.this, facing, stack, simulate);
				}

				@Nonnull
				@Override
				public ItemStack extractItem(int slot, int amount, boolean simulate) {
					return inventory.extractItem(slot, amount, simulate);
				}

				@Override
				public int getSlotLimit(int slot) {
					return inventory.getSlotLimit(slot);
				}

				@Override
				public boolean isItemValid(int slot, @NotNull ItemStack stack) {
					return true;
				}
			};
		}
	}

	@Override
	public void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.loadAdditional(nbt, registries);
		if (nbt.contains("orders")) {
			ListTag tagOrders = nbt.getList("orders",10);
			orders.clear();
			for (Tag tagOrder : tagOrders) {
				orders.add(new OrderStack((CompoundTag) tagOrder));
			}
		}
	}

	@Override
	public void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.saveAdditional(nbt, registries);
		ListTag tagOrders = new ListTag();
		for (OrderStack order : orders) {
			tagOrders.add(order.writeToNBT(new CompoundTag()));
		}
		nbt.put("orders", tagOrders);
	}

	public static IFilter FILTER_ANY = new FilterAny();

	public static void serverTick(Level level, BlockPos pos, BlockState state, ItemExtractorBlockEntity blockEntity) {
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
		blockEntity.cleanupOrders();
		blockEntity.active = ConfigManager.isRedstoneControlActive(level, pos);
		boolean moved = blockEntity.routeBufferedItem();
		OrderStack currentOrder = blockEntity.orders.isEmpty() ? null : blockEntity.orders.get(0);
		IFilter filter = /*FilterUtil.*/FILTER_ANY;
		if (blockEntity.active)
			currentOrder = null;
		else if (currentOrder != null)
			filter = currentOrder.getFilter();

		IItemHandler invDest = null;
		if(currentOrder != null) {
			IOrderSource destination = currentOrder.getSource(level);
			if(destination != null)
				invDest = destination.getItemHandler();
		}

		if (blockEntity.active || (currentOrder != null && currentOrder.getSize() > 0)) {
			moved |= blockEntity.extractAndRoute(filter, invDest, currentOrder);
		}
		blockEntity.updateRouteState(moved);
	}

	private boolean routeBufferedItem() {
		ItemStack stored = inventory.extractItem(0, getCapacity(), true);
		if (stored.isEmpty()) {
			return false;
		}
		ItemStack remainder = PipeNetworkUtil.routeItem(this, null, stored, false);
		int moved = stored.getCount() - remainder.getCount();
		if (moved > 0) {
			inventory.extractItem(0, moved, false);
		}
		return moved > 0;
	}

	private boolean isExtractionActive() {
		return level != null ? ConfigManager.isRedstoneControlActive(level, worldPosition) : active;
	}

	private boolean extractAndRoute(IFilter filter, IItemHandler invDest, OrderStack currentOrder) {
		for (Direction facing : Direction.values()) {
			if (!PipeNetworkUtil.canUseAdjacentSide(this, facing, ItemPipeBlockEntityBase.class)) {
				continue;
			}
			IItemHandler handler = PipeNetworkUtil.getAdjacentItemHandler(this, facing);
			if (handler == null) {
				continue;
			}
			for (int slot = 0; slot < handler.getSlots(); slot++) {
				ItemStack extracted = handler.extractItem(slot, 1, true);
				if (extracted.isEmpty() || !filter.acceptsItem(extracted, invDest)) {
					continue;
				}
				if (!PipeNetworkUtil.routeItem(this, facing, extracted, true).isEmpty()) {
					continue;
				}
				ItemStack drained = handler.extractItem(slot, 1, false);
				if (drained.isEmpty()) {
					continue;
				}
				ItemStack remainder = PipeNetworkUtil.routeItem(this, facing, drained, false);
				int moved = drained.getCount() - remainder.getCount();
				if (!remainder.isEmpty()) {
					inventory.insertItem(0, remainder, false);
				}
				if (moved > 0) {
					if (currentOrder != null) {
						currentOrder.deplete(moved);
					}
					return true;
				}
			}
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
		if (!this.isRemoved() && cap == ForgeCapabilities.ITEM_HANDLER) {
			if (side == null)
				return ForgeCapabilities.ITEM_HANDLER.orEmpty(cap, holder);
			else
				return ForgeCapabilities.ITEM_HANDLER.orEmpty(cap, LazyOptional.of(() -> this.sideHandlers[side.get3DDataValue()]));
		}
		return LazyOptional.empty();
	}

	@Override
	public int getCapacity() {
		return 4;
	}

	@Override
	public void order(BlockEntity source, IFilter filter, int orderSize) {
		OrderStack order = getOrder(source);
		if (order == null)
			orders.add(new OrderStack(source.getBlockPos(), filter, orderSize));
		else if(Objects.equals(order.getFilter(), filter))
			order.increment(orderSize);
		else {
			order.reset(filter, orderSize);
		}
	}

	@Override
	public void resetOrder(BlockEntity source) {
		orders.removeIf(order -> order.getPos().equals(source.getBlockPos()));
	}

	public OrderStack getOrder(BlockEntity source) {
		for (OrderStack order : orders) {
			if (order.getPos().equals(source.getBlockPos()))
				return order;
		}
		return null;
	}

	private void cleanupOrders() {
		orders.removeIf(this::isOrderInvalid);
	}

	private boolean isOrderInvalid(OrderStack order) {
		return order.getSize() <= 0 || order.getSource(level) == null;
	}

	@Override
	public void addOtherDescription(List<Component> strings, Direction facing) {
		strings.add(Component.translatable(Embers.MODID + ".tooltip.goggles.redstone_signal"));
	}
}
