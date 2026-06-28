package com.rekindled.embers.item;

import com.rekindled.embers.util.ItemData;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.rekindled.embers.EmbersEvents;
import com.rekindled.embers.api.item.IEmberChargedTool;
import com.rekindled.embers.particle.GlowParticleOptions;
import com.rekindled.embers.util.EmberInventoryUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ClockworkToolItem extends DiggerItem implements IEmberChargedTool {
	private static final double EMBER_USE_COST = 5.0;

	public ClockworkToolItem(float attackDamageModifier, float attackSpeedModifier, Tier tier, TagKey<Block> blocks, Properties properties) {
		super(tier, blocks, properties.attributes(DiggerItem.createAttributes(tier, attackDamageModifier, attackSpeedModifier)));
	}

	@Override
	public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
		if (canUseEmber(stack, player)) {
			entity.setRemainingFireTicks(40);
			return false;
		}
		return true;
	}

	@Override
	public float getDestroySpeed(ItemStack stack, BlockState state) {
		if (hasEmber(stack)) {
			return super.getDestroySpeed(stack, state);
		}
		return 0;
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		ItemData.updateTag(stack, tag -> tag.putBoolean("didUse", true));
		if (target.level() instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(GlowParticleOptions.EMBER, target.getX(), target.getY() + target.getEyeHeight() / 1.5, target.getZ(), 70, 0.15, 0.15, 0.15, 0.6);
		}
		return super.hurtEnemy(stack, target, attacker);
	}

	@Override
	public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
		return 0;
	}

	@Override
	public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entityLiving) {
		ItemData.updateTag(stack, tag -> tag.putBoolean("didUse", true));
		if (!level.isClientSide() && EmbersEvents.shouldGrantMiningHeat(state, level, pos))
			EmbersEvents.addHeat(entityLiving, stack, 1.0f);
		return super.mineBlock(stack, level, state, pos, entityLiving);
	}

	@Override
	public boolean isEnchantable(ItemStack pStack) {
		return true;
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		if (ItemData.hasTag(oldStack) && ItemData.hasTag(newStack)) {
			return slotChanged || ItemData.getTag(oldStack).getBoolean("poweredOn") != ItemData.getTag(newStack).getBoolean("poweredOn") || newStack.getItem() != oldStack.getItem();
		}
		return slotChanged || newStack.getItem() != oldStack.getItem();
	}

	@Override
	public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
		if (ItemData.hasTag(oldStack) && ItemData.hasTag(newStack)) {
			return ItemData.getTag(oldStack).getBoolean("poweredOn") != ItemData.getTag(newStack).getBoolean("poweredOn");
		}
		return false;
	}

	@Override
	public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
		if (!selected || world.isClientSide())
			return;
		if (!ItemData.hasTag(stack)) {
			ItemData.updateTag(stack, tag -> {
				tag.putBoolean("poweredOn", false);
				tag.putBoolean("didUse", false);
			});
		} else {
			if (entity instanceof Player player) {
				if (world.getGameTime() % 5 == 0) {
					if (player.isCreative() || EmberInventoryUtil.getEmberTotal(player) >= EMBER_USE_COST) {
						if (!ItemData.getTag(stack).getBoolean("poweredOn")) {
							ItemData.updateTag(stack, tag -> tag.putBoolean("poweredOn", true));
						}
					} else {
						if (ItemData.getTag(stack).getBoolean("poweredOn")) {
							ItemData.updateTag(stack, tag -> tag.putBoolean("poweredOn", false));
						}
					}
				}
				if (ItemData.getTag(stack).getBoolean("didUse")) {
					if (!player.isCreative()) {
						EmberInventoryUtil.removeEmber(player, EMBER_USE_COST);
					}
					boolean poweredOn = player.isCreative() || EmberInventoryUtil.getEmberTotal(player) >= EMBER_USE_COST;
					ItemData.updateTag(stack, tag -> {
						tag.putBoolean("didUse", false);
						if (!poweredOn)
							tag.putBoolean("poweredOn", false);
					});
				}
			}
		}
	}

	@Override
	public boolean hasEmber(ItemStack stack) {
		return ItemData.hasTag(stack) && ItemData.getTag(stack).getBoolean("poweredOn");
	}

	private boolean canUseEmber(ItemStack stack, Player player) {
		return hasEmber(stack) || player.isCreative() || EmberInventoryUtil.getEmberTotal(player) >= EMBER_USE_COST;
	}
}
