package com.rekindled.embers.recipe;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.rekindled.embers.Embers;
import com.rekindled.embers.api.augment.AugmentUtil;
import com.rekindled.embers.api.augment.IAugment;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class AnvilAugmentRecipe implements IDawnstoneAnvilRecipe, IVisuallySplitRecipe<IDawnstoneAnvilRecipe> {

	public static final Serializer SERIALIZER = new Serializer(); 

	public final ResourceLocation id;

	public final Ingredient tool;
	public final Ingredient input;
	public final IAugment augment;

	public static List<IDawnstoneAnvilRecipe> visualRecipes = new ArrayList<IDawnstoneAnvilRecipe>();

	public AnvilAugmentRecipe(ResourceLocation id, Ingredient tool, Ingredient input, IAugment augment) {
		this.id = id;
		this.tool = tool;
		this.input = input;
		this.augment = augment;
	}

	@Override
	public boolean matches(RecipeInput context, Level pLevel) {
		ItemStack toolStack = context.getItem(0);
		boolean matchesTool = tool.test(toolStack) || isCompatibleArmor(toolStack);
		if (augment.countTowardsTotalLevel()) {
			return matchesTool && input.test(context.getItem(1)) && AugmentUtil.getLevel(toolStack) > AugmentUtil.getTotalAugmentLevel(toolStack);
		} else 
			return matchesTool && input.test(context.getItem(1));
	}

	private boolean isCompatibleArmor(ItemStack stack) {
		EquipmentSlot slot = stack.getEquipmentSlot();
		if (slot == null) {
			Equipable equipable = Equipable.get(stack);
			slot = equipable == null ? null : equipable.getEquipmentSlot();
		}
		if (slot == null || !slot.isArmor()) {
			return false;
		}
		return switch (augment.getName().toString()) {
			case "embers:core", "embers:cinder_jet", "embers:blasting_core", "embers:flame_barrier",
					"embers:eldritch_insignia", "embers:intelligent_apparatus", "embers:shifting_scales" -> true;
			case "embers:tinker_lens", "embers:smoky_tinker_lens" -> slot == EquipmentSlot.HEAD;
			case "embers:winding_gears" -> slot == EquipmentSlot.FEET;
			default -> false;
		};
	}

	@Override
	public List<ItemStack> getOutput(RecipeInput context) {
		ItemStack result = context.getItem(0).copy();
		AugmentUtil.addAugment(result, context.getItem(1), augment);
		return List.of(result);
	}

	@Override
	public List<IDawnstoneAnvilRecipe> getVisualRecipes() {
		visualRecipes.clear();
		// one display whose tool slot cycles the whole ingredient, rather than one entry per tool
		List<ItemStack> leveledTools = new ArrayList<>();
		List<ItemStack> augmentedTools = new ArrayList<>();
		for (ItemStack stack : tool.getItems()) {
			try {
				ItemStack leveledTool = stack.copy();
				if (augment.countTowardsTotalLevel()) {
					AugmentUtil.setLevel(leveledTool, AugmentUtil.getLevel(leveledTool) + 1);
				}
				ItemStack augmentedTool = leveledTool.copy();
				AugmentUtil.addAugment(augmentedTool, ItemStack.EMPTY, augment);
				leveledTools.add(leveledTool);
				augmentedTools.add(augmentedTool);
			} catch (Exception e) {
				Embers.LOGGER.error("Skipping dawnstone anvil augment display for " + stack, e);
			}
		}
		if (!leveledTools.isEmpty())
			visualRecipes.add(new AnvilDisplayRecipe(id, augmentedTools, leveledTools, input));
		return visualRecipes;
	}

	@Override
	public List<ItemStack> getDisplayInputBottom() {
		return List.of();
	}

	@Override
	public List<ItemStack> getDisplayInputTop() {
		return List.of();
	}

	@Override
	public List<ItemStack> getDisplayOutput() {
		return List.of();
	}

	public ResourceLocation getId() {
		return id;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}

	public static class Serializer extends LegacyRecipeSerializer<AnvilAugmentRecipe> {

		@Override
		public AnvilAugmentRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
			Ingredient tool = RecipeSerialization.readIngredient(json.get("tool"));
			Ingredient input = RecipeSerialization.readIngredient(json.get("input"));
			IAugment augment = AugmentUtil.getAugment(ResourceLocation.parse(GsonHelper.getAsString(json, "augment")));
			return new AnvilAugmentRecipe(recipeId, tool, input, augment);
		}

		@Override
		public @Nullable AnvilAugmentRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
			Ingredient tool = RecipeSerialization.readIngredient(buffer);
			Ingredient input = RecipeSerialization.readIngredient(buffer);
			IAugment augment = AugmentUtil.getAugment(buffer.readResourceLocation());
			return new AnvilAugmentRecipe(recipeId, tool, input, augment);
		}

		@Override
		public void toNetwork(FriendlyByteBuf buffer, AnvilAugmentRecipe recipe) {
			RecipeSerialization.writeIngredient(buffer, recipe.tool);
			RecipeSerialization.writeIngredient(buffer, recipe.input);
			buffer.writeResourceLocation(recipe.augment.getName());
		}
	}
}
