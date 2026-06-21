package com.rekindled.embers.power;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.api.power.IEmberCapability;

import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import com.rekindled.embers.compat.legacy.capabilities.Capability;
import com.rekindled.embers.compat.legacy.LazyOptional;

public class DefaultEmberItemCapability implements IEmberCapability {
	@Nonnull
	public ItemStack stack;
	public final LazyOptional<?> capOptional;
	double ember = 0;
	double capacity = 0;

	public DefaultEmberItemCapability(@Nonnull ItemStack stack, double capacity) {
		this.stack = stack;
		setEmberCapacity(capacity);
		this.capOptional = LazyOptional.of(() -> this);

		CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
		CompoundTag BEnbt = blockEntityData == null ? com.rekindled.embers.util.ItemData.getTagElement(stack, "BlockEntityTag") : blockEntityData.copyTag();
		if (BEnbt != null) {
			setEmberCapacity(BEnbt.getDouble(EMBER_CAPACITY));
			setEmber(BEnbt.getDouble(EMBER));
		}
	}

	@Override
	public void invalidate() {
		capOptional.invalidate();
	}

	public @NotNull <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
		if (capability == EmbersCapabilities.EMBER_CAPABILITY)
			return capOptional.cast();
		return LazyOptional.empty();
	}

	@Override
	public double getEmber() {
		if (stack.isEmpty())
			return 0;
		CompoundTag tag = com.rekindled.embers.util.ItemData.getTagElement(stack, "ForgeCaps");
		return tag == null ? 0 : tag.getDouble(EMBER);
	}

	@Override
	public double getEmberCapacity() {
		if (stack.isEmpty())
			return 0;
		CompoundTag tag = com.rekindled.embers.util.ItemData.getTagElement(stack, "ForgeCaps");
		return tag == null ? 0 : tag.getDouble(EMBER_CAPACITY);
	}

	@Override
	public void setEmber(double value) {
		ember = sanitizeEmber(value, getEmberCapacity());
		double storedEmber = ember;
		com.rekindled.embers.util.ItemData.updateTagElement(stack, "ForgeCaps", tag -> tag.putDouble(EMBER, storedEmber));
	}

	@Override
	public void setEmberCapacity(double value) {
		double currentEmber = getEmber();
		capacity = sanitizeCapacity(value);
		double storedCapacity = capacity;
		com.rekindled.embers.util.ItemData.updateTagElement(stack, "ForgeCaps", tag -> tag.putDouble(EMBER_CAPACITY, storedCapacity));
		setEmber(currentEmber);
	}

	@Override
	public double addAmount(double value, boolean doAdd) {
		double ember = getEmber();
		double capacity = getEmberCapacity();
		double added = Math.min(capacity - ember, sanitizeAmount(value));
		double newEmber = ember + added;
		if (doAdd) {
			if (newEmber != ember)
				onContentsChanged();
			setEmber(newEmber);
		}
		return added;
	}

	@Override
	public double removeAmount(double value, boolean doRemove) {
		double ember = getEmber();
		double removed = Math.min(ember, sanitizeAmount(value));
		double newEmber = ember - removed;
		if (doRemove) {
			if (newEmber != ember)
				onContentsChanged();
			setEmber(newEmber);
		}
		return removed;
	}

	@Override
	public void writeToNBT(CompoundTag nbt) {
		nbt.putDouble(EMBER, sanitizeEmber(getEmber(), getEmberCapacity()));
		nbt.putDouble(EMBER_CAPACITY, sanitizeCapacity(getEmberCapacity()));
	}

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag nbt = new CompoundTag();
		writeToNBT(nbt);
		return nbt;
	}

	@Override
	public void deserializeNBT(CompoundTag nbt) {
		if (nbt.contains(EMBER_CAPACITY))
			setEmberCapacity(nbt.getDouble(EMBER_CAPACITY));
		if (nbt.contains(EMBER))
			setEmber(nbt.getDouble(EMBER));
	}

	@Override
	public void onContentsChanged() {

	}

	private static double sanitizeCapacity(double value) {
		if (!Double.isFinite(value)) {
			return 0;
		}
		return Math.max(0, value);
	}

	private static double sanitizeAmount(double value) {
		if (!Double.isFinite(value)) {
			return 0;
		}
		return Math.max(0, value);
	}

	private static double sanitizeEmber(double value, double capacity) {
		if (!Double.isFinite(value)) {
			return 0;
		}
		return Math.max(0, Math.min(value, capacity));
	}
}
