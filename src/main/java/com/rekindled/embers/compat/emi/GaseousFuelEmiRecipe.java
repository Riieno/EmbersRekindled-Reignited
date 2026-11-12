package com.rekindled.embers.compat.emi;

import java.text.DecimalFormat;
import java.util.List;

import com.rekindled.embers.Embers;
import com.rekindled.embers.recipe.IGaseousFuelRecipe;
import com.rekindled.embers.util.DecimalFormats;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;

/** EMI port of the GaseousFuelCategory. */
public class GaseousFuelEmiRecipe implements EmiRecipe {

	private final EmbersEmiCategory category;
	private final ResourceLocation id;
	private final IGaseousFuelRecipe recipe;

	public GaseousFuelEmiRecipe(EmbersEmiCategory category, RecipeHolder<IGaseousFuelRecipe> holder) {
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
		List<FluidStack> fluids = recipe.getDisplayInput().getFluids();
		int amount = fluids.isEmpty() ? 0 : fluids.get(0).getAmount();
		EmiIngredients.ingotTooltip(EmiIngredients.addTank(widgets, getInputs().get(0), 6, 7, 16, 16, amount), fluids.isEmpty() ? FluidStack.EMPTY : fluids.get(0));
		DecimalFormat multiplierFormat = DecimalFormats.getDecimalFormat(Embers.MODID + ".decimal_format.ember_multiplier");
		widgets.addText(Component.translatable(Embers.MODID + ".jei.recipe.gaseous_fuel.burn_time", recipe.getDisplayBurnTime()), 28, 5, 0xFFFFFF, true);
		widgets.addText(Component.translatable(Embers.MODID + ".jei.recipe.gaseous_fuel.power_multiplier", multiplierFormat.format(recipe.getDisplayMultiplier())), 28, 17, 0xFFFFFF, true);
	}
}
