package com.rekindled.embers.compat.sublevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.UUID;
import java.util.function.BiFunction;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import com.rekindled.embers.api.power.IEmberPacketReceiver;
import com.rekindled.embers.blockentity.BeamSplitterBlockEntity;
import com.rekindled.embers.blockentity.EmberEmitterBlockEntity;
import com.rekindled.embers.blockentity.EmberRelayBlockEntity;
import com.rekindled.embers.blockentity.MirrorRelayBlockEntity;
import com.rekindled.embers.compat.create.CreateCompat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

public final class SubLevelCompat {
	private static Object helper;
	private static Method getContaining;
	private static Method runIncludingSubLevels;
	private static Method logicalPose;
	private static Method transformPosition;
	private static Method transformPositionInverse;
	private static Method transformNormal;
	private static Method transformNormalInverse;
	private static Class<?> subLevelContainerClass;
	private static Class<?> trackingPointClass;
	private static Method getTrackingPointData;
	private static Method getTrackingPoint;
	private static Method setTrackingPoint;
	private static Method inSubLevel;
	private static Method subLevelID;
	private static Method point;
	private static Method globalPlaceholderPosition;
	private static Method getLastSerializationPointer;
	private static Constructor<?> trackingPointConstructor;
	private static final Map<BlockPos, UUID> movingEmberLinkTargets = new HashMap<>();
	private static boolean initialized;

	private SubLevelCompat() {
	}

	private static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		try {
			Class<?> sable = Class.forName("dev.ryanhcode.sable.Sable");
			Field helperField = sable.getField("HELPER");
			helper = helperField.get(null);
			getContaining = helper.getClass().getMethod("getContaining", BlockEntity.class);
			Class<?> access = Class.forName("dev.ryanhcode.sable.companion.SubLevelAccess");
			runIncludingSubLevels = helper.getClass().getMethod("runIncludingSubLevels", Level.class,
					net.minecraft.core.Position.class, boolean.class, access, BiFunction.class);
			subLevelContainerClass = Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer");
			logicalPose = access.getMethod("logicalPose");
			transformPosition = findMethod(logicalPose.getReturnType(), "transformPosition", "transformPoint", "transform");
			transformPositionInverse = findMethod(logicalPose.getReturnType(), "transformPositionInverse", "inverseTransformPosition", "transformInverse");
			transformNormal = findMethod(logicalPose.getReturnType(), "transformNormal", "transformDirection", "rotateNormal", "rotateDirection");
			transformNormalInverse = findMethod(logicalPose.getReturnType(), "transformNormalInverse", "transformDirectionInverse", "inverseTransformNormal", "inverseTransformDirection");
		} catch (ReflectiveOperationException ignored) {
			helper = null;
			return;
		}
		try {
			Class<?> trackingData = Class.forName("dev.ryanhcode.sable.sublevel.tracking_points.SubLevelTrackingPointSavedData");
			trackingPointClass = Class.forName("dev.ryanhcode.sable.sublevel.tracking_points.TrackingPoint");
			getTrackingPointData = trackingData.getMethod("getOrLoad", ServerLevel.class);
			getTrackingPoint = trackingData.getMethod("getTrackingPoint", UUID.class);
			setTrackingPoint = trackingData.getMethod("setTrackingPoint", UUID.class, trackingPointClass);
			inSubLevel = trackingPointClass.getMethod("inSubLevel");
			subLevelID = trackingPointClass.getMethod("subLevelID");
			point = trackingPointClass.getMethod("point");
			globalPlaceholderPosition = trackingPointClass.getMethod("globalPlaceholderPosition");
			Class<?> savedPointer = Class.forName("dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer");
			trackingPointConstructor = trackingPointClass.getConstructor(boolean.class, UUID.class, savedPointer, Vector3d.class, Vector3d.class);
		} catch (ReflectiveOperationException ignored) {
			trackingPointClass = null;
			getTrackingPointData = null;
			getTrackingPoint = null;
			setTrackingPoint = null;
			inSubLevel = null;
			subLevelID = null;
			point = null;
			globalPlaceholderPosition = null;
			getLastSerializationPointer = null;
			trackingPointConstructor = null;
		}
	}

	public record TrackedPosition(@Nullable BlockPos position, @Nullable UUID subLevelId, @Nullable UUID trackingPointId, @Nullable Vec3 physicalPosition) {
	}

	public static @Nullable UUID getContainingSubLevelId(BlockEntity blockEntity) {
		if (blockEntity == null) {
			return null;
		}
		init();
		if (helper == null || getContaining == null) {
			return null;
		}
		try {
			return getSubLevelId(getContaining.invoke(helper, blockEntity));
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return null;
		}
	}

	public static boolean isCrossSubLevelLink(BlockEntity origin, @Nullable UUID targetSubLevelId) {
		return origin != null && !Objects.equals(getContainingSubLevelId(origin), targetSubLevelId);
	}

	public static <T extends BlockEntity> List<T> findBlockEntitiesInPhysicalRange(BlockEntity origin, Class<T> type, double range) {
		if (origin == null || type == null || range < 0 || !(rootLevel(origin.getLevel()) instanceof ServerLevel serverLevel)) {
			return List.of();
		}
		Vec3 center = toPhysicalPosition(origin, Vec3.atCenterOf(origin.getBlockPos()));
		double rangeSquared = range * range;
		List<T> matches = new ArrayList<>();
		for (BlockEntity blockEntity : getLoadedBlockEntitiesIncludingSubLevels(serverLevel)) {
			if (!type.isInstance(blockEntity)) {
				continue;
			}
			Vec3 candidate = toPhysicalPosition(blockEntity, Vec3.atCenterOf(blockEntity.getBlockPos()));
			if (center.distanceToSqr(candidate) <= rangeSquared) {
				matches.add(type.cast(blockEntity));
			}
		}
		return matches;
	}

	private static Level rootLevel(@Nullable Level level) {
		if (level == null) {
			return null;
		}
		try {
			Object parent = level.getClass().getMethod("getLevel").invoke(level);
			if (parent instanceof Level parentLevel && parentLevel != level) {
				return parentLevel;
			}
		} catch (ReflectiveOperationException | RuntimeException ignored) {
		}
		return level;
	}

	public static @Nullable BlockEntity findLinkedTarget(BlockEntity origin, @Nullable BlockPos targetPosition, @Nullable UUID targetSubLevelId) {
		if (origin == null || targetPosition == null || origin.getLevel() == null) {
			return null;
		}
		if (targetSubLevelId != null) {
			Object subLevel = getSubLevel(origin.getLevel(), targetSubLevelId);
			if (subLevel != null) {
				return findBlockEntityInAccess(origin.getLevel(), subLevel, targetPosition);
			}
			return null;
		}
		BlockEntity movingTarget = findMovingCreateReceiver(origin.getLevel(), targetPosition);
		if (movingTarget != null) {
			return movingTarget;
		}
		BlockEntity target = findAtPhysicalPosition(origin.getLevel(), Vec3.atCenterOf(targetPosition));
		return target != null ? target : findAtPhysicalPosition(origin, targetPosition);
	}

	public static @Nullable BlockEntity findReachableLinkedTarget(BlockEntity origin, @Nullable BlockPos targetPosition, @Nullable UUID targetSubLevelId) {
		return findReachableLinkedTarget(origin, targetPosition, targetSubLevelId, null);
	}

	public static @Nullable BlockEntity findReachableLinkedTarget(BlockEntity origin, @Nullable BlockPos targetPosition, @Nullable UUID targetSubLevelId, @Nullable Vec3 targetPhysicalPosition) {
		BlockEntity target = findLinkedTarget(origin, targetPosition, targetSubLevelId);
		if (target == null && origin != null && origin.getLevel() != null && targetPhysicalPosition != null) {
			target = findAtPhysicalPosition(origin.getLevel(), targetPhysicalPosition);
		}
		if (target == null || origin == null || origin.getLevel() == null || targetPosition == null) {
			return null;
		}
		if (targetSubLevelId == null && target == findMovingCreateReceiver(origin.getLevel(), targetPosition)) {
			return target;
		}
		UUID actualSubLevelId = getContainingSubLevelId(target);
		if (targetPosition.equals(target.getBlockPos()) && Objects.equals(targetSubLevelId, actualSubLevelId)) {
			return target;
		}
		Vec3 physicalPosition = targetPhysicalPosition != null ? targetPhysicalPosition : linkedTargetPhysicalPosition(origin, targetPosition, targetSubLevelId);
		BlockEntity reachableTarget = findAtPhysicalPosition(origin.getLevel(), physicalPosition);
		if (reachableTarget == null) {
			return null;
		}
		if (reachableTarget == target) {
			return target;
		}
		return targetPosition.equals(reachableTarget.getBlockPos()) && Objects.equals(targetSubLevelId, getContainingSubLevelId(reachableTarget))
				? target
				: null;
	}

	public static @Nullable BlockEntity findStoredPosition(Level level, @Nullable BlockPos position, @Nullable UUID subLevelId) {
		if (level == null || position == null) {
			return null;
		}
		if (subLevelId != null) {
			Object subLevel = getSubLevel(level, subLevelId);
			return subLevel == null ? null : findBlockEntityInAccess(level, subLevel, position);
		}
		BlockEntity movingTarget = findMovingCreateReceiver(level, position);
		if (movingTarget != null) {
			return movingTarget;
		}
		return findAtPosition(level, position);
	}

	public static Vec3 linkedTargetPhysicalPosition(BlockEntity origin, @Nullable BlockPos targetPosition, @Nullable UUID targetSubLevelId) {
		if (targetPosition == null) {
			return origin == null ? Vec3.ZERO : Vec3.atCenterOf(origin.getBlockPos());
		}
		if (origin != null && origin.getLevel() != null && targetSubLevelId == null) {
			Vec3 movingPosition = findMovingCreateReceiverPosition(origin.getLevel(), targetPosition);
			if (movingPosition != null) {
				return movingPosition;
			}
		}
		if (origin == null || origin.getLevel() == null || targetSubLevelId == null) {
			return Vec3.atCenterOf(targetPosition);
		}
		Object subLevel = getSubLevel(origin.getLevel(), targetSubLevelId);
		if (subLevel == null) {
			return Vec3.atCenterOf(targetPosition);
		}
		return transformSubLevelPosition(subLevel, Vec3.atCenterOf(targetPosition));
	}

	public static Vec3 storedPhysicalPosition(Level level, @Nullable BlockPos position, @Nullable UUID subLevelId) {
		if (position == null) {
			return Vec3.ZERO;
		}
		if (level != null && subLevelId == null) {
			Vec3 movingPosition = findMovingCreateReceiverPosition(level, position);
			if (movingPosition != null) {
				return movingPosition;
			}
		}
		if (level == null || subLevelId == null) {
			return Vec3.atCenterOf(position);
		}
		Object subLevel = getSubLevel(level, subLevelId);
		return subLevel == null ? Vec3.atCenterOf(position) : transformSubLevelPosition(subLevel, Vec3.atCenterOf(position));
	}

	public static Vec3 currentTrackedPhysicalPosition(BlockEntity origin, @Nullable BlockPos position, @Nullable UUID subLevelId, @Nullable Vec3 fallbackPhysicalPosition) {
		return currentTrackedPhysicalPosition(origin == null ? null : origin.getLevel(), origin, position, subLevelId, fallbackPhysicalPosition);
	}

	public static Vec3 currentTrackedPhysicalPosition(Level level, @Nullable BlockPos position, @Nullable UUID subLevelId, @Nullable Vec3 fallbackPhysicalPosition) {
		return currentTrackedPhysicalPosition(level, null, position, subLevelId, fallbackPhysicalPosition);
	}

	private static Vec3 currentTrackedPhysicalPosition(@Nullable Level level, @Nullable BlockEntity origin, @Nullable BlockPos position, @Nullable UUID subLevelId, @Nullable Vec3 fallbackPhysicalPosition) {
		if (position == null) {
			return origin == null ? Vec3.ZERO : Vec3.atCenterOf(origin.getBlockPos());
		}
		if (level != null && subLevelId == null) {
			Vec3 movingPosition = findMovingCreateReceiverPosition(level, position);
			if (movingPosition != null) {
				return movingPosition;
			}
		}
		if (level != null && subLevelId != null) {
			Object subLevel = getSubLevel(level, subLevelId);
			if (subLevel != null) {
				return transformSubLevelPosition(subLevel, Vec3.atCenterOf(position));
			}
		}
		return fallbackPhysicalPosition != null ? fallbackPhysicalPosition : linkedTargetPhysicalPosition(origin, position, subLevelId);
	}

	public static TrackedPosition captureTrackedTarget(BlockEntity targetEntity, @Nullable UUID existingTrackingPointId) {
		if (targetEntity == null) {
			return new TrackedPosition(null, null, null, null);
		}
		BlockPos position = targetEntity.getBlockPos().immutable();
		UUID subLevelId = getContainingSubLevelId(targetEntity);
		Vec3 physicalPosition = toPhysicalPosition(targetEntity, Vec3.atCenterOf(position));
		UUID trackingPointId = writeTrackingPoint(targetEntity.getLevel(), existingTrackingPointId, position, subLevelId, physicalPosition);
		return new TrackedPosition(position, subLevelId, trackingPointId, physicalPosition);
	}

	public static TrackedPosition refreshTrackedTarget(Level level, @Nullable BlockPos position, @Nullable UUID subLevelId, @Nullable UUID trackingPointId, @Nullable Vec3 physicalPosition) {
		if (trackingPointId != null && level instanceof ServerLevel serverLevel) {
			TrackedPosition tracked = readTrackingPoint(serverLevel, trackingPointId);
			if (tracked.position() != null) {
				return tracked;
			}
			UUID recreated = writeTrackingPoint(level, trackingPointId, position, subLevelId, physicalPosition);
			if (recreated != null) {
				return new TrackedPosition(position, subLevelId, recreated, physicalPosition);
			}
		}
		if (position == null) {
			return new TrackedPosition(null, null, trackingPointId, physicalPosition);
		}
		Vec3 nextPhysical = physicalPosition != null ? physicalPosition : storedPhysicalPosition(level, position, subLevelId);
		UUID nextTrackingPointId = writeTrackingPoint(level, trackingPointId, position, subLevelId, nextPhysical);
		return new TrackedPosition(position, subLevelId, nextTrackingPointId, nextPhysical);
	}

	public static TrackedPosition refreshTrackedTarget(BlockEntity owner, @Nullable BlockPos position, @Nullable UUID subLevelId, @Nullable UUID trackingPointId, @Nullable Vec3 physicalPosition) {
		if (owner == null || owner.getLevel() == null) {
			return new TrackedPosition(position, subLevelId, trackingPointId, physicalPosition);
		}
		if (trackingPointId != null && owner.getLevel() instanceof ServerLevel serverLevel) {
			TrackedPosition tracked = readTrackingPoint(serverLevel, trackingPointId);
			if (tracked.position() != null) {
				return tracked;
			}
		}
		BlockEntity target = resolveTrackedTargetOwnerPosition(owner, position, subLevelId, physicalPosition);
		if (target != null) {
			return captureTrackedTarget(target, trackingPointId);
		}
		return refreshTrackedTarget(owner.getLevel(), position, subLevelId, trackingPointId, physicalPosition);
	}

	public static void rememberMovingEmberLinkTarget(BlockEntity target) {
		if (target instanceof IEmberPacketReceiver) {
			movingEmberLinkTargets.put(target.getBlockPos().immutable(), getContainingSubLevelId(target));
		}
	}

	public static void reconnectMovedEmberLinks(ServerLevel level, BlockPos oldPosition, BlockEntity movedTarget) {
		if (level == null || oldPosition == null || !(movedTarget instanceof IEmberPacketReceiver)) {
			return;
		}
		UUID oldSubLevelId = movingEmberLinkTargets.remove(oldPosition);
		for (BlockEntity owner : getLoadedBlockEntitiesIncludingSubLevels(level)) {
			if (owner instanceof EmberEmitterBlockEntity emitter) {
				reconnectEmitter(emitter, oldPosition, oldSubLevelId, movedTarget);
			} else if (owner instanceof EmberRelayBlockEntity relay) {
				reconnectRelay(relay, oldPosition, oldSubLevelId, movedTarget);
			} else if (owner instanceof MirrorRelayBlockEntity relay) {
				reconnectMirrorRelay(relay, oldPosition, oldSubLevelId, movedTarget);
			} else if (owner instanceof BeamSplitterBlockEntity splitter) {
				reconnectBeamSplitter(splitter, oldPosition, oldSubLevelId, movedTarget);
			}
		}
	}

	private static void reconnectEmitter(EmberEmitterBlockEntity emitter, BlockPos oldPosition, @Nullable UUID oldSubLevelId, BlockEntity movedTarget) {
		if (!sameStoredTarget(emitter.target, emitter.targetSubLevelId, oldPosition, oldSubLevelId)) {
			return;
		}
		TrackedPosition tracked = captureTrackedTarget(movedTarget, emitter.targetTrackingPointId);
		emitter.target = tracked.position();
		emitter.targetSubLevelId = tracked.subLevelId();
		emitter.targetTrackingPointId = tracked.trackingPointId();
		emitter.targetPhysicalPosition = tracked.physicalPosition();
		emitter.setChanged();
	}

	private static void reconnectRelay(EmberRelayBlockEntity relay, BlockPos oldPosition, @Nullable UUID oldSubLevelId, BlockEntity movedTarget) {
		if (!sameStoredTarget(relay.target, relay.targetSubLevelId, oldPosition, oldSubLevelId)) {
			return;
		}
		TrackedPosition tracked = captureTrackedTarget(movedTarget, relay.targetTrackingPointId);
		relay.target = tracked.position();
		relay.targetSubLevelId = tracked.subLevelId();
		relay.targetTrackingPointId = tracked.trackingPointId();
		relay.targetPhysicalPosition = tracked.physicalPosition();
		relay.setChanged();
	}

	private static void reconnectMirrorRelay(MirrorRelayBlockEntity relay, BlockPos oldPosition, @Nullable UUID oldSubLevelId, BlockEntity movedTarget) {
		if (!sameStoredTarget(relay.target, relay.targetSubLevelId, oldPosition, oldSubLevelId)) {
			return;
		}
		TrackedPosition tracked = captureTrackedTarget(movedTarget, relay.targetTrackingPointId);
		relay.target = tracked.position();
		relay.targetSubLevelId = tracked.subLevelId();
		relay.targetTrackingPointId = tracked.trackingPointId();
		relay.targetPhysicalPosition = tracked.physicalPosition();
		relay.setChanged();
	}

	private static void reconnectBeamSplitter(BeamSplitterBlockEntity splitter, BlockPos oldPosition, @Nullable UUID oldSubLevelId, BlockEntity movedTarget) {
		boolean changed = false;
		if (sameStoredTarget(splitter.target1, splitter.target1SubLevelId, oldPosition, oldSubLevelId)) {
			TrackedPosition tracked = captureTrackedTarget(movedTarget, splitter.target1TrackingPointId);
			splitter.target1 = tracked.position();
			splitter.target1SubLevelId = tracked.subLevelId();
			splitter.target1TrackingPointId = tracked.trackingPointId();
			splitter.target1PhysicalPosition = tracked.physicalPosition();
			changed = true;
		}
		if (sameStoredTarget(splitter.target2, splitter.target2SubLevelId, oldPosition, oldSubLevelId)) {
			TrackedPosition tracked = captureTrackedTarget(movedTarget, splitter.target2TrackingPointId);
			splitter.target2 = tracked.position();
			splitter.target2SubLevelId = tracked.subLevelId();
			splitter.target2TrackingPointId = tracked.trackingPointId();
			splitter.target2PhysicalPosition = tracked.physicalPosition();
			changed = true;
		}
		if (changed) {
			splitter.setChanged();
		}
	}

	private static boolean sameStoredTarget(@Nullable BlockPos target, @Nullable UUID subLevelId, BlockPos oldPosition, @Nullable UUID oldSubLevelId) {
		return oldPosition.equals(target) && Objects.equals(subLevelId, oldSubLevelId);
	}

	private static List<BlockEntity> getLoadedBlockEntitiesIncludingSubLevels(ServerLevel level) {
		List<BlockEntity> blockEntities = new ArrayList<>();
		Set<BlockEntity> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		for (BlockEntity blockEntity : getLoadedLevelBlockEntities(level)) {
			if (blockEntity != null && !blockEntity.isRemoved() && seen.add(blockEntity)) {
				blockEntities.add(blockEntity);
			}
		}
		for (Object subLevel : getAllSubLevels(level)) {
			for (BlockEntity blockEntity : getBlockEntities(subLevel)) {
				if (blockEntity != null && !blockEntity.isRemoved() && seen.add(blockEntity)) {
					blockEntities.add(blockEntity);
				}
			}
		}
		return blockEntities;
	}

	private static List<BlockEntity> getLoadedLevelBlockEntities(ServerLevel level) {
		List<BlockEntity> blockEntities = new ArrayList<>();
		try {
			Object chunkMap = level.getChunkSource().chunkMap;
			Method getChunksMethod = chunkMap.getClass().getDeclaredMethod("getChunks");
			getChunksMethod.setAccessible(true);
			Object chunksObject = getChunksMethod.invoke(chunkMap);
			if (!(chunksObject instanceof Iterable<?> chunks)) {
				return blockEntities;
			}
			for (Object holderObject : chunks) {
				if (!(holderObject instanceof ChunkHolder holder)) {
					continue;
				}
				LevelChunk chunk = holder.getTickingChunk();
				if (chunk != null) {
					blockEntities.addAll(chunk.getBlockEntities().values());
				}
			}
		} catch (ReflectiveOperationException | RuntimeException ignored) {
		}
		return blockEntities;
	}

	private static List<Object> getAllSubLevels(Level level) {
		init();
		if (subLevelContainerClass == null) {
			return List.of();
		}
		try {
			Object container = getContainer(level);
			if (container == null) {
				return List.of();
			}
			Object subLevels = container.getClass().getMethod("getAllSubLevels").invoke(container);
			if (!(subLevels instanceof Iterable<?> iterable)) {
				return List.of();
			}
			List<Object> result = new ArrayList<>();
			for (Object subLevel : iterable) {
				result.add(subLevel);
			}
			return result;
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return List.of();
		}
	}

	private static @Nullable BlockEntity resolveTrackedTargetOwnerPosition(BlockEntity owner, @Nullable BlockPos position, @Nullable UUID subLevelId, @Nullable Vec3 physicalPosition) {
		if (position == null || owner == null || owner.getLevel() == null) {
			return null;
		}
		if (subLevelId != null) {
			BlockEntity target = findStoredPosition(owner.getLevel(), position, subLevelId);
			return target != null ? target : findAtPhysicalPosition(owner.getLevel(), physicalPosition);
		}
		BlockEntity target = isInSubLevel(owner) ? findAtPhysicalPosition(owner, position) : null;
		if (target != null) {
			return target;
		}
		target = findStoredPosition(owner.getLevel(), position, null);
		return target != null ? target : findAtPhysicalPosition(owner.getLevel(), physicalPosition);
	}

	public static <T extends Entity> List<T> getEntitiesInPhysicalBounds(BlockEntity origin, Class<T> entityClass, AABB localBounds) {
		if (origin == null || origin.getLevel() == null || entityClass == null || localBounds == null) {
			return List.of();
		}
		AABB physicalBounds = toPhysicalBounds(origin, localBounds);
		List<T> found = origin.getLevel().getEntitiesOfClass(entityClass, physicalBounds);
		if (found.size() < 2) {
			return found;
		}
		List<T> unique = new ArrayList<>(found.size());
		Set<T> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		for (T entity : found) {
			if (entity != null && !entity.isRemoved() && seen.add(entity)) {
				unique.add(entity);
			}
		}
		return unique;
	}

	public static AABB toPhysicalBounds(BlockEntity origin, AABB localBounds) {
		if (origin == null || localBounds == null) {
			return localBounds;
		}
		Vec3[] corners = new Vec3[] {
				new Vec3(localBounds.minX, localBounds.minY, localBounds.minZ),
				new Vec3(localBounds.minX, localBounds.minY, localBounds.maxZ),
				new Vec3(localBounds.minX, localBounds.maxY, localBounds.minZ),
				new Vec3(localBounds.minX, localBounds.maxY, localBounds.maxZ),
				new Vec3(localBounds.maxX, localBounds.minY, localBounds.minZ),
				new Vec3(localBounds.maxX, localBounds.minY, localBounds.maxZ),
				new Vec3(localBounds.maxX, localBounds.maxY, localBounds.minZ),
				new Vec3(localBounds.maxX, localBounds.maxY, localBounds.maxZ)
		};
		Vec3 first = toPhysicalPosition(origin, corners[0]);
		double minX = first.x;
		double minY = first.y;
		double minZ = first.z;
		double maxX = first.x;
		double maxY = first.y;
		double maxZ = first.z;
		for (int i = 1; i < corners.length; i++) {
			Vec3 transformed = toPhysicalPosition(origin, corners[i]);
			minX = Math.min(minX, transformed.x);
			minY = Math.min(minY, transformed.y);
			minZ = Math.min(minZ, transformed.z);
			maxX = Math.max(maxX, transformed.x);
			maxY = Math.max(maxY, transformed.y);
			maxZ = Math.max(maxZ, transformed.z);
		}
		return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
	}

	private static @Nullable Method findMethod(Class<?> owner, String... names) {
		for (String name : names) {
			try {
				return owner.getMethod(name, Vec3.class);
			} catch (ReflectiveOperationException ignored) {
			}
		}
		return null;
	}

	public static @Nullable BlockEntity findAdjacent(BlockEntity origin, Direction localDirection) {
		Level level = origin.getLevel();
		if (level == null) {
			return null;
		}
		BlockPos directPos = origin.getBlockPos().relative(localDirection);

		init();
		if (helper == null || getContaining == null || runIncludingSubLevels == null) {
			return level.getBlockEntity(directPos);
		}
		try {
			Object containing = getContaining.invoke(helper, origin);
			if (containing == null) {
				return level.getBlockEntity(directPos);
			}
			Object result = runIncludingSubLevels.invoke(helper, level, Vec3.atCenterOf(directPos), true, containing,
					(BiFunction<Object, BlockPos, BlockEntity>) (subLevel, internalPos) -> {
						BlockEntity candidate = findBlockEntityInAccess(level, subLevel, internalPos);
						return candidate == origin ? null : candidate;
					});
			return result instanceof BlockEntity blockEntity ? blockEntity : null;
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return null;
		}
	}

	public static @Nullable BlockEntity findAtPosition(Level level, BlockPos position) {
		if (level == null || position == null) {
			return null;
		}
		return findAtPhysicalPosition(level, Vec3.atCenterOf(position));
	}

	public static BlockState findBlockState(Level level, BlockPos position) {
		if (level == null || position == null) {
			return null;
		}
		return findBlockState(level, Vec3.atCenterOf(position));
	}

	public static @Nullable BlockEntity findAtPhysicalPosition(BlockEntity origin, BlockPos localPosition) {
		Level level = origin.getLevel();
		if (level == null) {
			return null;
		}
		BlockEntity movingTarget = findMovingCreateReceiver(level, localPosition);
		if (movingTarget != null) {
			return movingTarget;
		}
		init();
		if (helper == null || getContaining == null || runIncludingSubLevels == null) {
			return level.getBlockEntity(localPosition);
		}
		try {
			Object containing = getContaining.invoke(helper, origin);
			if (containing == null) {
				return findAtPhysicalPosition(level, Vec3.atCenterOf(localPosition));
			}
			Object result = runIncludingSubLevels.invoke(helper, level, Vec3.atCenterOf(localPosition), true, containing,
					(BiFunction<Object, BlockPos, BlockEntity>) (subLevel, internalPos) -> findBlockEntityInAccess(level, subLevel, internalPos));
			return result instanceof BlockEntity blockEntity ? blockEntity : null;
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return null;
		}
	}

	public static BlockState findBlockState(BlockEntity origin, BlockPos localPosition) {
		Level level = origin.getLevel();
		if (level == null) {
			return null;
		}
		init();
		if (helper == null || getContaining == null || runIncludingSubLevels == null) {
			return level.getBlockState(localPosition);
		}
		try {
			Object containing = getContaining.invoke(helper, origin);
			if (containing == null) {
				return findBlockState(level, Vec3.atCenterOf(localPosition));
			}
			Object result = runIncludingSubLevels.invoke(helper, level, Vec3.atCenterOf(localPosition), true, containing,
					(BiFunction<Object, BlockPos, BlockState>) (subLevel, internalPos) -> level.getBlockState(internalPos));
			return result instanceof BlockState state ? state : level.getBlockState(localPosition);
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return level.getBlockState(localPosition);
		}
	}

	public static boolean isInSubLevel(BlockEntity blockEntity) {
		if (blockEntity == null) {
			return false;
		}
		init();
		if (helper == null || getContaining == null) {
			return false;
		}
		try {
			return getContaining.invoke(helper, blockEntity) != null;
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return false;
		}
	}

	public static @Nullable BlockEntity findAtPhysicalPosition(Level level, Vec3 physicalPosition) {
		if (level == null || physicalPosition == null) {
			return null;
		}
		BlockPos directPos = BlockPos.containing(physicalPosition);
		BlockEntity direct = level.getBlockEntity(directPos);
		if (direct != null) {
			return direct;
		}
		BlockEntity movingTarget = findMovingCreateReceiver(level, physicalPosition);
		if (movingTarget != null) {
			return movingTarget;
		}
		init();
		if (helper == null || runIncludingSubLevels == null) {
			return null;
		}
		try {
			Object result = runIncludingSubLevels.invoke(helper, level, physicalPosition, true, null,
					(BiFunction<Object, BlockPos, BlockEntity>) (subLevel, internalPos) -> findBlockEntityInAccess(level, subLevel, internalPos));
			return result instanceof BlockEntity blockEntity ? blockEntity : null;
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return null;
		}
	}

	private static @Nullable BlockEntity findMovingCreateReceiver(Level level, BlockPos originalPosition) {
		if (level == null || originalPosition == null || !ModList.get().isLoaded("create")) {
			return null;
		}
		try {
			return CreateCompat.findMovingEmberReceiver(level, originalPosition);
		} catch (LinkageError ignored) {
			return null;
		}
	}

	private static @Nullable BlockEntity findMovingCreateReceiver(Level level, Vec3 physicalPosition) {
		if (level == null || physicalPosition == null || !ModList.get().isLoaded("create")) {
			return null;
		}
		try {
			return CreateCompat.findMovingEmberReceiver(level, physicalPosition);
		} catch (LinkageError ignored) {
			return null;
		}
	}

	private static @Nullable Vec3 findMovingCreateReceiverPosition(Level level, BlockPos originalPosition) {
		if (level == null || originalPosition == null || !ModList.get().isLoaded("create")) {
			return null;
		}
		try {
			return CreateCompat.getMovingEmberReceiverPosition(level, originalPosition);
		} catch (LinkageError ignored) {
			return null;
		}
	}

	public static BlockState findBlockState(Level level, Vec3 physicalPosition) {
		if (level == null || physicalPosition == null) {
			return null;
		}
		BlockPos directPos = BlockPos.containing(physicalPosition);
		BlockState direct = level.getBlockState(directPos);
		if (!direct.isAir()) {
			return direct;
		}
		init();
		if (helper == null || runIncludingSubLevels == null) {
			return direct;
		}
		try {
			Object result = runIncludingSubLevels.invoke(helper, level, physicalPosition, true, null,
					(BiFunction<Object, BlockPos, BlockState>) (subLevel, internalPos) -> level.getBlockState(internalPos));
			return result instanceof BlockState state ? state : direct;
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return direct;
		}
	}

	public static Vec3 toPhysicalPosition(BlockEntity origin, BlockPos localPosition) {
		return toPhysicalPosition(origin, Vec3.atCenterOf(localPosition));
	}

	public static Vec3 toPhysicalPosition(BlockEntity origin, Vec3 localPosition) {
		if (origin == null || localPosition == null) {
			return localPosition;
		}
		Level level = origin.getLevel();
		if (level != null && origin == findMovingCreateReceiver(level, origin.getBlockPos())) {
			Vec3 movingCenter = findMovingCreateReceiverPosition(level, origin.getBlockPos());
			if (movingCenter != null) {
				return movingCenter.add(localPosition.subtract(Vec3.atCenterOf(origin.getBlockPos())));
			}
		}
		init();
		if (helper == null || getContaining == null || logicalPose == null || transformPosition == null) {
			return localPosition;
		}
		try {
			Object containing = getContaining.invoke(helper, origin);
			if (containing == null) {
				return localPosition;
			}
			Object pose = logicalPose.invoke(containing);
			Object transformed = transformPosition.invoke(pose, localPosition);
			return transformed instanceof Vec3 vec ? vec : localPosition;
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return localPosition;
		}
	}

	private static Vec3 transformSubLevelPosition(Object subLevel, Vec3 localPosition) {
		if (subLevel == null || localPosition == null) {
			return localPosition;
		}
		init();
		if (logicalPose == null || transformPosition == null) {
			return localPosition;
		}
		try {
			Object pose = logicalPose.invoke(subLevel);
			Object transformed = transformPosition.invoke(pose, localPosition);
			return transformed instanceof Vec3 vec ? vec : localPosition;
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return localPosition;
		}
	}

	private static @Nullable UUID writeTrackingPoint(@Nullable Level level, @Nullable UUID trackingPointId, @Nullable BlockPos position, @Nullable UUID subLevelId, @Nullable Vec3 physicalPosition) {
		if (!(level instanceof ServerLevel serverLevel) || position == null) {
			return trackingPointId;
		}
		init();
		if (getTrackingPointData == null || setTrackingPoint == null || trackingPointConstructor == null) {
			return trackingPointId;
		}
		try {
			UUID id = trackingPointId != null ? trackingPointId : UUID.randomUUID();
			Object subLevel = subLevelId == null ? null : getSubLevel(level, subLevelId);
			Object pointer = null;
			if (subLevel != null) {
				if (getLastSerializationPointer == null) {
					getLastSerializationPointer = subLevel.getClass().getMethod("getLastSerializationPointer");
				}
				pointer = getLastSerializationPointer.invoke(subLevel);
			}
			Vec3 physical = physicalPosition != null ? physicalPosition : storedPhysicalPosition(level, position, subLevelId);
			Vector3d pointVector = subLevelId == null
					? new Vector3d(physical.x, physical.y, physical.z)
					: new Vector3d(position.getX() + 0.5D, position.getY() + 0.5D, position.getZ() + 0.5D);
			Vector3d placeholder = subLevelId == null ? null : new Vector3d(physical.x, physical.y, physical.z);
			Object pointObject = trackingPointConstructor.newInstance(subLevelId != null, subLevelId, pointer, pointVector, placeholder);
			Object data = getTrackingPointData.invoke(null, serverLevel);
			setTrackingPoint.invoke(data, id, pointObject);
			return id;
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return trackingPointId;
		}
	}

	private static TrackedPosition readTrackingPoint(ServerLevel level, UUID trackingPointId) {
		init();
		if (getTrackingPointData == null || getTrackingPoint == null || point == null || inSubLevel == null) {
			return new TrackedPosition(null, null, trackingPointId, null);
		}
		try {
			Object data = getTrackingPointData.invoke(null, level);
			Object trackingPoint = getTrackingPoint.invoke(data, trackingPointId);
			if (trackingPoint == null) {
				return new TrackedPosition(null, null, trackingPointId, null);
			}
			Vector3d pointVector = (Vector3d) point.invoke(trackingPoint);
			boolean inSub = Boolean.TRUE.equals(inSubLevel.invoke(trackingPoint));
			UUID subLevelId = inSub ? (UUID) subLevelID.invoke(trackingPoint) : null;
			BlockPos position = BlockPos.containing(pointVector.x(), pointVector.y(), pointVector.z());
			Vec3 physicalPosition;
			if (inSub && subLevelId != null) {
				Object subLevel = getSubLevel(level, subLevelId);
				if (subLevel != null) {
					physicalPosition = transformSubLevelPosition(subLevel, new Vec3(pointVector.x(), pointVector.y(), pointVector.z()));
				} else {
					Vector3d placeholder = (Vector3d) globalPlaceholderPosition.invoke(trackingPoint);
					physicalPosition = placeholder == null ? null : new Vec3(placeholder.x(), placeholder.y(), placeholder.z());
				}
			} else {
				physicalPosition = new Vec3(pointVector.x(), pointVector.y(), pointVector.z());
			}
			return new TrackedPosition(position, subLevelId, trackingPointId, physicalPosition);
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return new TrackedPosition(null, null, trackingPointId, null);
		}
	}

	public static Vec3 toLocalPosition(BlockEntity origin, Vec3 physicalPosition) {
		if (origin == null || physicalPosition == null) {
			return physicalPosition;
		}
		init();
		if (helper == null || getContaining == null || logicalPose == null || transformPositionInverse == null) {
			return physicalPosition;
		}
		try {
			Object containing = getContaining.invoke(helper, origin);
			if (containing == null) {
				return physicalPosition;
			}
			Object pose = logicalPose.invoke(containing);
			Object transformed = transformPositionInverse.invoke(pose, physicalPosition);
			return transformed instanceof Vec3 vec ? vec : physicalPosition;
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return physicalPosition;
		}
	}

	public static Vec3 toPhysicalDirection(BlockEntity origin, Vec3 localDirection) {
		return transformDirection(origin, localDirection, transformNormal);
	}

	public static Vec3 toLocalDirection(BlockEntity origin, Vec3 physicalDirection) {
		return transformDirection(origin, physicalDirection, transformNormalInverse);
	}

	private static Vec3 transformDirection(BlockEntity origin, Vec3 direction, Method transformMethod) {
		if (origin == null || direction == null || direction.lengthSqr() < 1.0E-8D) {
			return direction;
		}
		init();
		if (helper == null || getContaining == null || logicalPose == null || transformMethod == null) {
			return direction;
		}
		try {
			Object containing = getContaining.invoke(helper, origin);
			if (containing == null) {
				return direction;
			}
			Object pose = logicalPose.invoke(containing);
			Object transformed = transformMethod.invoke(pose, direction);
			if (transformed instanceof Vec3 vec) {
				return vec;
			}
		} catch (ReflectiveOperationException | RuntimeException ignored) {
		}
		return direction;
	}

	private static @Nullable BlockEntity findBlockEntityInAccess(Level level, @Nullable Object subLevel, BlockPos internalPos) {
		if (subLevel == null) {
			return level.getBlockEntity(internalPos);
		}
		for (BlockEntity blockEntity : getBlockEntities(subLevel)) {
			if (blockEntity != null && !blockEntity.isRemoved() && internalPos.equals(blockEntity.getBlockPos())) {
				return blockEntity;
			}
		}
		return null;
	}

	private static @Nullable Object getSubLevel(Level level, UUID id) {
		if (level == null || id == null) {
			return null;
		}
		init();
		if (subLevelContainerClass == null) {
			return null;
		}
		try {
			Object container = getContainer(level);
			if (container == null) {
				return null;
			}
			return container.getClass().getMethod("getSubLevel", UUID.class).invoke(container, id);
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return null;
		}
	}

	private static @Nullable Object getContainer(Level level) throws ReflectiveOperationException {
		if (subLevelContainerClass == null || level == null) {
			return null;
		}
		for (Method method : subLevelContainerClass.getMethods()) {
			if (!method.getName().equals("getContainer") || !Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1) {
				continue;
			}
			Class<?> parameter = method.getParameterTypes()[0];
			if (parameter.isInstance(level)) {
				return method.invoke(null, level);
			}
		}
		return null;
	}

	private static @Nullable UUID getSubLevelId(@Nullable Object subLevel) {
		if (subLevel == null) {
			return null;
		}
		try {
			Object id = subLevel.getClass().getMethod("getUniqueId").invoke(subLevel);
			return id instanceof UUID uuid ? uuid : null;
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return null;
		}
	}

	private static List<BlockEntity> getBlockEntities(Object subLevel) {
		Map<BlockPos, BlockEntity> blockEntities = new LinkedHashMap<>();
		if (subLevel == null) {
			return List.of();
		}
		try {
			Object plot = subLevel.getClass().getMethod("getPlot").invoke(subLevel);
			collectActorBlockEntities(plot, blockEntities);
			Object loadedChunks = plot.getClass().getMethod("getLoadedChunks").invoke(plot);
			if (!(loadedChunks instanceof Collection<?> chunks)) {
				return new ArrayList<>(blockEntities.values());
			}
			for (Object chunkHolder : chunks) {
				Object chunk = chunkHolder.getClass().getMethod("getChunk").invoke(chunkHolder);
				Object blockEntityMap = chunk.getClass().getMethod("getBlockEntities").invoke(chunk);
				if (!(blockEntityMap instanceof Map<?, ?> entries)) {
					continue;
				}
				for (Object value : entries.values()) {
					if (value instanceof BlockEntity blockEntity) {
						blockEntities.putIfAbsent(blockEntity.getBlockPos().immutable(), blockEntity);
					}
				}
			}
		} catch (ReflectiveOperationException ignored) {
		}
		return new ArrayList<>(blockEntities.values());
	}

	private static void collectActorBlockEntities(Object plot, Map<BlockPos, BlockEntity> blockEntities) throws ReflectiveOperationException {
		Object actors = plot.getClass().getMethod("getBlockEntityActors").invoke(plot);
		if (!(actors instanceof Iterable<?> actorIterable)) {
			return;
		}
		for (Object actor : actorIterable) {
			if (actor instanceof BlockEntity blockEntity) {
				blockEntities.putIfAbsent(blockEntity.getBlockPos().immutable(), blockEntity);
			}
		}
	}
}
