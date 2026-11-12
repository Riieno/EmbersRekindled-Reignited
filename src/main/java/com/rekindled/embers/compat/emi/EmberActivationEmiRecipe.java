package com.rekindled.embers.compat.emi;

import java.util.List;

import com.rekindled.embers.Embers;
import com.rekindled.embers.recipe.IEmberActivationRecipe;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

/** EMI port of the EmberActivationCategory. */
public class EmberActivationEmiRecipe implements EmiRecipe {

	private final EmbersEmiCategory category;
	private final ResourceLocation id;
	private final IEmberActivationRecipe recipe;

	public EmberActivationEmiRecipe(EmbersEmiCategory category, RecipeHolder<IEmberActivationRecipe> holder) {
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
		return List.of(EmiIngredients.ingredient(recipe.getDisplayInput()));
	}

	@Override
	public List<EmiStack> getOutputs() {
		return List.of();
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
	public boolean supportsRecipeTree() {
		return false;
	}

	@Override
	public boolean hideCraftable() {
		return true;
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		category.drawBackground(widgets);
		EmiIngredients.addSlot(widgets, getInputs().get(0), 6, 6);
		widgets.addText(Component.translatable(Embers.MODID + ".jei.recipe.ember_activation.ember", recipe.getDisplayOutput()), 28, 10, 0xFFFFFF, true);
	}
}
