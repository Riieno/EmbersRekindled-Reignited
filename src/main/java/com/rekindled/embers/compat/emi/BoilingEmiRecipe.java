package com.rekindled.embers.compat.emi;

import java.util.List;

import com.rekindled.embers.recipe.IBoilingRecipe;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;

/** EMI port of the BoilingCategory. */
public class BoilingEmiRecipe implements EmiRecipe {

	private final EmbersEmiCategory category;
	private final ResourceLocation id;
	private final IBoilingRecipe recipe;

	public BoilingEmiRecipe(EmbersEmiCategory category, RecipeHolder<IBoilingRecipe> holder) {
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
		return List.of(EmiIngredients.fluids(recipe.getDisplayInput().getFluids()));
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
		List<FluidStack> fluids = recipe.getDisplayInput().getFluids();
		int amount = fluids.isEmpty() ? 0 : fluids.get(0).getAmount();
		EmiIngredients.ingotTooltip(EmiIngredients.addTank(widgets, getInputs().get(0), 26, 6, 16, 16, amount), fluids.isEmpty() ? FluidStack.EMPTY : fluids.get(0));
		EmiIngredients.ingotTooltip(EmiIngredients.addTank(widgets, getOutputs().get(0), 84, 6, 16, 16, recipe.getDisplayOutput().getAmount()),
				recipe.getDisplayOutput()).recipeContext(this);
	}
}
