package com.rekindled.embers.api.block;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

public interface IHammerInteraction {
	InteractionResult onHammerUse(ItemStack hammer, UseOnContext context);
}
