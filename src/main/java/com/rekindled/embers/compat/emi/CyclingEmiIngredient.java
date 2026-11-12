package com.rekindled.embers.compat.emi;

import java.util.List;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

/**
 * An ingredient that steps through its stacks over time. EmiIngredient.of() folds a stack list into a tag
 * ingredient as soon as common item tags cover it, and a tag ingredient only ever shows its first stack, which
 * hides everything past the first entry of a long list such as the tools an augment applies to.
 */
public class CyclingEmiIngredient implements EmiIngredient {

	private final List<EmiStack> stacks;
	private long amount;
	private float chance = 1;

	public CyclingEmiIngredient(List<EmiStack> stacks, long amount) {
		this.stacks = stacks;
		this.amount = amount;
	}

	@Override
	public List<EmiStack> getEmiStacks() {
		return stacks;
	}

	@Override
	public long getAmount() {
		return amount;
	}

	@Override
	public EmiIngredient setAmount(long amount) {
		this.amount = amount;
		return this;
	}

	@Override
	public float getChance() {
		return chance;
	}

	@Override
	public EmiIngredient setChance(float chance) {
		this.chance = chance;
		return this;
	}

	@Override
	public void render(GuiGraphics draw, int x, int y, float delta, int flags) {
		current().render(draw, x, y, delta, flags);
	}

	@Override
	public EmiIngredient copy() {
		return new CyclingEmiIngredient(stacks, amount);
	}

	@Override
	public List<ClientTooltipComponent> getTooltip() {
		return current().getTooltip();
	}

	private EmiStack current() {
		return stacks.get((int) (System.currentTimeMillis() / 1000 % stacks.size()));
	}
}
