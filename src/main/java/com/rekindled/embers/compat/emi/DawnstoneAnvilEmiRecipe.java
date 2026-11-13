package com.rekindled.embers.compat.emi;

import java.util.ArrayList;
import java.util.List;

import com.rekindled.embers.recipe.IDawnstoneAnvilRecipe;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

/** EMI port of the DawnstoneAnvilCategory. */
public class DawnstoneAnvilEmiRecipe implements EmiRecipe {

	private final EmbersEmiCategory category;
	private final ResourceLocation id;
	private final IDawnstoneAnvilRecipe recipe;

	public DawnstoneAnvilEmiRecipe(EmbersEmiCategory category, RecipeHolder<IDawnstoneAnvilRecipe> holder) {
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
		List<EmiIngredient> inputs = new ArrayList<>();
		if (!recipe.getDisplayInputTop().isEmpty())
			inputs.add(EmiIngredients.items(recipe.getDisplayInputTop()));
		if (!recipe.getDisplayInputBottom().isEmpty())
			inputs.add(EmiIngredients.items(recipe.getDisplayInputBottom()));
		return inputs;
	}

	@Override
	public List<EmiStack> getOutputs() {
		List<EmiStack> outputs = new ArrayList<>();
		for (var stack : recipe.getDisplayOutput())
			outputs.add(EmiStack.of(stack));
		return outputs;
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
		if (!recipe.getDisplayInputTop().isEmpty())
			EmiIngredients.addSlot(widgets, EmiIngredients.items(recipe.getDisplayInputTop()), 22, 19);
		if (!recipe.getDisplayInputBottom().isEmpty())
			EmiIngredients.addSlot(widgets, EmiIngredients.items(recipe.getDisplayInputBottom()), 22, 37);
		if (!recipe.getDisplayOutput().isEmpty())
			EmiIngredients.addSlot(widgets, EmiIngredients.items(recipe.getDisplayOutput()), 77, 28).recipeContext(this);
	}
}
