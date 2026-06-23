package com.rekindled.embers.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.rekindled.embers.Embers;
import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.recipe.IAlchemyRecipe;
import com.rekindled.embers.recipe.IBoilingRecipe;
import com.rekindled.embers.recipe.IBoringRecipe;
import com.rekindled.embers.recipe.ICatalysisCombustionRecipe;
import com.rekindled.embers.recipe.IDawnstoneAnvilRecipe;
import com.rekindled.embers.recipe.IEmberActivationRecipe;
import com.rekindled.embers.recipe.IGaseousFuelRecipe;
import com.rekindled.embers.recipe.IMeltingRecipe;
import com.rekindled.embers.recipe.IMetalCoefficientRecipe;
import com.rekindled.embers.recipe.IMixingRecipe;
import com.rekindled.embers.recipe.IStampingRecipe;
import com.rekindled.embers.recipe.IVisuallySplitRecipe;
import com.rekindled.embers.item.DynamicCrystalSeedBlockItem;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeInput;

@JeiPlugin
public class JEIPlugin implements IModPlugin {

	public static ResourceLocation pluginID = ResourceLocation.fromNamespaceAndPath(Embers.MODID, "jei_plugin");

	public static final RecipeType<IBoringRecipe> BORING = RecipeType.create(Embers.MODID, "boring", IBoringRecipe.class);
	public static final RecipeType<IBoringRecipe> EXCAVATION = RecipeType.create(Embers.MODID, "excavation", IBoringRecipe.class);
	public static final RecipeType<IEmberActivationRecipe> EMBER_ACTIVATION = RecipeType.create(Embers.MODID, "ember_activation", IEmberActivationRecipe.class);
	public static final RecipeType<IMeltingRecipe> MELTING = RecipeType.create(Embers.MODID, "melting", IMeltingRecipe.class);
	public static final RecipeType<IMeltingRecipe> MELTING_BONUS = RecipeType.create(Embers.MODID, "melting_bonus", IMeltingRecipe.class);
	public static final RecipeType<IStampingRecipe> STAMPING = RecipeType.create(Embers.MODID, "stamping", IStampingRecipe.class);
	public static final RecipeType<IMixingRecipe> MIXING = RecipeType.create(Embers.MODID, "mixing", IMixingRecipe.class);
	public static final RecipeType<IMetalCoefficientRecipe> METAL_COEFFICIENT = RecipeType.create(Embers.MODID, "metal_coefficient", IMetalCoefficientRecipe.class);
	public static final RecipeType<IAlchemyRecipe> ALCHEMY = RecipeType.create(Embers.MODID, "alchemy", IAlchemyRecipe.class);
	public static final RecipeType<IBoilingRecipe> BOILING = RecipeType.create(Embers.MODID, "boiling", IBoilingRecipe.class);
	public static final RecipeType<IGaseousFuelRecipe> GASEOUS_FUEL = RecipeType.create(Embers.MODID, "gaseous_fuel", IGaseousFuelRecipe.class);
	public static final RecipeType<ICatalysisCombustionRecipe> CATALYSIS_COMBUSTION = RecipeType.create(Embers.MODID, "catalysis_combustion", ICatalysisCombustionRecipe.class);
	public static final RecipeType<IDawnstoneAnvilRecipe> DAWNSTONE_ANVIL = RecipeType.create(Embers.MODID, "dawnstone_anvil", IDawnstoneAnvilRecipe.class);
	public static final RecipeType<WorldInteractionRecipe> WORLD_INTERACTION = RecipeType.create(Embers.MODID, "world_interaction", WorldInteractionRecipe.class);

	@Override
	public ResourceLocation getPluginUid() {
		return pluginID;
	}

	@Override
	public void registerItemSubtypes(ISubtypeRegistration registry) {
		registry.registerSubtypeInterpreter(RegistryManager.DYNAMIC_CRYSTAL_SEED_ITEM.get(), new ISubtypeInterpreter<ItemStack>() {
			@Override
			public Object getSubtypeData(ItemStack stack, UidContext context) {
				return DynamicCrystalSeedBlockItem.getMetal(stack);
			}

			@Override
			public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
				return DynamicCrystalSeedBlockItem.getMetal(stack);
			}
		});
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registry) {
		IGuiHelper guiHelper = registry.getJeiHelpers().getGuiHelper();

		registry.addRecipeCategories(new BoringCategory(guiHelper));
		registry.addRecipeCategories(new ExcavationCategory(guiHelper));
		registry.addRecipeCategories(new EmberActivationCategory(guiHelper));
		registry.addRecipeCategories(new MeltingCategory(guiHelper));
		registry.addRecipeCategories(new MeltingBonusCategory(guiHelper));
		registry.addRecipeCategories(new StampingCategory(guiHelper));
		registry.addRecipeCategories(new MixingCategory(guiHelper));
		registry.addRecipeCategories(new MetalCoefficientCategory(guiHelper));
		registry.addRecipeCategories(new AlchemyCategory(guiHelper));
		registry.addRecipeCategories(new BoilingCategory(guiHelper));
		registry.addRecipeCategories(new GaseousFuelCategory(guiHelper));
		registry.addRecipeCategories(new CatalysisCombustionCategory(guiHelper));
		registry.addRecipeCategories(new DawnstoneAnvilCategory(guiHelper));
		registry.addRecipeCategories(new WorldInteractionCategory(guiHelper));
	}

	@SuppressWarnings("unchecked")
	public static <I extends RecipeInput, T extends Recipe<I>> void addRecipes(IRecipeRegistration register, RecipeManager manager, RecipeType<T> jeiType, net.minecraft.world.item.crafting.RecipeType<T> type) {
		List<T> recipes = manager.getAllRecipesFor(type).stream().map(RecipeHolder::value).toList();
		List<T> visualRecipes = new ArrayList<T>();
		for (T recipe : recipes) {
			if (recipe instanceof IVisuallySplitRecipe) {
				visualRecipes.addAll(((IVisuallySplitRecipe<T>) recipe).getVisualRecipes());
			} else {
				visualRecipes.add(recipe);
			}
		}
		register.addRecipes(jeiType, visualRecipes);
	}

	@SuppressWarnings("resource")
	@Override
	public void registerRecipes(IRecipeRegistration register) {
		assert Minecraft.getInstance().level != null;
		RecipeManager manager = Minecraft.getInstance().level.getRecipeManager();

		addRecipes(register, manager, BORING, RegistryManager.BORING.get());

		addRecipes(register, manager, EXCAVATION, RegistryManager.EXCAVATION.get());

		addRecipes(register, manager, EMBER_ACTIVATION, RegistryManager.EMBER_ACTIVATION.get());

		List<IMeltingRecipe> meltingRecipes = manager.getAllRecipesFor(RegistryManager.MELTING.get()).stream().map(RecipeHolder::value).toList();
		register.addRecipes(MELTING, meltingRecipes);

		List<IMeltingRecipe> meltingBonusRecipes = new ArrayList<IMeltingRecipe>();
		for (IMeltingRecipe recipe : meltingRecipes) {
			if (!recipe.getBonus().isEmpty())
				meltingBonusRecipes.add(recipe);
		}
		register.addRecipes(MELTING_BONUS, meltingBonusRecipes);

		addRecipes(register, manager, STAMPING, RegistryManager.STAMPING.get());

		addRecipes(register, manager, MIXING, RegistryManager.MIXING.get());

		addRecipes(register, manager, METAL_COEFFICIENT, RegistryManager.METAL_COEFFICIENT.get());

		addRecipes(register, manager, ALCHEMY, RegistryManager.ALCHEMY.get());

		addRecipes(register, manager, BOILING, RegistryManager.BOILING.get());

		addRecipes(register, manager, GASEOUS_FUEL, RegistryManager.GASEOUS_FUEL.get());

		addRecipes(register, manager, CATALYSIS_COMBUSTION, RegistryManager.CATALYSIS_COMBUSTION.get());

		addRecipes(register, manager, DAWNSTONE_ANVIL, RegistryManager.DAWNSTONE_ANVIL_RECIPE.get());

		register.addRecipes(WORLD_INTERACTION, List.of(WorldInteractionRecipe.solidifiedMetal()));
	}


	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registry) {
		registry.addRecipeCatalyst(new ItemStack(RegistryManager.EMBER_BORE_ITEM.get()), BORING);
		registry.addRecipeCatalyst(new ItemStack(RegistryManager.EMBER_BORE_ITEM.get()), EXCAVATION);
		registry.addRecipeCatalyst(new ItemStack(RegistryManager.EXCAVATION_BUCKETS_ITEM.get()), EXCAVATION);
		registry.addRecipeCatalyst(new ItemStack(RegistryManager.EMBER_ACTIVATOR_ITEM.get()), EMBER_ACTIVATION);
		registry.addRecipeCatalyst(new ItemStack(RegistryManager.PRESSURE_REFINERY_ITEM.get()), EMBER_ACTIVATION);
		registry.addRecipeCatalyst(new ItemStack(RegistryManager.IGNEM_REACTOR_ITEM.get()), EMBER_ACTIVATION);
		registry.addRecipeCatalyst(new ItemStack(RegistryManager.MELTER_ITEM.get()), MELTING, MELTING_BONUS);
		registry.addRecipeCatalyst(new ItemStack(RegistryManager.GEOLOGIC_SEPARATOR_ITEM.get()), MELTING_BONUS);
		registry.addRecipeCatalyst(new ItemStack(RegistryManager.STAMPER_ITEM.get()), STAMPING);
		registry.addRecipeCatalyst(new ItemStack(RegistryManager.STAMP_BASE_ITEM.get()), STAMPING);
		registry.addRecipeCatalyst(new ItemStack(RegistryManager.MIXER_CENTRIFUGE_ITEM.get()), MIXING);
		registry.addRecipeCatalyst(new ItemStack(RegistryManager.PRESSURE_REFINERY_ITEM.get()), METAL_COEFFICIENT);
		registry.addRecipeCatalyst(new ItemStack(RegistryManager.HEARTH_COIL_ITEM.get()), RecipeTypes.SMELTING);
		registry.addRecipeCatalyst(new ItemStack(RegistryManager.HEARTH_COIL_ITEM.get()), RecipeTypes.SMOKING);
		registry.addRecipeCatalyst(new ItemStack(RegistryManager.CHAR_INSTILLER.get()), RecipeTypes.SMOKING);
		registry.addRecipeCatalyst(new ItemStack(RegistryManager.HEARTH_COIL_ITEM.get()), RecipeTypes.BLASTING);
		registry.addRecipeCatalyst(new ItemStack(RegistryManager.ATMOSPHERIC_BELLOWS.get()), RecipeTypes.BLASTING);
		registry.addRecipeCatalyst(new ItemStack(RegistryManager.ALCHEMY_PEDESTAL_ITEM.get()), ALCHEMY);
		registry.addRecipeCatalyst(new ItemStack(RegistryManager.ALCHEMY_TABLET_ITEM.get()), ALCHEMY);
		registry.addRecipeCatalyst(new ItemStack(RegistryManager.BEAM_CANNON_ITEM.get()), ALCHEMY);
		registry.addRecipeCatalyst(new ItemStack(RegistryManager.MINI_BOILER_ITEM.get()), BOILING);
		registry.addRecipeCatalyst(new ItemStack(RegistryManager.CATALYTIC_PLUG_ITEM.get()), GASEOUS_FUEL);
		registry.addRecipeCatalyst(new ItemStack(RegistryManager.WILDFIRE_STIRLING_ITEM.get()), GASEOUS_FUEL);
		registry.addRecipeCatalyst(new ItemStack(RegistryManager.TINKER_HAMMER.get()), DAWNSTONE_ANVIL);
		registry.addRecipeCatalyst(new ItemStack(RegistryManager.DAWNSTONE_ANVIL_ITEM.get()), DAWNSTONE_ANVIL);
		registry.addRecipeCatalyst(new ItemStack(RegistryManager.AUTOMATIC_HAMMER_ITEM.get()), DAWNSTONE_ANVIL);
	}
}
