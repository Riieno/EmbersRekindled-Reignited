package com.rekindled.embers.compat.emi;

import com.rekindled.embers.Embers;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;

/**
 * EMI counterpart to a JEI {@link mezz.jei.api.recipe.category.IRecipeCategory}.
 * Keeps the JEI background texture and title so both viewers look the same.
 */
public class EmbersEmiCategory extends EmiRecipeCategory {

	private final Component name;
	private final ResourceLocation texture;
	private final int textureU;
	private final int textureV;
	private final int width;
	private final int height;

	public EmbersEmiCategory(String path, ItemLike icon, Component name, ResourceLocation texture, int u, int v, int width, int height) {
		super(ResourceLocation.fromNamespaceAndPath(Embers.MODID, path), EmiIngredients.item(icon));
		this.name = name;
		this.texture = texture;
		this.textureU = u;
		this.textureV = v;
		this.width = width;
		this.height = height;
	}

	/** Category without a background texture, EMI draws its own slot backgrounds. */
	public EmbersEmiCategory(String path, ItemLike icon, Component name, int width, int height) {
		this(path, icon, name, null, 0, 0, width, height);
	}

	@Override
	public Component getName() {
		return name;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public void drawBackground(WidgetHolder widgets) {
		drawBackground(widgets, 0);
	}

	public void drawBackground(WidgetHolder widgets, int yOffset) {
		if (texture != null)
			widgets.addTexture(texture, 0, yOffset, width, height, textureU, textureV);
	}

	public static ResourceLocation guiTexture(String name) {
		return ResourceLocation.fromNamespaceAndPath(Embers.MODID, "textures/gui/" + name);
	}
}
