package com.rekindled.embers.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.rekindled.embers.Embers;
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

public class AnvilRepairRecipe implements IDawnstoneAnvilRecipe, IVisuallySplitRecipe<IDawnstoneAnvilRecipe> {

	public static final Serializer SERIALIZER = new Serializer(); 

	public final ResourceLocation id;

	public static List<IDawnstoneAnvilRecipe> visualRecipes = new ArrayList<IDawnstoneAnvilRecipe>();
	Ingredient blacklist = Ingredient.of(EmbersItemTags.REPAIR_BLACKLIST);

	public AnvilRepairRecipe(ResourceLocation id) {
		this.id = id;
	}

	@Override
	public boolean matches(RecipeInput context, Level pLevel) {
		ItemStack tool = context.getItem(0);
		return tool.isDamageableItem() && tool.isDamaged() && !blacklist.test(tool) && tool.getItem().isValidRepairItem(tool, context.getItem(1));
	}

	@Override
	public List<ItemStack> getOutput(RecipeInput context) {
		ItemStack result = context.getItem(0).copy();
		result.setDamageValue(Math.max(0, result.getDamageValue() - result.getMaxDamage()));
		return List.of(result);
	}

	@Override
	public List<IDawnstoneAnvilRecipe> getVisualRecipes() {
		visualRecipes.clear();
		// one display recipe per repair material, with the tool slots cycling through the group,
		// instead of one entry per repairable item
		Map<String, RepairGroup> groups = new LinkedHashMap<>();
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
				groups.computeIfAbsent(materialKey(repairMaterial), key -> new RepairGroup(repairMaterial))
						.add(toolStack, brokenTool);
			} catch (Exception e) {
				// a single misbehaving item must not cost every other repair recipe
				Embers.LOGGER.error("Skipping dawnstone anvil repair display for " + holder.value(), e);
			}
		}
		for (RepairGroup group : groups.values())
			visualRecipes.add(new AnvilDisplayRecipe(id, group.tools, group.brokenTools, group.material));
		return visualRecipes;
	}

	/** Items repaired by the same thing share a display, so they are grouped by what repairs them. */
	private static String materialKey(Ingredient ingredient) {
		List<String> items = new ArrayList<>();
		for (ItemStack stack : ingredient.getItems())
			items.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
		Collections.sort(items);
		return String.join(",", items);
	}

	private static class RepairGroup {
		final Ingredient material;
		final List<ItemStack> tools = new ArrayList<>();
		final List<ItemStack> brokenTools = new ArrayList<>();

		RepairGroup(Ingredient material) {
			this.material = material;
		}

		void add(ItemStack tool, ItemStack brokenTool) {
			tools.add(tool);
			brokenTools.add(brokenTool);
		}
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

	public static class Serializer extends LegacyRecipeSerializer<AnvilRepairRecipe> {

		@Override
		public AnvilRepairRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
			return new AnvilRepairRecipe(recipeId);
		}

		@Override
		public @Nullable AnvilRepairRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
			return new AnvilRepairRecipe(recipeId);
		}

		@Override
		public void toNetwork(FriendlyByteBuf buffer, AnvilRepairRecipe recipe) {
		}
	}
}
