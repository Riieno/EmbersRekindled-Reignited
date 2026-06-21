package com.rekindled.embers.compat.create;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;

import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.api.power.IEmberPacketReceiver;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

final class EmberReceiverMovementBehaviour implements MovementBehaviour {
	static final EmberReceiverMovementBehaviour INSTANCE = new EmberReceiverMovementBehaviour();
	private static final Map<Level, Map<BlockPos, MovingReceiver>> MOVING_RECEIVERS = new WeakHashMap<>();

	private EmberReceiverMovementBehaviour() {
	}

	@Override
	public void startMoving(MovementContext context) {
		if (context.temporaryData instanceof MovingReceiver) {
			return;
		}
		BlockPos originalPosition = context.contraption.anchor.offset(context.localPos);
		BlockEntity blockEntity = createBlockEntity(context, originalPosition);
		if (!(blockEntity instanceof IEmberPacketReceiver)) {
			return;
		}
		blockEntity.setLevel(context.world);
		MovingReceiver moving = new MovingReceiver(originalPosition, blockEntity);
		context.temporaryData = moving;
		synchronized (MOVING_RECEIVERS) {
			MOVING_RECEIVERS.computeIfAbsent(context.world, ignored -> new HashMap<>()).put(originalPosition, moving);
		}
	}

	@Override
	public void tick(MovementContext context) {
		if (!(context.temporaryData instanceof MovingReceiver)) {
			startMoving(context);
		}
		if (context.temporaryData instanceof MovingReceiver moving) {
			if (context.position != null) {
				moving.physicalPosition = context.position;
			}
			writeEmberData(context, moving.blockEntity);
		}
	}

	@Override
	public void writeExtraData(MovementContext context) {
		if (context.temporaryData instanceof MovingReceiver moving) {
			writeEmberData(context, moving.blockEntity);
		}
	}

	@Override
	public void stopMoving(MovementContext context) {
		if (!(context.temporaryData instanceof MovingReceiver moving)) {
			return;
		}
		writeEmberData(context, moving.blockEntity);
		synchronized (MOVING_RECEIVERS) {
			Map<BlockPos, MovingReceiver> receivers = MOVING_RECEIVERS.get(context.world);
			if (receivers != null && receivers.get(moving.originalPosition) == moving) {
				receivers.remove(moving.originalPosition);
				if (receivers.isEmpty()) {
					MOVING_RECEIVERS.remove(context.world);
				}
			}
		}
		context.temporaryData = null;
	}

	@Override
	public @Nullable ItemStack canBeDisabledVia(MovementContext context) {
		return null;
	}

	static @Nullable BlockEntity findByOriginalPosition(Level level, BlockPos position) {
		synchronized (MOVING_RECEIVERS) {
			Map<BlockPos, MovingReceiver> receivers = MOVING_RECEIVERS.get(level);
			MovingReceiver moving = receivers == null ? null : receivers.get(position);
			return moving == null ? null : moving.blockEntity;
		}
	}

	static @Nullable BlockEntity findAtPhysicalPosition(Level level, Vec3 position) {
		synchronized (MOVING_RECEIVERS) {
			Map<BlockPos, MovingReceiver> receivers = MOVING_RECEIVERS.get(level);
			if (receivers == null) {
				return null;
			}
			for (MovingReceiver moving : receivers.values()) {
				if (moving.physicalPosition.distanceToSqr(position) < 0.5625D) {
					return moving.blockEntity;
				}
			}
			return null;
		}
	}

	static @Nullable Vec3 getPhysicalPosition(Level level, BlockPos originalPosition) {
		synchronized (MOVING_RECEIVERS) {
			Map<BlockPos, MovingReceiver> receivers = MOVING_RECEIVERS.get(level);
			MovingReceiver moving = receivers == null ? null : receivers.get(originalPosition);
			return moving == null ? null : moving.physicalPosition;
		}
	}

	private static BlockEntity createBlockEntity(MovementContext context, BlockPos originalPosition) {
		BlockEntity blockEntity = null;
		if (context.blockEntityData != null && context.blockEntityData.contains("id")) {
			blockEntity = BlockEntity.loadStatic(originalPosition, context.state, context.blockEntityData,
					context.world.registryAccess());
		}
		if (blockEntity == null && context.state.is(RegistryManager.EMBER_FUNNEL.get())) {
			blockEntity = RegistryManager.EMBER_FUNNEL_ENTITY.get().create(originalPosition, context.state);
		}
		if (blockEntity == null && context.state.is(RegistryManager.EMBER_RECEIVER.get())) {
			blockEntity = RegistryManager.EMBER_RECEIVER_ENTITY.get().create(originalPosition, context.state);
		}
		if (blockEntity == null && context.state.is(RegistryManager.EMBER_ENERGY_CONVERTER.get())) {
			blockEntity = RegistryManager.EMBER_ENERGY_CONVERTER_ENTITY.get().create(originalPosition, context.state);
		}
		if (blockEntity == null && context.state.is(RegistryManager.EMBER_RELAY.get())) {
			blockEntity = RegistryManager.EMBER_RELAY_ENTITY.get().create(originalPosition, context.state);
		}
		if (blockEntity == null && context.state.is(RegistryManager.MIRROR_RELAY.get())) {
			blockEntity = RegistryManager.MIRROR_RELAY_ENTITY.get().create(originalPosition, context.state);
		}
		if (blockEntity == null && context.state.is(RegistryManager.BEAM_SPLITTER.get())) {
			blockEntity = RegistryManager.BEAM_SPLITTER_ENTITY.get().create(originalPosition, context.state);
		}
		return blockEntity;
	}

	private static void writeEmberData(MovementContext context, BlockEntity blockEntity) {
		if (context.blockEntityData != null && blockEntity.getLevel() != null) {
			CompoundTag saved = blockEntity.saveWithFullMetadata(context.world.registryAccess());
			saved.remove("x");
			saved.remove("y");
			saved.remove("z");
			for (String key : new HashSet<>(context.blockEntityData.getAllKeys())) {
				context.blockEntityData.remove(key);
			}
			context.blockEntityData.merge(saved);
		}
	}

	private static final class MovingReceiver {
		private final BlockPos originalPosition;
		private final BlockEntity blockEntity;
		private Vec3 physicalPosition;

		private MovingReceiver(BlockPos originalPosition, BlockEntity blockEntity) {
			this.originalPosition = originalPosition;
			this.blockEntity = blockEntity;
			this.physicalPosition = Vec3.atCenterOf(originalPosition);
		}
	}
}
