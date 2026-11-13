package com.rekindled.embers.compat.emi;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.text.WordUtils;

import com.rekindled.embers.Embers;
import com.rekindled.embers.recipe.IBoringRecipe;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

/** EMI port of the BoringCategory, shared by boring and excavation. */
public class BoringEmiRecipe implements EmiRecipe {

	private final EmbersEmiCategory category;
	private final ResourceLocation id;
	private final IBoringRecipe recipe;
	private final EmiIngredient input;

	public BoringEmiRecipe(EmbersEmiCategory category, RecipeHolder<IBoringRecipe> holder) {
		this.category = category;
		this.id = holder.id();
		this.recipe = holder.value();
		this.input = EmiIngredients.items(recipe.getDisplayInput());
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
		return List.of();
	}

	@Override
	public List<EmiIngredient> getCatalysts() {
		return List.of(input);
	}

	@Override
	public List<EmiStack> getOutputs() {
		return List.of(EmiStack.of(recipe.getDisplayOutput().getStack()));
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
		EmiIngredients.addSlot(widgets, getOutputs().get(0), 6, 6).recipeContext(this);
		EmiIngredients.addSlot(widgets, input, 6, 26);

		int textY = 48;
		widgets.addText(Component.translatable(Embers.MODID + ".jei.recipe.boring.weight", recipe.getDisplayOutput().getWeight().asInt()), 28, 10, 0xFFFFFF, true);
		widgets.addText(Component.translatable(Embers.MODID + ".jei.recipe.boring.required_blocks"), 28, 30, 0xFFFFFF, true);
		if (recipe.getMinHeight() != Integer.MIN_VALUE) {
			widgets.addText(Component.translatable(Embers.MODID + ".jei.recipe.boring.min_height", recipe.getMinHeight()), 10, textY, 0xFFFFFF, true);
			textY += 11;
		}
		if (recipe.getMaxHeight() != Integer.MAX_VALUE) {
			widgets.addText(Component.translatable(Embers.MODID + ".jei.recipe.boring.max_height", recipe.getMaxHeight()), 10, textY, 0xFFFFFF, true);
			textY += 11;
		}
		if (!recipe.getDimensions().isEmpty()) {
			widgets.addText(Component.translatable(Embers.MODID + ".jei.recipe.boring.dimensions"), 10, textY, 0xFFB54D, true);
			widgets.addTooltip(tooltip(dimensionTooltip()), 7, textY, 112, 11);
			textY += 11;
		}
		if (!recipe.getBiomes().isEmpty()) {
			widgets.addText(Component.translatable(Embers.MODID + ".jei.recipe.boring.biomes"), 10, textY, 0xFFB54D, true);
			widgets.addTooltip(tooltip(biomeTooltip()), 7, textY, 112, 11);
		}
	}

	private List<Component> dimensionTooltip() {
		List<Component> text = new ArrayList<>();
		recipe.getDimensions().forEach(dimension -> text.add(Component.translatableWithFallback("dimension." + dimension.toLanguageKey(), WordUtils.capitalize(dimension.getPath().replace("_", " ")))));
		return text;
	}

	private List<Component> biomeTooltip() {
		List<Component> text = new ArrayList<>();
		recipe.getBiomes().forEach(biome -> text.add(Component.literal(biome.toString())));
		return text;
	}

	private static List<ClientTooltipComponent> tooltip(List<Component> lines) {
		return lines.stream().map(Component::getVisualOrderText).map(ClientTooltipComponent::create).toList();
	}
}
