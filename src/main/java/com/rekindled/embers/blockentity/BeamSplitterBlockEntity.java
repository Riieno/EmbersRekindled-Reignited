package com.rekindled.embers.blockentity;

import java.util.HashSet;
import java.util.Random;
import java.util.UUID;

import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.api.power.IEmberPacketProducer;
import com.rekindled.embers.api.power.IEmberPacketReceiver;
import com.rekindled.embers.api.power.ITargetable;
import com.rekindled.embers.api.tile.ISparkable;
import com.rekindled.embers.datagen.EmbersSounds;
import com.rekindled.embers.entity.EmberPacketEntity;
import com.rekindled.embers.compat.sublevel.SubLevelCompat;
import com.rekindled.embers.util.Misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class BeamSplitterBlockEntity extends BlockEntity implements IEmberPacketProducer, ITargetable, IEmberPacketReceiver, ISparkable {

	public BlockPos target1 = null;
	public BlockPos target2 = null;
	public UUID target1SubLevelId = null;
	public UUID target2SubLevelId = null;
	public UUID target1TrackingPointId = null;
	public UUID target2TrackingPointId = null;
	public Vec3 target1PhysicalPosition = null;
	public Vec3 target2PhysicalPosition = null;
	public Random random = new Random();
	public boolean polled = false;
	public HashSet<ChunkPos> trajectoryChunks1 = null;
	public HashSet<ChunkPos> trajectoryChunks2 = null;

	public BeamSplitterBlockEntity(BlockPos pPos, BlockState pBlockState) {
		super(RegistryManager.BEAM_SPLITTER_ENTITY.get(), pPos, pBlockState);
	}

	@Override
	public void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.loadAdditional(nbt, registries);
		if (nbt.contains("target1X")){
			target1 = new BlockPos(nbt.getInt("target1X"), nbt.getInt("target1Y"), nbt.getInt("target1Z"));
		}
		target1SubLevelId = readTargetSubLevelId(nbt, "target1SubLevel");
		target1TrackingPointId = readUuid(nbt, "target1TrackingPoint");
		target1PhysicalPosition = readVec3(nbt, "target1PhysicalX", "target1PhysicalY", "target1PhysicalZ");
		if (nbt.contains("target2X")){
			target2 = new BlockPos(nbt.getInt("target2X"), nbt.getInt("target2Y"), nbt.getInt("target2Z"));
		}
		target2SubLevelId = readTargetSubLevelId(nbt, "target2SubLevel");
		target2TrackingPointId = readUuid(nbt, "target2TrackingPoint");
		target2PhysicalPosition = readVec3(nbt, "target2PhysicalX", "target2PhysicalY", "target2PhysicalZ");
	}

	@Override
	public void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.saveAdditional(nbt, registries);
		if (target1 != null){
			nbt.putInt("target1X", target1.getX());
			nbt.putInt("target1Y", target1.getY());
			nbt.putInt("target1Z", target1.getZ());
		}
		if (target1SubLevelId != null) {
			nbt.putString("target1SubLevel", target1SubLevelId.toString());
		}
		if (target1TrackingPointId != null) {
			nbt.putString("target1TrackingPoint", target1TrackingPointId.toString());
		}
		writeVec3(nbt, target1PhysicalPosition, "target1PhysicalX", "target1PhysicalY", "target1PhysicalZ");
		if (target2 != null){
			nbt.putInt("target2X", target2.getX());
			nbt.putInt("target2Y", target2.getY());
			nbt.putInt("target2Z", target2.getZ());
		}
		if (target2SubLevelId != null) {
			nbt.putString("target2SubLevel", target2SubLevelId.toString());
		}
		if (target2TrackingPointId != null) {
			nbt.putString("target2TrackingPoint", target2TrackingPointId.toString());
		}
		writeVec3(nbt, target2PhysicalPosition, "target2PhysicalX", "target2PhysicalY", "target2PhysicalZ");
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag nbt = super.getUpdateTag(registries);
		if (target1 != null){
			nbt.putInt("target1X", target1.getX());
			nbt.putInt("target1Y", target1.getY());
			nbt.putInt("target1Z", target1.getZ());
		}
		if (target1SubLevelId != null) {
			nbt.putString("target1SubLevel", target1SubLevelId.toString());
		}
		if (target1TrackingPointId != null) {
			nbt.putString("target1TrackingPoint", target1TrackingPointId.toString());
		}
		writeVec3(nbt, target1PhysicalPosition, "target1PhysicalX", "target1PhysicalY", "target1PhysicalZ");
		if (target2 != null){
			nbt.putInt("target2X", target2.getX());
			nbt.putInt("target2Y", target2.getY());
			nbt.putInt("target2Z", target2.getZ());
		}
		if (target2SubLevelId != null) {
			nbt.putString("target2SubLevel", target2SubLevelId.toString());
		}
		if (target2TrackingPointId != null) {
			nbt.putString("target2TrackingPoint", target2TrackingPointId.toString());
		}
		writeVec3(nbt, target2PhysicalPosition, "target2PhysicalX", "target2PhysicalY", "target2PhysicalZ");
		return nbt;
	}

	@Override
	public void setChanged() {
		super.setChanged();
		if (level instanceof ServerLevel)
			((ServerLevel) level).getChunkSource().blockChanged(worldPosition);

		if (trajectoryChunks1 == null)
			trajectoryChunks1 = new HashSet<ChunkPos>();
		if (trajectoryChunks2 == null)
			trajectoryChunks2 = new HashSet<ChunkPos>();

		Axis axis = getBlockState().getValue(BlockStateProperties.AXIS);
		Misc.calculateTrajectoryChunks(trajectoryChunks1, worldPosition, target1, EmberEmitterBlockEntity.getBurstVelocity(Direction.get(AxisDirection.POSITIVE, axis)));
		Misc.calculateTrajectoryChunks(trajectoryChunks2, worldPosition, target2, EmberEmitterBlockEntity.getBurstVelocity(Direction.get(AxisDirection.NEGATIVE, axis)));
	}

	@Override
	public boolean hasRoomFor(double ember) {
		refreshTrackedTargets();
		if (trajectoryChunks1 == null || trajectoryChunks2 == null) {
			Axis axis = getBlockState().getValue(BlockStateProperties.AXIS);
			trajectoryChunks1 = new HashSet<ChunkPos>();
			trajectoryChunks2 = new HashSet<ChunkPos>();
			Misc.calculateTrajectoryChunks(trajectoryChunks1, worldPosition, target1, EmberEmitterBlockEntity.getBurstVelocity(Direction.get(AxisDirection.POSITIVE, axis)));
			Misc.calculateTrajectoryChunks(trajectoryChunks2, worldPosition, target2, EmberEmitterBlockEntity.getBurstVelocity(Direction.get(AxisDirection.NEGATIVE, axis)));
		}
		if (polled)
			return false;
		polled = true;

		if (hasRoomTarget(target1, target1SubLevelId, target1PhysicalPosition, trajectoryChunks1, ember / 2.0) && hasRoomTarget(target2, target2SubLevelId, target2PhysicalPosition, trajectoryChunks2, ember / 2.0)) {
			polled = false;
			return true;
		}
		if (hasRoomTarget(target1, target1SubLevelId, target1PhysicalPosition, trajectoryChunks1, ember)) {
			polled = false;
			return true;
		}
		if (hasRoomTarget(target2, target2SubLevelId, target2PhysicalPosition, trajectoryChunks2, ember)) {
			polled = false;
			return true;
		}
		polled = false;
		return false;
	}

	public boolean hasRoomTarget(BlockPos target, UUID targetSubLevelId, Vec3 targetPhysicalPosition, HashSet<ChunkPos> trajectoryChunks, double ember) {
		BlockEntity targetTile = target == null ? null : SubLevelCompat.findReachableLinkedTarget(this, target, targetSubLevelId, targetPhysicalPosition);
		if (targetTile instanceof IEmberPacketReceiver targetBE) {
			if (!SubLevelCompat.isInSubLevel(this) && !SubLevelCompat.isInSubLevel(targetTile) && level instanceof ServerLevel serverLevel) {
				for (ChunkPos chunk : trajectoryChunks) {
					if (!serverLevel.isNaturalSpawningAllowed(chunk))
						return false;
				}
			}
			return targetBE.hasRoomFor(ember);
		}
		return false;
	}

	@Override
	public void sparkProgress(BlockEntity tile, double ember) {
		split(ember);
	}

	@Override
	public boolean onReceive(EmberPacketEntity packet) {
		if (!getBlockPos().equals(packet.pos)) {
			split(packet.value);
		}
		return true;
	}

	public void split(double ember) {
		refreshTrackedTargets();
		if ((target1 != null || target2 != null) && ember > 0.1) {
			Axis axis = getBlockState().getValue(BlockStateProperties.AXIS);
			if (trajectoryChunks1 == null || trajectoryChunks2 == null) {
				trajectoryChunks1 = new HashSet<ChunkPos>();
				trajectoryChunks2 = new HashSet<ChunkPos>();
				Misc.calculateTrajectoryChunks(trajectoryChunks1, worldPosition, target1, EmberEmitterBlockEntity.getBurstVelocity(Direction.get(AxisDirection.POSITIVE, axis)));
				Misc.calculateTrajectoryChunks(trajectoryChunks2, worldPosition, target2, EmberEmitterBlockEntity.getBurstVelocity(Direction.get(AxisDirection.NEGATIVE, axis)));
			}
			boolean room1 = target1 != null && hasRoomTarget(target1, target1SubLevelId, target1PhysicalPosition, trajectoryChunks1, ember / 2.0);
			boolean room2 = target2 != null && hasRoomTarget(target2, target2SubLevelId, target2PhysicalPosition, trajectoryChunks2, ember / 2.0);
			if (!room1 && !room2) {
				return;
			}
			double value = room1 && room2 ? ember / 2.0 : ember;

			if (room1) {
				EmberPacketEntity packet1 = RegistryManager.EMBER_PACKET.get().create(level);
				Vec3 velocity1 = SubLevelCompat.toPhysicalDirection(this, EmberEmitterBlockEntity.getBurstVelocity(Direction.get(AxisDirection.POSITIVE, axis)));
				Vec3 start = SubLevelCompat.toPhysicalPosition(this, Vec3.atCenterOf(worldPosition));
				Vec3 destination = target1 == null ? start : SubLevelCompat.currentTrackedPhysicalPosition(this, target1, target1SubLevelId, target1PhysicalPosition);
				packet1.initCustom(start, destination, velocity1.x, velocity1.y, velocity1.z, value);
				packet1.pos = getBlockPos().immutable();
				packet1.setTrackedTarget(target1, target1SubLevelId);
				level.addFreshEntity(packet1);
			}
			if (room2) {
				EmberPacketEntity packet2 = RegistryManager.EMBER_PACKET.get().create(level);
				Vec3 velocity2 = SubLevelCompat.toPhysicalDirection(this, EmberEmitterBlockEntity.getBurstVelocity(Direction.get(AxisDirection.NEGATIVE, axis)));
				Vec3 start = SubLevelCompat.toPhysicalPosition(this, Vec3.atCenterOf(worldPosition));
				Vec3 destination = target2 == null ? start : SubLevelCompat.currentTrackedPhysicalPosition(this, target2, target2SubLevelId, target2PhysicalPosition);
				packet2.initCustom(start, destination, velocity2.x, velocity2.y, velocity2.z, value);
				packet2.pos = getBlockPos().immutable();
				packet2.setTrackedTarget(target2, target2SubLevelId);
				level.addFreshEntity(packet2);
			}
			level.playSound(null, worldPosition, ember >= 100 ? EmbersSounds.EMBER_EMIT_BIG.get() : EmbersSounds.EMBER_EMIT.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
		}
	}

	@Override
	public void setTargetPosition(BlockPos pos, Direction side) {
		if (!pos.equals(worldPosition) && side.getAxis() == getBlockState().getValue(BlockStateProperties.AXIS)) {
			if (side.getAxisDirection() == AxisDirection.POSITIVE) {
				target1 = pos;
				target1SubLevelId = null;
				target1TrackingPointId = null;
				target1PhysicalPosition = null;
			} else {
				target2 = pos;
				target2SubLevelId = null;
				target2TrackingPointId = null;
				target2PhysicalPosition = null;
			}
			this.setChanged();
		}
	}

	@Override
	public void setTargetPosition(BlockPos pos, Direction side, BlockEntity targetEntity) {
		if (!pos.equals(worldPosition) && side.getAxis() == getBlockState().getValue(BlockStateProperties.AXIS)) {
			SubLevelCompat.TrackedPosition trackedPosition = SubLevelCompat.captureTrackedTarget(targetEntity,
					side.getAxisDirection() == AxisDirection.POSITIVE ? target1TrackingPointId : target2TrackingPointId);
			if (side.getAxisDirection() == AxisDirection.POSITIVE) {
				target1 = trackedPosition.position() == null ? pos : trackedPosition.position();
				target1SubLevelId = trackedPosition.subLevelId();
				target1TrackingPointId = trackedPosition.trackingPointId();
				target1PhysicalPosition = trackedPosition.physicalPosition();
			} else {
				target2 = trackedPosition.position() == null ? pos : trackedPosition.position();
				target2SubLevelId = trackedPosition.subLevelId();
				target2TrackingPointId = trackedPosition.trackingPointId();
				target2PhysicalPosition = trackedPosition.physicalPosition();
			}
			this.setChanged();
		}
	}

	@Override
	public Vec3 getEmittingDirection(Direction side) {
		if (side.getAxis() == getBlockState().getValue(BlockStateProperties.AXIS)) {
			return EmberEmitterBlockEntity.getBurstVelocity(side);
		}
		return null;
	}

	@Override
	public BlockPos getTarget(Direction side) {
		if (side.getAxis() == getBlockState().getValue(BlockStateProperties.AXIS)) {
			if (side.getAxisDirection() == AxisDirection.POSITIVE)
				return target1;
			return target2;
		}
		return null;
	}

	@Override
	public UUID getTargetSubLevelId(Direction side) {
		if (side.getAxis() == getBlockState().getValue(BlockStateProperties.AXIS)) {
			if (side.getAxisDirection() == AxisDirection.POSITIVE)
				return target1 == null ? null : target1SubLevelId;
			return target2 == null ? null : target2SubLevelId;
		}
		return null;
	}

	private static UUID readTargetSubLevelId(CompoundTag nbt, String key) {
		return readUuid(nbt, key);
	}

	public void refreshTrackedTargets() {
		SubLevelCompat.TrackedPosition tracked1 = SubLevelCompat.refreshTrackedTarget(this, target1, target1SubLevelId, target1TrackingPointId, target1PhysicalPosition);
		SubLevelCompat.TrackedPosition tracked2 = SubLevelCompat.refreshTrackedTarget(this, target2, target2SubLevelId, target2TrackingPointId, target2PhysicalPosition);
		boolean changed = applyTrackedTarget(1, tracked1);
		changed |= applyTrackedTarget(2, tracked2);
		if (changed) {
			this.setChanged();
		}
	}

	private boolean applyTrackedTarget(int targetIndex, SubLevelCompat.TrackedPosition trackedPosition) {
		boolean changed = false;
		if (targetIndex == 1) {
			if (!java.util.Objects.equals(target1, trackedPosition.position())) {
				target1 = trackedPosition.position();
				changed = true;
			}
			if (!java.util.Objects.equals(target1SubLevelId, trackedPosition.subLevelId())) {
				target1SubLevelId = trackedPosition.subLevelId();
				changed = true;
			}
			if (!java.util.Objects.equals(target1TrackingPointId, trackedPosition.trackingPointId())) {
				target1TrackingPointId = trackedPosition.trackingPointId();
				changed = true;
			}
			if (!java.util.Objects.equals(target1PhysicalPosition, trackedPosition.physicalPosition())) {
				target1PhysicalPosition = trackedPosition.physicalPosition();
				changed = true;
			}
			return changed;
		}
		if (!java.util.Objects.equals(target2, trackedPosition.position())) {
			target2 = trackedPosition.position();
			changed = true;
		}
		if (!java.util.Objects.equals(target2SubLevelId, trackedPosition.subLevelId())) {
			target2SubLevelId = trackedPosition.subLevelId();
			changed = true;
		}
		if (!java.util.Objects.equals(target2TrackingPointId, trackedPosition.trackingPointId())) {
			target2TrackingPointId = trackedPosition.trackingPointId();
			changed = true;
		}
		if (!java.util.Objects.equals(target2PhysicalPosition, trackedPosition.physicalPosition())) {
			target2PhysicalPosition = trackedPosition.physicalPosition();
			changed = true;
		}
		return changed;
	}

	private static UUID readUuid(CompoundTag nbt, String key) {
		if (!nbt.contains(key)) {
			return null;
		}
		try {
			return UUID.fromString(nbt.getString(key));
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	private static Vec3 readVec3(CompoundTag nbt, String xKey, String yKey, String zKey) {
		return nbt.contains(xKey) && nbt.contains(yKey) && nbt.contains(zKey)
				? new Vec3(nbt.getDouble(xKey), nbt.getDouble(yKey), nbt.getDouble(zKey))
				: null;
	}

	private static void writeVec3(CompoundTag nbt, Vec3 vec, String xKey, String yKey, String zKey) {
		if (vec == null) {
			return;
		}
		nbt.putDouble(xKey, vec.x);
		nbt.putDouble(yKey, vec.y);
		nbt.putDouble(zKey, vec.z);
	}
}
