package com.rekindled.embers.compat.emi;

import java.util.List;

import com.rekindled.embers.recipe.IStampingRecipe;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;

/** EMI port of the StampingCategory. */
public class StampingEmiRecipe implements EmiRecipe {

	private final EmbersEmiCategory category;
	private final ResourceLocation id;
	private final IStampingRecipe recipe;

	public StampingEmiRecipe(EmbersEmiCategory category, RecipeHolder<IStampingRecipe> holder) {
		this.category = category;
		this.id = holder.id();
		this.recipe = holder.value();
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
		return List.of(EmiIngredients.ingredient(recipe.getDisplayInput()), EmiIngredients.fluids(recipe.getDisplayInputFluid().getFluids()));
	}

	@Override
	public List<EmiIngredient> getCatalysts() {
		return List.of(EmiIngredients.ingredient(recipe.getDisplayStamp()));
	}

	@Override
	public List<EmiStack> getOutputs() {
		return List.of(EmiStack.of(recipe.getResultItem()));
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
		EmiIngredients.addSlot(widgets, getInputs().get(0), 8, 28);
		EmiIngredients.addSlot(widgets, getCatalysts().get(0), 47, 7);
		EmiIngredients.addSlot(widgets, getOutputs().get(0), 84, 28).recipeContext(this);
		List<FluidStack> fluids = recipe.getDisplayInputFluid().getFluids();
		int amount = fluids.isEmpty() ? 0 : fluids.get(0).getAmount();
		// same fill height as the JEI fluid renderer
		double scale = 1.0 / 16.0;
		int capacity = (int) (1500 * scale + amount * (1.0 - scale));
		EmiIngredients.ingotTooltip(EmiIngredients.addTank(widgets, getInputs().get(1), 47, 48, 16, 32, capacity),
				fluids.isEmpty() ? FluidStack.EMPTY : fluids.get(0));
	}
}
