package com.rekindled.embers.compat.aeronautics;

import java.util.UUID;

import com.rekindled.embers.compat.sublevel.SubLevelCompat;
import com.rekindled.embers.util.ItemData;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

final class TurngearHammerData {
	private static final String WORLD = "turngearWorld";
	private static final String POSITION = "turngearPosition";
	private static final String SUB_LEVEL = "turngearSubLevel";

	private TurngearHammerData() {
	}

	static void store(ItemStack hammer, CaminiteTurngearBlockEntity turngear) {
		CompoundTag tag = ItemData.getOrCreateTag(hammer);
		tag.putString(WORLD, turngear.getLevel().dimension().location().toString());
		tag.putLong(POSITION, turngear.getBlockPos().asLong());
		UUID subLevelId = SubLevelCompat.getContainingSubLevelId(turngear);
		if (subLevelId == null) {
			tag.remove(SUB_LEVEL);
		} else {
			tag.putUUID(SUB_LEVEL, subLevelId);
		}
	}

	static CaminiteTurngearBlockEntity resolve(ItemStack hammer, Level level) {
		if (!ItemData.hasTag(hammer)) {
			return null;
		}
		CompoundTag tag = ItemData.getTag(hammer);
		if (!tag.contains(WORLD) || !tag.contains(POSITION)
				|| !level.dimension().location().toString().equals(tag.getString(WORLD))) {
			return null;
		}
		UUID subLevelId = tag.hasUUID(SUB_LEVEL) ? tag.getUUID(SUB_LEVEL) : null;
		BlockEntity blockEntity = SubLevelCompat.findStoredPosition(level, BlockPos.of(tag.getLong(POSITION)), subLevelId);
		return blockEntity instanceof CaminiteTurngearBlockEntity turngear ? turngear : null;
	}
}
