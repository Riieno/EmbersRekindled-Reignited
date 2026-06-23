package com.rekindled.embers.compat.jei;

import com.rekindled.embers.Embers;
import com.rekindled.embers.RegistryManager;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidType;

public class WorldInteractionCategory implements IRecipeCategory<WorldInteractionRecipe> {

	private final IDrawable background;
	private final IDrawable icon;
	private final IDrawable slot;
	private final IDrawable plus;
	private final IDrawable arrow;

	public WorldInteractionCategory(IGuiHelper helper) {
		background = helper.createBlankDrawable(122, 36);
		icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(RegistryManager.SOLIDIFIED_METAL_ITEM.get()));
		slot = helper.getSlotDrawable();
		plus = helper.getRecipePlusSign();
		arrow = helper.getRecipeArrow();
	}

	@Override
	public RecipeType<WorldInteractionRecipe> getRecipeType() {
		return JEIPlugin.WORLD_INTERACTION;
	}

	@Override
	public Component getTitle() {
		return Component.translatable(Embers.MODID + ".jei.recipe.world_interaction");
	}

	@Override
	public IDrawable getBackground() {
		return background;
	}

	@Override
	public IDrawable getIcon() {
		return icon;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, WorldInteractionRecipe recipe, IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.INPUT, 8, 10)
				.setBackground(slot, -1, -1)
				.setFluidRenderer(FluidType.BUCKET_VOLUME, false, 16, 16)
				.addIngredients(NeoForgeTypes.FLUID_STACK, recipe.primaryFluids());

		builder.addSlot(RecipeIngredientRole.INPUT, 45, 10)
				.setBackground(slot, -1, -1)
				.setFluidRenderer(FluidType.BUCKET_VOLUME, false, 16, 16)
				.addIngredient(NeoForgeTypes.FLUID_STACK, recipe.secondaryFluid());

		builder.addSlot(RecipeIngredientRole.OUTPUT, 98, 10)
				.setBackground(slot, -1, -1)
				.addItemStack(recipe.output());
	}

	@Override
	public void draw(WorldInteractionRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
		plus.draw(guiGraphics, 28, 11);
		arrow.draw(guiGraphics, 68, 9);
	}
}
