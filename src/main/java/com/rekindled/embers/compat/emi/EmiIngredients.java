package com.rekindled.embers.compat.emi;

import java.util.ArrayList;
import java.util.List;

import com.rekindled.embers.datagen.EmbersFluidTags;
import com.rekindled.embers.util.FluidAmounts;

import dev.emi.emi.api.neoforge.NeoForgeEmiStack;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.TankWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.fluids.FluidStack;

public final class EmiIngredients {

	private EmiIngredients() {
	}

	public static EmiStack item(ItemLike item) {
		return EmiStack.of(item);
	}

	/**
	 * EMI places the 18x18 slot background at x,y and the ingredient at x+1,y+1, while the JEI
	 * categories lay their slots out on the 16x16 ingredient rect. Shift so both draw the same pixels.
	 */
	public static SlotWidget addSlot(WidgetHolder widgets, EmiIngredient ingredient, int x, int y) {
		return widgets.addSlot(ingredient, x - 1, y - 1);
	}

	public static EmiIngredient items(List<ItemStack> stacks) {
		List<EmiIngredient> parts = new ArrayList<>(stacks.size());
		for (ItemStack stack : stacks) {
			if (!stack.isEmpty())
				parts.add(EmiStack.of(stack));
		}
		return EmiIngredient.of(parts);
	}

	public static EmiIngredient ingredient(Ingredient ingredient) {
		return EmiIngredient.of(ingredient);
	}

	public static EmiStack fluid(FluidStack stack) {
		return NeoForgeEmiStack.of(stack);
	}

	/**
	 * JEI fluid renderers fill the raw texture socket (16x32 etc) with no slot chrome, while EMI's
	 * TankWidget paints an 18x18 slot background and insets the fluid by 1px inside its bounds.
	 * Growing the bounds by 1 per side and dropping the background reproduces the JEI look.
	 */
	public static TankWidget addTank(WidgetHolder widgets, EmiIngredient stack, int x, int y, int width, int height, int capacity) {
		TankWidget tank = widgets.addTank(stack, x - 1, y - 1, width + 2, height + 2, capacity);
		tank.drawBack(false);
		return tank;
	}

	public static EmiIngredient fluids(List<FluidStack> stacks) {
		if (stacks.isEmpty())
			return EmiStack.EMPTY;
		long amount = stacks.get(0).getAmount();
		List<EmiIngredient> parts = new ArrayList<>(stacks.size());
		for (FluidStack stack : stacks) {
			if (!stack.isEmpty())
				parts.add(NeoForgeEmiStack.of(stack));
		}
		return EmiIngredient.of(parts, amount);
	}

	/** Mirrors the JEI IngotTooltipCallback; EMI already shows the fluid amount itself. */
	public static SlotWidget ingotTooltip(SlotWidget slot, FluidStack stack) {
		if (stack.getFluid().defaultFluidState().is(EmbersFluidTags.INGOT_TOOLTIP) && stack.getAmount() >= FluidAmounts.nuggetValue())
			slot.appendTooltip(FluidAmounts.getIngotTooltip(stack.getAmount()).withStyle(ChatFormatting.GRAY));
		return slot;
	}
}
