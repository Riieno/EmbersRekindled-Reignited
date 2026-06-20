package com.rekindled.embers.blockentity;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.rekindled.embers.ConfigManager;
import com.rekindled.embers.Embers;
import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.api.power.IEmberCapability;
import com.rekindled.embers.api.power.IEmberPacketProducer;
import com.rekindled.embers.api.power.IEmberPacketReceiver;
import com.rekindled.embers.api.power.ITargetable;
import com.rekindled.embers.api.tile.IExtraCapabilityInformation;
import com.rekindled.embers.datagen.EmbersSounds;
import com.rekindled.embers.entity.EmberPacketEntity;
import com.rekindled.embers.power.DefaultEmberCapability;
import com.rekindled.embers.compat.sublevel.SubLevelCompat;
import com.rekindled.embers.util.Misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import com.rekindled.embers.compat.legacy.capabilities.Capability;
import com.rekindled.embers.compat.legacy.LazyOptional;

public class EmberEmitterBlockEntity extends BlockEntity implements IEmberPacketProducer, ITargetable, IExtraCapabilityInformation {

	public IEmberCapability capability = new DefaultEmberCapability() {
		@Override
		public void onContentsChanged() {
			super.onContentsChanged();
			EmberEmitterBlockEntity.this.setChanged();
		}

		@Override
		public boolean acceptsVolatile() {
			return false;
		}
	};

	public static final double TRANSFER_RATE = 40.0;
	public static final double PULL_RATE = 10.0;

	public BlockPos target = null;
	public UUID targetSubLevelId = null;
	public UUID targetTrackingPointId = null;
	public Vec3 targetPhysicalPosition = null;
	public boolean rangeLimitedEndpoint;
	public long ticksExisted = 0;
	public Random random = new Random();
	public int offset = random.nextInt(40);
	public HashSet<ChunkPos> trajectoryChunks = null;

	public EmberEmitterBlockEntity(BlockPos pPos, BlockState pBlockState) {
		super(RegistryManager.EMBER_EMITTER_ENTITY.get(), pPos, pBlockState);
		capability.setEmberCapacity(200);
		capability.setEmber(0);
	}

	public EmberEmitterBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
		super(pType, pPos, pBlockState);
	}

	@Override
	public void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.loadAdditional(nbt, registries);
		if (nbt.contains("targetX")){
			target = new BlockPos(nbt.getInt("targetX"), nbt.getInt("targetY"), nbt.getInt("targetZ"));
		}
		targetSubLevelId = readTargetSubLevelId(nbt);
		targetTrackingPointId = readUuid(nbt, "targetTrackingPoint");
		targetPhysicalPosition = readVec3(nbt, "targetPhysicalX", "targetPhysicalY", "targetPhysicalZ");
		rangeLimitedEndpoint = nbt.getBoolean("rangeLimitedEndpoint");
		capability.deserializeNBT(nbt);
	}

	@Override
	public void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.saveAdditional(nbt, registries);
		if (target != null){
			nbt.putInt("targetX", target.getX());
			nbt.putInt("targetY", target.getY());
			nbt.putInt("targetZ", target.getZ());
		}
		if (targetSubLevelId != null) {
			nbt.putString("targetSubLevel", targetSubLevelId.toString());
		}
		if (targetTrackingPointId != null) {
			nbt.putString("targetTrackingPoint", targetTrackingPointId.toString());
		}
		writeVec3(nbt, targetPhysicalPosition, "targetPhysicalX", "targetPhysicalY", "targetPhysicalZ");
		nbt.putBoolean("rangeLimitedEndpoint", rangeLimitedEndpoint);
		capability.writeToNBT(nbt);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag nbt = super.getUpdateTag(registries);
		if (target != null){
			nbt.putInt("targetX", target.getX());
			nbt.putInt("targetY", target.getY());
			nbt.putInt("targetZ", target.getZ());
		}
		if (targetSubLevelId != null) {
			nbt.putString("targetSubLevel", targetSubLevelId.toString());
		}
		if (targetTrackingPointId != null) {
			nbt.putString("targetTrackingPoint", targetTrackingPointId.toString());
		}
		writeVec3(nbt, targetPhysicalPosition, "targetPhysicalX", "targetPhysicalY", "targetPhysicalZ");
		nbt.putBoolean("rangeLimitedEndpoint", rangeLimitedEndpoint);
		return nbt;
	}

	@Override
	public void setChanged() {
		super.setChanged();
		if (level instanceof ServerLevel)
			((ServerLevel) level).getChunkSource().blockChanged(worldPosition);
		if (trajectoryChunks == null)
			trajectoryChunks = new HashSet<ChunkPos>();
		Misc.calculateTrajectoryChunks(trajectoryChunks, worldPosition, target, getEmittingDirection(level.getBlockState(worldPosition).getValue(BlockStateProperties.FACING)));
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, EmberEmitterBlockEntity blockEntity) {
		blockEntity.ticksExisted ++;
		Direction facing = state.getValue(BlockStateProperties.FACING);
		BlockEntity attachedTile = SubLevelCompat.findAdjacent(blockEntity, facing.getOpposite());
		if (blockEntity.ticksExisted % 5 == 0 && attachedTile != null) {
			IEmberCapability cap = com.rekindled.embers.util.CapabilityCompat.getCapability(attachedTile, EmbersCapabilities.EMBER_CAPABILITY, facing).orElse(null);
			if (cap != null) {
				if (cap.getEmber() > 0 && blockEntity.capability.getEmber() < blockEntity.capability.getEmberCapacity()){
					double removed = cap.removeAmount(PULL_RATE, true);
					blockEntity.capability.addAmount(removed, true);
				}
			}
		}
		if ((blockEntity.ticksExisted + blockEntity.offset) % 20 == 0 && blockEntity.canSendBurst() && blockEntity.capability.getEmber() > PULL_RATE) {
			BlockEntity targetTile = SubLevelCompat.findReachableLinkedTarget(blockEntity, blockEntity.target, blockEntity.targetSubLevelId, blockEntity.targetPhysicalPosition);
			if (targetTile instanceof IEmberPacketReceiver) {
				if (((IEmberPacketReceiver) targetTile).hasRoomFor(TRANSFER_RATE)) {
					EmberPacketEntity packet = RegistryManager.EMBER_PACKET.get().create(blockEntity.level);
					Vec3 velocity = SubLevelCompat.toPhysicalDirection(blockEntity, getBurstVelocity(facing));
					Vec3 start = SubLevelCompat.toPhysicalPosition(blockEntity, Vec3.atCenterOf(pos));
					Vec3 destination = SubLevelCompat.currentTrackedPhysicalPosition(blockEntity, blockEntity.target, blockEntity.targetSubLevelId, blockEntity.targetPhysicalPosition);
					packet.initCustom(start, destination, velocity.x, velocity.y, velocity.z, Math.min(TRANSFER_RATE, blockEntity.capability.getEmber()));
					packet.pos = blockEntity.getBlockPos().immutable();
					packet.setTrackedTarget(blockEntity.target, blockEntity.targetSubLevelId);
					blockEntity.capability.removeAmount(Math.min(TRANSFER_RATE, blockEntity.capability.getEmber()), true);
					blockEntity.level.addFreshEntity(packet);
					level.playSound(null, pos, EmbersSounds.EMBER_EMIT.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
				}
			}
		}
	}

	public boolean canSendBurst() {
		validateRangeLimitedLink();
		if (!ConfigManager.isRedstoneControlActive(level, worldPosition) || target == null || level.isClientSide) {
			return false;
		}
		BlockEntity targetTile = SubLevelCompat.findReachableLinkedTarget(this, target, targetSubLevelId, targetPhysicalPosition);
		if (isRangeLimitedEndpoint(targetTile)) {
			rangeLimitedEndpoint = true;
		}
		if (targetTile == null) {
			if (rangeLimitedEndpoint && SubLevelCompat.isCrossSubLevelLink(this, targetSubLevelId)) {
				clearTarget();
			}
			return false;
		}
		if (!SubLevelCompat.isInSubLevel(this) && !SubLevelCompat.isInSubLevel(targetTile)) {
			if (!level.isLoaded(target)) {
				return false;
			}
			if (trajectoryChunks == null) {
				trajectoryChunks = new HashSet<ChunkPos>();
				Misc.calculateTrajectoryChunks(trajectoryChunks, worldPosition, target, getEmittingDirection(level.getBlockState(worldPosition).getValue(BlockStateProperties.FACING)));
			}
			if (level instanceof ServerLevel serverLevel) {
				for (ChunkPos chunk : trajectoryChunks) {
					if (!serverLevel.isNaturalSpawningAllowed(chunk))
						return false;
				}
			}
		}
		return true;
	}

	public void validateRangeLimitedLink() {
		if (target == null || level == null || level.isClientSide) {
			return;
		}
		refreshTrackedTarget();
		BlockEntity targetTile = SubLevelCompat.findReachableLinkedTarget(this, target, targetSubLevelId, targetPhysicalPosition);
		if (isRangeLimitedEndpoint(targetTile)) {
			rangeLimitedEndpoint = true;
		}
		if (targetTile == null && rangeLimitedEndpoint && SubLevelCompat.isCrossSubLevelLink(this, targetSubLevelId)) {
			clearTarget();
		}
	}

	public static Vec3 getBurstVelocity(Direction facing) {
		switch(facing) {
		case DOWN:
			return new Vec3(0, -0.5, 0);
		case UP:
			return new Vec3(0, 0.5, 0);
		case NORTH:
			return new Vec3(0, -0.01, -0.5);
		case SOUTH:
			return new Vec3(0, -0.01, 0.5);
		case WEST:
			return new Vec3(-0.5, -0.01, 0);
		case EAST:
			return new Vec3(0.5, -0.01, 0);
		default:
			return Vec3.ZERO;
		}
	}

	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if (!this.isRemoved() && cap == EmbersCapabilities.EMBER_CAPABILITY && level.getBlockState(this.getBlockPos()).getValue(BlockStateProperties.FACING) != side) {
			return capability.getCapability(cap, side);
		}
		return LazyOptional.empty();
	}

	public void invalidateCaps() {
		
		capability.invalidate();
	}

	@Override
	public void setTargetPosition(BlockPos pos, Direction side) {
		target = pos;
		targetSubLevelId = null;
		targetTrackingPointId = null;
		targetPhysicalPosition = null;
		rangeLimitedEndpoint = false;
		this.setChanged();
	}

	@Override
	public void setTargetPosition(BlockPos pos, Direction side, BlockEntity targetEntity) {
		SubLevelCompat.TrackedPosition trackedPosition = SubLevelCompat.captureTrackedTarget(targetEntity, targetTrackingPointId);
		target = trackedPosition.position() == null ? pos : trackedPosition.position();
		targetSubLevelId = trackedPosition.subLevelId();
		targetTrackingPointId = trackedPosition.trackingPointId();
		targetPhysicalPosition = trackedPosition.physicalPosition();
		rangeLimitedEndpoint = isRangeLimitedEndpoint(targetEntity);
		this.setChanged();
	}

	private static boolean isRangeLimitedEndpoint(BlockEntity targetEntity) {
		return targetEntity instanceof EmberFunnelBlockEntity
				|| targetEntity != null && targetEntity.getType() == RegistryManager.EMBER_RECEIVER_ENTITY.get();
	}

	private void clearTarget() {
		target = null;
		targetSubLevelId = null;
		targetTrackingPointId = null;
		targetPhysicalPosition = null;
		rangeLimitedEndpoint = false;
		setChanged();
	}

	@Override
	public Vec3 getEmittingDirection(Direction side) {
		return getBurstVelocity(level.getBlockState(worldPosition).getValue(BlockStateProperties.FACING));
	}

	@Override
	public BlockPos getTarget(Direction side) {
		BlockState state = level.getBlockState(worldPosition);
		if (state.hasProperty(BlockStateProperties.FACING)) {
			Direction facing = state.getValue(BlockStateProperties.FACING);
			if (side != facing)
				return null;
		}
		return target;
	}

	@Override
	public UUID getTargetSubLevelId(Direction side) {
		return getTarget(side) == null ? null : targetSubLevelId;
	}

	private static UUID readTargetSubLevelId(CompoundTag nbt) {
		return readUuid(nbt, "targetSubLevel");
	}

	public void refreshTrackedTarget() {
		SubLevelCompat.TrackedPosition trackedPosition = SubLevelCompat.refreshTrackedTarget(this, target, targetSubLevelId, targetTrackingPointId, targetPhysicalPosition);
		boolean changed = false;
		if (!java.util.Objects.equals(target, trackedPosition.position())) {
			target = trackedPosition.position();
			changed = true;
		}
		if (!java.util.Objects.equals(targetSubLevelId, trackedPosition.subLevelId())) {
			targetSubLevelId = trackedPosition.subLevelId();
			changed = true;
		}
		if (!java.util.Objects.equals(targetTrackingPointId, trackedPosition.trackingPointId())) {
			targetTrackingPointId = trackedPosition.trackingPointId();
			changed = true;
		}
		if (!java.util.Objects.equals(targetPhysicalPosition, trackedPosition.physicalPosition())) {
			targetPhysicalPosition = trackedPosition.physicalPosition();
			changed = true;
		}
		if (changed) {
			this.setChanged();
		}
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

	@Override
	public void addOtherDescription(List<Component> strings, Direction facing) {
		strings.add(Component.translatable(Embers.MODID + ".tooltip.goggles.redstone_signal"));
	}
}
