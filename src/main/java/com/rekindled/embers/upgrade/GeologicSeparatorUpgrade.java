package com.rekindled.embers.upgrade;

import java.util.List;

import com.rekindled.embers.Embers;
import com.rekindled.embers.api.event.MachineRecipeEvent;
import com.rekindled.embers.api.event.UpgradeEvent;
import com.rekindled.embers.api.upgrades.UpgradeContext;
import com.rekindled.embers.blockentity.GeologicSeparatorBlockEntity;
import com.rekindled.embers.recipe.MeltingRecipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.rekindled.embers.compat.legacy.capabilities.ForgeCapabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public class GeologicSeparatorUpgrade extends DefaultUpgradeProvider {

	public GeologicSeparatorUpgrade(BlockEntity tile) {
		super(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "geologic_separator"), tile);
	}

	@Override
	public int getPriority() {
		return 100; //after everything else
	}

	@Override
	public void throwEvent(BlockEntity tile, List<UpgradeContext> upgrades, UpgradeEvent event, int distance, int count) {
		if (distance <= 0 && event instanceof MachineRecipeEvent.Success) {
			Object recipe = ((MachineRecipeEvent<?>) event).getRecipe();
			if (recipe instanceof MeltingRecipe) {
				FluidStack bonus = ((MeltingRecipe) recipe).getBonus();
				if (!bonus.isEmpty() && this.tile instanceof GeologicSeparatorBlockEntity) {
					if (findTarget(upgrades, bonus) != this)
						return;
					fill(this, bonus, FluidAction.EXECUTE);
				}
			}
		}
	}

	public static int fillOutput(List<UpgradeContext> upgrades, FluidStack output, FluidAction action) {
		GeologicSeparatorUpgrade target = findTarget(upgrades, output);
		if (target == null)
			return 0;
		return fill(target, output, action);
	}

	private static GeologicSeparatorUpgrade findTarget(List<UpgradeContext> upgrades, FluidStack stack) {
		GeologicSeparatorUpgrade emptyTarget = null;
		for (UpgradeContext context : upgrades) {
			if (context.distance() > 0 || !(context.upgrade() instanceof GeologicSeparatorUpgrade separator))
				continue;
			IFluidHandler fluidHandler = getFluidHandler(separator);
			if (fluidHandler == null || fluidHandler.fill(stack.copy(), FluidAction.SIMULATE) < stack.getAmount())
				continue;
			FluidStack contained = fluidHandler.getFluidInTank(0);
			if (!contained.isEmpty() && FluidStack.isSameFluidSameComponents(contained, stack))
				return separator;
			if (contained.isEmpty() && emptyTarget == null)
				emptyTarget = separator;
		}
		return emptyTarget;
	}

	private static int fill(GeologicSeparatorUpgrade separator, FluidStack stack, FluidAction action) {
		IFluidHandler fluidHandler = getFluidHandler(separator);
		if (fluidHandler == null)
			return 0;
		return fluidHandler.fill(stack.copy(), action);
	}

	private static IFluidHandler getFluidHandler(GeologicSeparatorUpgrade separator) {
		return com.rekindled.embers.util.CapabilityCompat.getCapability(separator.tile, ForgeCapabilities.FLUID_HANDLER, null).orElse(null);
	}
}
