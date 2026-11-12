package com.rekindled.embers.compat.emi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.rekindled.embers.Embers;
import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.compat.WorldInteractionRecipe;
import com.rekindled.embers.recipe.IVisuallySplitRecipe;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiWorldInteractionRecipe;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;

@EmiEntrypoint
public class EMIPlugin implements EmiPlugin {

	public static final ResourceLocation ALCHEMY_PILLAR_TEXTURE = EmbersEmiCategory.guiTexture("jei_alchemy.png");

	public static final EmbersEmiCategory BORING = new EmbersEmiCategory("boring", RegistryManager.EMBER_BORE_ITEM.get(), title("boring"), EmbersEmiCategory.guiTexture("jei_boring.png"), 0, 0, 126, 98);
	public static final EmbersEmiCategory EXCAVATION = new EmbersEmiCategory("excavation", RegistryManager.EXCAVATION_BUCKETS_ITEM.get(), title("excavation"), EmbersEmiCategory.guiTexture("jei_boring.png"), 0, 0, 126, 98);
	public static final EmbersEmiCategory EMBER_ACTIVATION = new EmbersEmiCategory("ember_activation", RegistryManager.EMBER_ACTIVATOR_ITEM.get(), title("ember_activation"), EmbersEmiCategory.guiTexture("jei_item_text.png"), 0, 0, 126, 28);
	public static final EmbersEmiCategory MELTING = new EmbersEmiCategory("melting", RegistryManager.MELTER_ITEM.get(), title("melting"), EmbersEmiCategory.guiTexture("jei_melter.png"), 0, 14, 108, 42);
	public static final EmbersEmiCategory MELTING_BONUS = new EmbersEmiCategory("melting_bonus", RegistryManager.GEOLOGIC_SEPARATOR_ITEM.get(), title("geologic_separator"), EmbersEmiCategory.guiTexture("jei_geologic_separator.png"), 0, 14, 108, 42);
	public static final EmbersEmiCategory STAMPING = new EmbersEmiCategory("stamping", RegistryManager.STAMPER_ITEM.get(), title("stamping"), EmbersEmiCategory.guiTexture("jei_stamp.png"), 0, 0, 108, 83);
	public static final EmbersEmiCategory MIXING = new EmbersEmiCategory("mixing", RegistryManager.MIXER_CENTRIFUGE_ITEM.get(), title("mixing"), EmbersEmiCategory.guiTexture("jei_mixer.png"), 0, 0, 108, 124);
	public static final EmbersEmiCategory METAL_COEFFICIENT = new EmbersEmiCategory("metal_coefficient", RegistryManager.PRESSURE_REFINERY_ITEM.get(), title("metal_coefficient"), EmbersEmiCategory.guiTexture("jei_item_text.png"), 0, 0, 126, 28);
	public static final EmbersEmiCategory ALCHEMY = new EmbersEmiCategory("alchemy", RegistryManager.ALCHEMY_TABLET_ITEM.get(), title("alchemy"), EmbersEmiCategory.guiTexture("jei_alchemy.png"), 0, 0, 126, 108);
	public static final EmbersEmiCategory BOILING = new EmbersEmiCategory("boiling", RegistryManager.MINI_BOILER_ITEM.get(), title("boiling"), EmbersEmiCategory.guiTexture("jei_boiler.png"), 0, 0, 126, 28);
	public static final EmbersEmiCategory GASEOUS_FUEL = new EmbersEmiCategory("gaseous_fuel", RegistryManager.WILDFIRE_STIRLING_ITEM.get(), title("gaseous_fuel"), EmbersEmiCategory.guiTexture("jei_gaseous_fuel.png"), 0, 0, 126, 31);
	public static final EmbersEmiCategory CATALYSIS_COMBUSTION = new EmbersEmiCategory("catalysis_combustion", RegistryManager.IGNEM_REACTOR_ITEM.get(), title("catalysis_combustion"), EmbersEmiCategory.guiTexture("jei_catalysis_combustion.png"), 0, 0, 126, 46);
	public static final EmbersEmiCategory DAWNSTONE_ANVIL = new EmbersEmiCategory("dawnstone_anvil", RegistryManager.DAWNSTONE_ANVIL_ITEM.get(), title("dawnstone_anvil"), EmbersEmiCategory.guiTexture("jei_dawnstone_anvil.png"), 0, 0, 110, 58);

	private static Component title(String key) {
		return Component.translatable(Embers.MODID + ".jei.recipe." + key);
	}

	@Override
	public void register(EmiRegistry registry) {
		// EMI counterpart of JEIPlugin#registerItemSubtypes, dynamic crystal seeds differ by their stored metal
		registry.setDefaultComparison(RegistryManager.DYNAMIC_CRYSTAL_SEED_ITEM.get(), Comparison.compareComponents());

		registry.addCategory(BORING);
		registry.addCategory(EXCAVATION);
		registry.addCategory(EMBER_ACTIVATION);
		registry.addCategory(MELTING);
		registry.addCategory(MELTING_BONUS);
		registry.addCategory(STAMPING);
		registry.addCategory(MIXING);
		registry.addCategory(METAL_COEFFICIENT);
		registry.addCategory(ALCHEMY);
		registry.addCategory(BOILING);
		registry.addCategory(GASEOUS_FUEL);
		registry.addCategory(CATALYSIS_COMBUSTION);
		registry.addCategory(DAWNSTONE_ANVIL);

		addRecipes(registry, RegistryManager.BORING.get(), BORING, BoringEmiRecipe::new);
		addRecipes(registry, RegistryManager.EXCAVATION.get(), EXCAVATION, BoringEmiRecipe::new);
		addRecipes(registry, RegistryManager.EMBER_ACTIVATION.get(), EMBER_ACTIVATION, EmberActivationEmiRecipe::new);
		addRecipes(registry, RegistryManager.STAMPING.get(), STAMPING, StampingEmiRecipe::new);
		addRecipes(registry, RegistryManager.MIXING.get(), MIXING, MixingEmiRecipe::new);
		addRecipes(registry, RegistryManager.METAL_COEFFICIENT.get(), METAL_COEFFICIENT, MetalCoefficientEmiRecipe::new);
		addRecipes(registry, RegistryManager.ALCHEMY.get(), ALCHEMY, AlchemyEmiRecipe::new);
		addRecipes(registry, RegistryManager.BOILING.get(), BOILING, BoilingEmiRecipe::new);
		addRecipes(registry, RegistryManager.GASEOUS_FUEL.get(), GASEOUS_FUEL, GaseousFuelEmiRecipe::new);
		addRecipes(registry, RegistryManager.CATALYSIS_COMBUSTION.get(), CATALYSIS_COMBUSTION, CatalysisCombustionEmiRecipe::new);
		addRecipes(registry, RegistryManager.DAWNSTONE_ANVIL_RECIPE.get(), DAWNSTONE_ANVIL, DawnstoneAnvilEmiRecipe::new);

		forEachHolder(registry, RegistryManager.MELTING.get(), holder -> {
			registry.addRecipe(new MeltingEmiRecipe(MELTING, holder, false));
			if (!holder.value().getBonus().isEmpty())
				registry.addRecipe(new MeltingEmiRecipe(MELTING_BONUS, withId(holder, "melting_bonus"), true));
		});

		// reused rather than duplicated: EMI already ships a world interaction category for this kind of recipe
		try {
			WorldInteractionRecipe worldInteraction = WorldInteractionRecipe.solidifiedMetal();
			registry.addRecipe(EmiWorldInteractionRecipe.builder()
					.id(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "/world_interaction/solidified_metal"))
					.leftInput(EmiIngredients.fluids(worldInteraction.primaryFluids()))
					.rightInput(EmiIngredients.fluid(worldInteraction.secondaryFluid()), false)
					.output(EmiStack.of(worldInteraction.output()))
					.build());
		} catch (Throwable t) {
			Embers.LOGGER.error("Could not register the solidified metal world interaction recipe with EMI", t);
		}

		addWorkstations(registry);
	}

	private static void addWorkstations(EmiRegistry registry) {
		workstation(registry, BORING, RegistryManager.EMBER_BORE_ITEM.get());
		workstation(registry, EXCAVATION, RegistryManager.EMBER_BORE_ITEM.get());
		workstation(registry, EXCAVATION, RegistryManager.EXCAVATION_BUCKETS_ITEM.get());
		workstation(registry, EMBER_ACTIVATION, RegistryManager.EMBER_ACTIVATOR_ITEM.get());
		workstation(registry, EMBER_ACTIVATION, RegistryManager.PRESSURE_REFINERY_ITEM.get());
		workstation(registry, EMBER_ACTIVATION, RegistryManager.IGNEM_REACTOR_ITEM.get());
		workstation(registry, MELTING, RegistryManager.MELTER_ITEM.get());
		workstation(registry, MELTING_BONUS, RegistryManager.MELTER_ITEM.get());
		workstation(registry, MELTING_BONUS, RegistryManager.GEOLOGIC_SEPARATOR_ITEM.get());
		workstation(registry, STAMPING, RegistryManager.STAMPER_ITEM.get());
		workstation(registry, STAMPING, RegistryManager.STAMP_BASE_ITEM.get());
		workstation(registry, MIXING, RegistryManager.MIXER_CENTRIFUGE_ITEM.get());
		workstation(registry, METAL_COEFFICIENT, RegistryManager.PRESSURE_REFINERY_ITEM.get());
		workstation(registry, ALCHEMY, RegistryManager.ALCHEMY_PEDESTAL_ITEM.get());
		workstation(registry, ALCHEMY, RegistryManager.ALCHEMY_TABLET_ITEM.get());
		workstation(registry, ALCHEMY, RegistryManager.BEAM_CANNON_ITEM.get());
		workstation(registry, BOILING, RegistryManager.MINI_BOILER_ITEM.get());
		workstation(registry, GASEOUS_FUEL, RegistryManager.CATALYTIC_PLUG_ITEM.get());
		workstation(registry, GASEOUS_FUEL, RegistryManager.WILDFIRE_STIRLING_ITEM.get());
		workstation(registry, DAWNSTONE_ANVIL, RegistryManager.TINKER_HAMMER.get());
		workstation(registry, DAWNSTONE_ANVIL, RegistryManager.DAWNSTONE_ANVIL_ITEM.get());
		workstation(registry, DAWNSTONE_ANVIL, RegistryManager.AUTOMATIC_HAMMER_ITEM.get());
	}

	private static void workstation(EmiRegistry registry, EmbersEmiCategory category, net.minecraft.world.level.ItemLike item) {
		try {
			registry.addWorkstation(category, EmiStack.of(item));
		} catch (Throwable t) {
			Embers.LOGGER.error("Could not register an EMI workstation for " + category.getId(), t);
		}
	}

	private static <I extends RecipeInput, T extends Recipe<I>> void addRecipes(EmiRegistry registry, RecipeType<T> type, EmbersEmiCategory category,
			BiFunction<EmbersEmiCategory, RecipeHolder<T>, EmiRecipe> factory) {
		forEachHolder(registry, type, holder -> registry.addRecipe(factory.apply(category, holder)));
	}

	/**
	 * Feeds every recipe of a type to the action. One recipe that fails to read or display is logged and
	 * skipped instead of aborting the whole plugin, which would cost every Embers category.
	 */
	private static <I extends RecipeInput, T extends Recipe<I>> void forEachHolder(EmiRegistry registry, RecipeType<T> type, Consumer<RecipeHolder<T>> action) {
		List<RecipeHolder<T>> holders;
		try {
			holders = registry.getRecipeManager().getAllRecipesFor(type);
		} catch (Throwable t) {
			Embers.LOGGER.error("Could not read recipes of type " + type + " for EMI", t);
			return;
		}
		for (RecipeHolder<T> holder : visualHolders(holders)) {
			try {
				action.accept(holder);
			} catch (Throwable t) {
				Embers.LOGGER.error("Skipping EMI recipe " + holder.id(), t);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends Recipe<?>> List<RecipeHolder<T>> visualHolders(List<RecipeHolder<T>> holders) {
		List<RecipeHolder<T>> recipes = new ArrayList<>();
		for (RecipeHolder<T> holder : holders) {
			if (holder.value() instanceof IVisuallySplitRecipe<?> splitRecipe) {
				List<T> visualRecipes;
				try {
					visualRecipes = (List<T>) (List<?>) splitRecipe.getVisualRecipes();
				} catch (Throwable t) {
					Embers.LOGGER.error("Could not split the visual recipes of " + holder.id() + ", showing it whole", t);
					recipes.add(holder);
					continue;
				}
				for (int index = 0; index < visualRecipes.size(); index++)
					recipes.add(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(holder.id().getNamespace(), "/" + holder.id().getPath() + "/display_" + index), visualRecipes.get(index)));
			} else {
				recipes.add(holder);
			}
		}
		return recipes;
	}

	/** EMI keys recipes by id, so a recipe shown in two categories needs a synthetic id in one of them. */
	private static <T extends Recipe<?>> RecipeHolder<T> withId(RecipeHolder<T> holder, String prefix) {
		return new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(holder.id().getNamespace(), "/" + prefix + "/" + holder.id().getPath()), holder.value());
	}
}
