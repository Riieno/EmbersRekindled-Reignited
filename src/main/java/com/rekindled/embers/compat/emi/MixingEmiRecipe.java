package com.rekindled.embers.compat.emi;

import java.util.ArrayList;
import java.util.List;

import com.rekindled.embers.recipe.FluidIngredient;
import com.rekindled.embers.recipe.IMixingRecipe;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

/** EMI port of the MixingCategory. */
public class MixingEmiRecipe implements EmiRecipe {

	// slot positions in the JEI layout, in display order
	private static final int[][] INPUT_SLOTS = {{34, 7}, {59, 7}, {9, 7}, {83, 7}};

	private final EmbersEmiCategory category;
	private final ResourceLocation id;
	private final IMixingRecipe recipe;
	private final List<EmiIngredient> inputs = new ArrayList<>();

	public MixingEmiRecipe(EmbersEmiCategory category, RecipeHolder<IMixingRecipe> holder) {
		this.category = category;
		this.id = holder.id();
		this.recipe = holder.value();
		int count = 0;
		for (FluidIngredient ingredient : recipe.getDisplayInputFluids()) {
			if (count > 3)
				break;
			inputs.add(EmiIngredients.fluids(ingredient.getFluids()));
			count++;
		}
	}

	@Override
	public EmbersEmiCategory getCategory() {
		return category;
	}

	@Override
	public ResourceLocation getId() {
		return id;
	}

	@Override
	public List<EmiIngredient> getInputs() {
		return inputs;
	}

	@Override
	public List<EmiStack> getOutputs() {
		return List.of(EmiIngredients.fluid(recipe.getDisplayOutput()));
	}

	@Override
	public int getDisplayWidth() {
		return category.getWidth();
	}

	@Override
	public int getDisplayHeight() {
		return category.getHeight();
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		category.drawBackground(widgets);
		// same fill heights as the JEI fluid renderers
		double scale = 1.0 / 1000.0;
		int capacity = FluidType.BUCKET_VOLUME * 8;
		List<FluidIngredient> fluidInputs = recipe.getDisplayInputFluids();
		for (int i = 0; i < inputs.size() && i < INPUT_SLOTS.length; i++) {
			FluidStack stack = firstFluid(fluidInputs, i);
			int tankCapacity = stack.isEmpty() ? capacity : (int) (capacity * scale + stack.getAmount() * (1.0 - scale));
			EmiIngredients.ingotTooltip(EmiIngredients.addTank(widgets, inputs.get(i), INPUT_SLOTS[i][0], INPUT_SLOTS[i][1], 16, 32, tankCapacity), stack);
		}
		EmiIngredients.ingotTooltip(EmiIngredients.addTank(widgets, EmiIngredients.fluid(recipe.getDisplayOutput()), 46, 84, 16, 32,
				(int) (capacity * scale + recipe.getDisplayOutput().getAmount() * (1.0 - scale))),
				recipe.getDisplayOutput()).recipeContext(this);
	}

	private static FluidStack firstFluid(List<FluidIngredient> fluidInputs, int index) {
		if (index >= fluidInputs.size())
			return FluidStack.EMPTY;
		List<FluidStack> fluids = fluidInputs.get(index).getFluids();
		return fluids.isEmpty() ? FluidStack.EMPTY : fluids.get(0);
	}
}
