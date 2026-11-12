package com.rekindled.embers.compat.emi;

import java.text.DecimalFormat;
import java.util.List;

import com.rekindled.embers.Embers;
import com.rekindled.embers.recipe.ICatalysisCombustionRecipe;
import com.rekindled.embers.util.DecimalFormats;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

/** EMI port of the CatalysisCombustionCategory. */
public class CatalysisCombustionEmiRecipe implements EmiRecipe {

	private final EmbersEmiCategory category;
	private final ResourceLocation id;
	private final ICatalysisCombustionRecipe recipe;

	public CatalysisCombustionEmiRecipe(EmbersEmiCategory category, RecipeHolder<ICatalysisCombustionRecipe> holder) {
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
	public List<EmiIngredient> getCatalysts() {
		return List.of(EmiIngredients.ingredient(recipe.getDisplayMachine()));
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
		EmiIngredients.addSlot(widgets, getCatalysts().get(0), 6, 6);
		EmiIngredients.addSlot(widgets, getInputs().get(0), 6, 24);
		DecimalFormat multiplierFormat = DecimalFormats.getDecimalFormat(Embers.MODID + ".decimal_format.ember_multiplier");
		widgets.addText(Component.translatable(Embers.MODID + ".jei.recipe.catalysis_combustion.multiplier", multiplierFormat.format(recipe.getDisplayMultiplier())), 28, 10, 0xFFFFFF, true);
		widgets.addText(Component.translatable(Embers.MODID + ".jei.recipe.catalysis_combustion.burn_time", recipe.getDisplayTime()), 28, 28, 0xFFFFFF, true);
	}
}
