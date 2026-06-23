package com.rekindled.embers.compat.jei;

import java.util.List;

import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.fluidtypes.MoltenMetalFluidType;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

public record WorldInteractionRecipe(List<FluidStack> primaryFluids, FluidStack secondaryFluid, ItemStack output) {

	public static WorldInteractionRecipe solidifiedMetal() {
		List<FluidStack> moltenMetals = RegistryManager.fluidList.stream()
				.filter(fluid -> fluid.TYPE.get() instanceof MoltenMetalFluidType)
				.map(fluid -> new FluidStack(fluid.FLUID.get(), FluidType.BUCKET_VOLUME))
				.toList();
		return new WorldInteractionRecipe(
				moltenMetals,
				new FluidStack(Fluids.WATER, FluidType.BUCKET_VOLUME),
				new ItemStack(RegistryManager.SOLIDIFIED_METAL_ITEM.get()));
	}
}
