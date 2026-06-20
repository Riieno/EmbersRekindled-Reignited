package com.rekindled.embers.blockentity;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.rekindled.embers.Embers;
import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.api.event.EmberEvent;
import com.rekindled.embers.api.power.IEmberCapability;
import com.rekindled.embers.api.power.IEmberPacketProducer;
import com.rekindled.embers.api.power.IEmberPacketReceiver;
import com.rekindled.embers.api.tile.IExtraCapabilityInformation;
import com.rekindled.embers.api.tile.ISparkable;
import com.rekindled.embers.api.tile.IUpgradeable;
import com.rekindled.embers.api.upgrades.UpgradeContext;
import com.rekindled.embers.api.upgrades.UpgradeUtil;
import com.rekindled.embers.damage.DamageEmber;
import com.rekindled.embers.datagen.EmbersDamageTypes;
import com.rekindled.embers.datagen.EmbersSounds;
import com.rekindled.embers.network.PacketHandler;
import com.rekindled.embers.network.message.MessageBeamCannonFX;
import com.rekindled.embers.power.DefaultEmberCapability;
import com.rekindled.embers.compat.sublevel.SubLevelCompat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.rekindled.embers.compat.legacy.capabilities.Capability;
import com.rekindled.embers.compat.legacy.LazyOptional;
import net.neoforged.neoforge.network.PacketDistributor;

public class BeamCannonBlockEntity extends BlockEntity implements IUpgradeable, IEmberPacketProducer, IExtraCapabilityInformation {

	public IEmberCapability capability = new DefaultEmberCapability() {
		@Override
		public void onContentsChanged() {
			super.onContentsChanged();
			BeamCannonBlockEntity.this.setChanged();
		}

		@Override
		public boolean acceptsVolatile() {
			return false;
		}
	};
	public static final double PULL_RATE = 2000.0;
	public static final int FIRE_THRESHOLD = AlchemyTabletBlockEntity.SPARK_THRESHOLD;
	public static final float DAMAGE = 25.0f;
	public static final int MAX_DISTANCE = 64;

	public long ticksExisted = 0;
	public boolean lastPowered = false;
	public Random random = new Random();
	public int offset = random.nextInt(40);
	protected List<UpgradeContext> upgrades = new LinkedList<UpgradeContext>();
	private int mountedFireCooldown;

	public BeamCannonBlockEntity(BlockPos pPos, BlockState pBlockState) {
		super(RegistryManager.BEAM_CANNON_ENTITY.get(), pPos, pBlockState);
		capability.setEmberCapacity(2000);
	}

	public BeamCannonBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
		super(pType, pPos, pBlockState);
	}

	@Override
	public void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.loadAdditional(nbt, registries);
		lastPowered = nbt.getBoolean("lastPowered");
		capability.deserializeNBT(nbt);
	}

	@Override
	public void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.saveAdditional(nbt, registries);
		nbt.putBoolean("lastPowered", lastPowered);
		capability.writeToNBT(nbt);
	}

	public static void clientTick(Level level, BlockPos pos, BlockState state, BeamCannonBlockEntity blockEntity) {
		blockEntity.upgrades = UpgradeUtil.getUpgrades(level, pos, Direction.values());
		UpgradeUtil.verifyUpgrades(blockEntity, blockEntity.upgrades);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, BeamCannonBlockEntity blockEntity) {
		blockEntity.ticksExisted ++;
		if (blockEntity.mountedFireCooldown > 0) {
			blockEntity.mountedFireCooldown--;
		}
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
		blockEntity.upgrades = UpgradeUtil.getUpgrades(level, pos, Direction.values());
		UpgradeUtil.verifyUpgrades(blockEntity, blockEntity.upgrades);
		boolean cancel = UpgradeUtil.doWork(blockEntity, blockEntity.upgrades);
		boolean isPowered = level.hasNeighborSignal(pos);
		boolean redstoneEnabled = UpgradeUtil.getOtherParameter(blockEntity, "redstone_enabled", true, blockEntity.upgrades);
		int threshold = UpgradeUtil.getOtherParameter(blockEntity, "fire_threshold", FIRE_THRESHOLD, blockEntity.upgrades);
		boolean redstoneFiring = !redstoneEnabled || isPowered && !blockEntity.lastPowered;
		if (!cancel && redstoneFiring && blockEntity.capability.getEmber() >= threshold) {
			blockEntity.fire(facing);
		}
		blockEntity.lastPowered = isPowered;
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

	public void fire(Direction facing) {
		Vec3 localRay = Vec3.atLowerCornerOf(facing.getNormal());
		firePhysical(SubLevelCompat.toPhysicalDirection(this, localRay));
	}

	public boolean tryMountedFire() {
		int threshold = UpgradeUtil.getOtherParameter(this, "fire_threshold", FIRE_THRESHOLD, upgrades);
		if (mountedFireCooldown > 0 || capability.getEmber() < threshold) {
			return false;
		}
		mountedFireCooldown = 10;
		fire(getBlockState().getValue(BlockStateProperties.FACING));
		return true;
	}

	private void firePhysical(Vec3 physicalDirection) {
		Vec3 ray = physicalDirection.normalize();
		double damage = UpgradeUtil.getOtherParameter(this, "damage", DAMAGE, upgrades);
		int maxDist = UpgradeUtil.getOtherParameter(this, "distance", MAX_DISTANCE, upgrades);
		Vec3 start = SubLevelCompat.toPhysicalPosition(this, Vec3.atCenterOf(worldPosition));
		Vec3 impactPos = start.add(ray.scale(maxDist));
		for (double distance = 0.75D; distance <= maxDist; distance += 0.25D) {
			Vec3 sample = start.add(ray.scale(distance));
			BlockEntity tile = SubLevelCompat.findAtPhysicalPosition(level, sample);
			if (tile == this) {
				continue;
			}
			BlockState state = SubLevelCompat.findBlockState(level, sample);
			if (sparkTarget(tile)) {
				impactPos = sample;
			} else if (tile instanceof IEmberPacketReceiver) {
				IEmberCapability cap = com.rekindled.embers.util.CapabilityCompat.getCapability(tile, EmbersCapabilities.EMBER_CAPABILITY, null).orElse(null);
				if (cap != null) {
					cap.addAmount(capability.getEmber(), true);
				}
				impactPos = sample;
			} else if (state != null && !state.isAir()
					&& !state.getCollisionShape(level, BlockPos.containing(sample)).isEmpty()) {
				impactPos = sample;
			} else {
				continue;
			}
			level.playSound(null, BlockPos.containing(impactPos), EmbersSounds.BEAM_CANNON_HIT.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
			break;
		}
		List<Entity> entities = level.getEntities((Entity) null, new AABB(start, impactPos).inflate(0.75D), EntitySelector.NO_SPECTATORS);
		for (Entity entity : entities) {
			if (distanceToSegmentSqr(entity.getBoundingBox().getCenter(), start, impactPos) > 0.75D * 0.75D) {
				continue;
			}
			DamageSource damageSource = new DamageEmber(level.registryAccess().registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(EmbersDamageTypes.EMBER_KEY), start);
			entity.hurt(damageSource, (float)damage);
		}

		Vec3 beam = impactPos.subtract(start);
		PacketHandler.sendTrackingChunk((ServerLevel) level, level.getChunkAt(worldPosition).getPos(),
				new MessageBeamCannonFX(start.x, start.y, start.z, beam.x, beam.y, beam.z));

		UpgradeUtil.throwEvent(this, new EmberEvent(this, EmberEvent.EnumType.TRANSFER, this.capability.getEmber()), upgrades);
		this.capability.setEmber(0);
		this.setChanged();

		level.playSound(null, worldPosition, EmbersSounds.BEAM_CANNON_FIRE.get(), SoundSource.BLOCKS, 0.7f, 1.0f);
	}

	private static double distanceToSegmentSqr(Vec3 point, Vec3 start, Vec3 end) {
		Vec3 segment = end.subtract(start);
		double lengthSquared = segment.lengthSqr();
		if (lengthSquared < 1.0E-9D) {
			return point.distanceToSqr(start);
		}
		double progress = Math.max(0.0D, Math.min(1.0D, point.subtract(start).dot(segment) / lengthSquared));
		return point.distanceToSqr(start.add(segment.scale(progress)));
	}

	public boolean sparkTarget(BlockEntity target) {
		if (target instanceof ISparkable) {
			((ISparkable) target).sparkProgress(this, capability.getEmber());
			return true;
		}
		return false;
	}

	@Override
	public boolean isSideUpgradeSlot(Direction face) {
		return true;
	}

	@Override
	public Vec3 getEmittingDirection(Direction side) {
		BlockState state = level.getBlockState(worldPosition);
		if (state.hasProperty(BlockStateProperties.FACING)) {
			Direction facing = state.getValue(BlockStateProperties.FACING);
			return new Vec3(facing.getNormal().getX(), facing.getNormal().getY(), facing.getNormal().getZ());
		}
		return null;
	}

	@Override
	public BlockPos getTarget(Direction side) {
		BlockState state = level.getBlockState(worldPosition);
		if (state.hasProperty(BlockStateProperties.FACING)) {
			Direction facing = state.getValue(BlockStateProperties.FACING);
			if (side != facing)
				return null;
			int maxDist = UpgradeUtil.getOtherParameter(this, "distance", MAX_DISTANCE, upgrades);
			BlockPos hitPos = worldPosition;
			for (int i = 0; i < maxDist; i++) {
				hitPos = hitPos.relative(facing);
				BlockState hitState = SubLevelCompat.findBlockState(this, hitPos);
				BlockEntity tile = SubLevelCompat.findAtPhysicalPosition(this, hitPos);
				if (tile instanceof ISparkable || tile instanceof IEmberPacketReceiver || (hitState != null && !hitState.getCollisionShape(level, hitPos).isEmpty())) {
					return hitPos;
				}
			}
			return hitPos;
		}
		return null;
	}

	@Override
	public void addOtherDescription(List<Component> strings, Direction facing) {
		strings.add(Component.translatable(Embers.MODID + ".tooltip.goggles.redstone_signal"));
	}
}
