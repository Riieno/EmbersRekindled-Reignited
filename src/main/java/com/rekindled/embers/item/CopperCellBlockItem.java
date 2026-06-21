package com.rekindled.embers.item;

import java.text.DecimalFormat;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.rekindled.embers.Embers;
import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.api.power.IEmberCapability;
import com.rekindled.embers.power.DefaultEmberItemCapability;
import com.rekindled.embers.util.DecimalFormats;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.rekindled.embers.compat.legacy.capabilities.ICapabilityProvider;

public class CopperCellBlockItem extends BlockItem {

	public CopperCellBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		ensureBlockEntityId(stack);
		return com.rekindled.embers.util.CapabilityCompat.getCapability(stack, EmbersCapabilities.EMBER_CAPABILITY).isPresent();
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		ensureBlockEntityId(stack);
		IEmberCapability cap = com.rekindled.embers.util.CapabilityCompat.getCapability(stack, EmbersCapabilities.EMBER_CAPABILITY).orElse(null);
		if (cap != null) {
			return Math.round(13.0F - (float) (cap.getEmberCapacity()-cap.getEmber()) * 13.0F / (float) cap.getEmberCapacity());
		}
		return 0;
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
	}

	@Override
	public int getBarColor(ItemStack pStack) {
		return 0xFF6600;
	}

	public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
		ensureBlockEntityId(stack);
		return new DefaultEmberItemCapability(stack, 24000);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		ensureBlockEntityId(stack);
		super.inventoryTick(stack, level, entity, slot, selected);
	}

	public static ItemStack getCharged() {
		ItemStack chargedCell = new ItemStack(RegistryManager.COPPER_CELL_ITEM.get());
		IEmberCapability cap = com.rekindled.embers.util.CapabilityCompat.getCapability(chargedCell, EmbersCapabilities.EMBER_CAPABILITY).orElse(null);
		cap.setEmber(cap.getEmberCapacity());
		return chargedCell;
	}

	@Override
	protected boolean updateCustomBlockEntityTag(BlockPos pPos, Level pLevel, @Nullable Player pPlayer, ItemStack pStack, BlockState pState) {
		IEmberCapability cap = com.rekindled.embers.util.CapabilityCompat.getCapability(pStack, EmbersCapabilities.EMBER_CAPABILITY).orElse(null);
		if (cap != null) {
			CompoundTag blockEntityData = new CompoundTag();
			blockEntityData.putString("id", RegistryManager.COPPER_CELL_ENTITY.getId().toString());
			cap.writeToNBT(blockEntityData);
			pStack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(blockEntityData));
		}
		return BlockItem.updateCustomBlockEntityTag(pLevel, pPlayer, pPos, pStack);
	}

	@Override
	public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag isAdvanced) {
		ensureBlockEntityId(stack);
		IEmberCapability cap = com.rekindled.embers.util.CapabilityCompat.getCapability(stack, EmbersCapabilities.EMBER_CAPABILITY).orElse(null);
		if (cap != null) {
			DecimalFormat emberFormat = DecimalFormats.getDecimalFormat(Embers.MODID + ".decimal_format.ember");
			tooltip.add(Component.translatable(Embers.MODID + ".tooltip.item.ember", emberFormat.format(cap.getEmber()),  emberFormat.format(cap.getEmberCapacity())).withStyle(ChatFormatting.GRAY));
		}
	}

	private static void ensureBlockEntityId(ItemStack stack) {
		CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
		if (data == null) {
			return;
		}
		CompoundTag tag = data.copyTag();
		if (!tag.contains("id")) {
			tag.putString("id", RegistryManager.COPPER_CELL_ENTITY.getId().toString());
			stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
		}
	}
}
