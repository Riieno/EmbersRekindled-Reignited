package com.rekindled.embers.compat.emi;

import java.util.ArrayList;
import java.util.List;

import com.rekindled.embers.recipe.IMeltingRecipe;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidType;

/** EMI port of the MeltingCategory and the MeltingBonusCategory. */
public class MeltingEmiRecipe implements EmiRecipe {

	private final EmbersEmiCategory category;
	private final ResourceLocation id;
	private final IMeltingRecipe recipe;
	private final boolean bonus;

	public MeltingEmiRecipe(EmbersEmiCategory category, RecipeHolder<IMeltingRecipe> holder, boolean bonus) {
		this.category = category;
		this.id = holder.id();
		this.recipe = holder.value();
		this.bonus = bonus;
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
		return List.of(EmiIngredients.ingredient(recipe.getDisplayInput()));
	}

	@Override
	public List<EmiStack> getOutputs() {
		List<EmiStack> outputs = new ArrayList<>();
		outputs.add(EmiIngredients.fluid(recipe.getDisplayOutput()));
		if (bonus && !recipe.getBonus().isEmpty())
			outputs.add(EmiIngredients.fluid(recipe.getBonus()));
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
		EmiIngredients.addSlot(widgets, getInputs().get(0), 8, 14);
		// same fill heights as the JEI fluid renderers
		double scale = 1.0 / 32.0;
		int outputCapacity = (int) (FluidType.BUCKET_VOLUME * 4 * scale + recipe.getDisplayOutput().getAmount() * (1.0 - scale));
		EmiIngredients.ingotTooltip(EmiIngredients.addTank(widgets, EmiIngredients.fluid(recipe.getDisplayOutput()), bonus ? 63 : 84, 6, 16, 32,
				outputCapacity), recipe.getDisplayOutput()).recipeContext(this);
		if (bonus && !recipe.getBonus().isEmpty()) {
			int bonusCapacity = (int) (FluidType.BUCKET_VOLUME * scale + recipe.getBonus().getAmount() * (1.0 - scale));
			EmiIngredients.ingotTooltip(EmiIngredients.addTank(widgets, EmiIngredients.fluid(recipe.getBonus()), 84, 26, 16, 12,
					bonusCapacity), recipe.getBonus());
		}
	}
}
