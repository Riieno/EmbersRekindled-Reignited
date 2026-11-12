package com.rekindled.embers.compat.emi;

import java.util.ArrayList;
import java.util.List;

import com.rekindled.embers.ConfigManager;
import com.rekindled.embers.recipe.IAlchemyRecipe;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;

/** EMI port of the AlchemyCategory. */
public class AlchemyEmiRecipe implements EmiRecipe {

	private static final int VERTICAL_MARGIN = 2;
	private static final int PALETTE_Y = 96;

	private final EmbersEmiCategory category;
	private final ResourceLocation id;
	private final IAlchemyRecipe recipe;
	private final List<EmiIngredient> inputs = new ArrayList<>();
	private final List<EmiIngredient> catalysts = new ArrayList<>();
	// aspect ingredient for each ring input, index aligned with inputs
	private final List<EmiIngredient> ringAspects = new ArrayList<>();
	private final List<EmiIngredient> aspectPalette = new ArrayList<>();

	public AlchemyEmiRecipe(EmbersEmiCategory category, RecipeHolder<IAlchemyRecipe> holder) {
		this.category = category;
		this.id = holder.id();
		this.recipe = holder.value();

		int aspecti = recipe.getAspects().size();
		int inputs = recipe.getInputs().size();
		boolean randomizeAspectus = ConfigManager.randomizeAlchemyAspectus();
		List<Ingredient> exactAspectus = randomizeAspectus ? List.of() : recipe.getCode(0L);

		Ingredient[][] aspectusCombinations = new Ingredient[0][0];
		if (randomizeAspectus) {
			aspectusCombinations = new Ingredient[inputs][(int) Math.pow(aspecti, inputs)];
			for (int i = 0; i < inputs; i++) {
				for (int j = 0; j < ((int) Math.pow(aspecti, inputs)); j++) {
					aspectusCombinations[i][j] = recipe.getAspects().get((j / ((int) Math.pow(aspecti, i))) % aspecti);
				}
			}
		}

		this.inputs.add(EmiIngredients.ingredient(recipe.getCenterInput()));
		for (int i = 0; i < inputs; i++) {
			this.inputs.add(EmiIngredients.ingredient(recipe.getInputs().get(i)));
			Ingredient aspectus = randomizeAspectus ? CompoundIngredient.of(aspectusCombinations[i]) : exactAspectus.get(i);
			this.ringAspects.add(EmiIngredients.ingredient(aspectus));
		}
		this.catalysts.addAll(this.ringAspects);
		if (randomizeAspectus) {
			for (int i = 0; i < aspecti; i++)
				this.aspectPalette.add(EmiIngredients.ingredient(recipe.getAspects().get(i)));
			this.catalysts.addAll(this.aspectPalette);
		}
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
		return inputs;
	}

	@Override
	public List<EmiIngredient> getCatalysts() {
		return catalysts;
	}

	@Override
	public List<EmiStack> getOutputs() {
		return List.of(EmiStack.of(recipe.getResultItem()));
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
	public void addWidgets(WidgetHolder widgets) {
		int yOffset = VERTICAL_MARGIN;
		category.drawBackground(widgets, yOffset);
		EmiIngredients.addSlot(widgets, inputs.get(0), 32, 37 + yOffset);
		EmiIngredients.addSlot(widgets, getOutputs().get(0), 101, 37 + yOffset).recipeContext(this);

		Vec3 center = new Vec3(0, 30, 0);
		int ringInputs = ringAspects.size();
		for (int i = 0; i < ringInputs; i++) {
			Vec3 rotated = center.zRot((float) (i * 2.0 * Math.PI / ringInputs));
			int x = (int) (32 + rotated.x());
			int y = (int) (29 + rotated.y() + yOffset);
			EmiIngredients.addSlot(widgets, inputs.get(i + 1), x, y);
			widgets.addTexture(EMIPlugin.ALCHEMY_PILLAR_TEXTURE, x, (int) (45 + rotated.y() + yOffset), 16, 16, 126, 0);
			EmiIngredients.addSlot(widgets, ringAspects.get(i), x, (int) (45 + rotated.y() + yOffset)).drawBack(false);
		}
		for (int i = 0; i < aspectPalette.size(); i++)
			EmiIngredients.addSlot(widgets, aspectPalette.get(i), 63 - 8 * aspectPalette.size() + 16 * i, PALETTE_Y + yOffset);
	}
}
