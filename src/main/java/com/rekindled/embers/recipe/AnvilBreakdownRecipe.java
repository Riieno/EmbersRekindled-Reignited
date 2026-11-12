package com.rekindled.embers.recipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.rekindled.embers.Embers;
import com.rekindled.embers.api.augment.AugmentUtil;
import com.rekindled.embers.datagen.EmbersItemTags;
import com.rekindled.embers.util.Misc;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class AnvilBreakdownRecipe implements IDawnstoneAnvilRecipe, IVisuallySplitRecipe<IDawnstoneAnvilRecipe> {

	public static final Serializer SERIALIZER = new Serializer(); 

	public final ResourceLocation id;

	public static List<IDawnstoneAnvilRecipe> visualRecipes = new ArrayList<IDawnstoneAnvilRecipe>();
	Ingredient blacklist = Ingredient.of(EmbersItemTags.BREAKDOWN_BLACKLIST);

	public AnvilBreakdownRecipe(ResourceLocation id) {
		this.id = id;
	}

	@Override
	public boolean matches(RecipeInput context, Level pLevel) {
		ItemStack tool = context.getItem(0);
		return tool.isDamageableItem() && !blacklist.test(tool) && context.getItem(1).isEmpty() && !AugmentUtil.hasHeat(tool) && !Misc.getRepairIngredient(tool.getItem()).isEmpty();
	}

	@Override
	public List<ItemStack> getOutput(RecipeInput context) {
		Ingredient repairMaterial = Misc.getRepairIngredient(context.getItem(0).getItem());
		if (!repairMaterial.isEmpty())
			return List.of(Misc.getPreferredItem(repairMaterial.getItems()));
		return List.of();
	}

	@Override
	public List<IDawnstoneAnvilRecipe> getVisualRecipes() {
		visualRecipes.clear();
		// grouped by the material that comes back out, so the tool slot cycles instead of one entry per item
		Map<String, List<ItemStack>> brokenToolsByOutput = new LinkedHashMap<>();
		Map<String, ItemStack> outputs = new LinkedHashMap<>();
		for (Holder<Item> holder : BuiltInRegistries.ITEM.asHolderIdMap()) {
			try {
				ItemStack toolStack = new ItemStack(holder.value());
				if (!toolStack.isDamageableItem() || blacklist.test(toolStack))
					continue;
				Ingredient repairMaterial = Misc.getRepairIngredient(holder.value());
				if (repairMaterial.isEmpty())
					continue;
				ItemStack brokenTool = toolStack.copy();
				brokenTool.setDamageValue(brokenTool.getMaxDamage() / 2);
				ItemStack output = Misc.getPreferredItem(repairMaterial.getItems());
				String key = BuiltInRegistries.ITEM.getKey(output.getItem()).toString();
				outputs.putIfAbsent(key, output);
				brokenToolsByOutput.computeIfAbsent(key, k -> new ArrayList<>()).add(brokenTool);
			} catch (Exception e) {
				// a single misbehaving item must not cost every other breakdown display
				Embers.LOGGER.error("Skipping dawnstone anvil breakdown display for " + holder.value(), e);
			}
		}
		for (Map.Entry<String, List<ItemStack>> entry : brokenToolsByOutput.entrySet())
			visualRecipes.add(new AnvilDisplayRecipe(id, List.of(outputs.get(entry.getKey())), entry.getValue(), Ingredient.EMPTY));
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

	public static class Serializer extends LegacyRecipeSerializer<AnvilBreakdownRecipe> {

		@Override
		public AnvilBreakdownRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
			return new AnvilBreakdownRecipe(recipeId);
		}

		@Override
		public @Nullable AnvilBreakdownRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
			return new AnvilBreakdownRecipe(recipeId);
		}

		@Override
		public void toNetwork(FriendlyByteBuf buffer, AnvilBreakdownRecipe recipe) {
		}
	}
}
