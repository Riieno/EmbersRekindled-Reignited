package com.rekindled.embers.compat.aeronautics;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.rekindled.embers.Embers;
import com.rekindled.embers.api.block.IHammerInteraction;
import com.rekindled.embers.api.block.IManualAimController;
import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.api.power.IEmberCapability;
import com.rekindled.embers.api.power.IEmberPacketReceiver;
import com.rekindled.embers.blockentity.BeamCannonBlockEntity;
import com.rekindled.embers.compat.sublevel.SubLevelCompat;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.gui.AllIcons;
import com.rekindled.embers.compat.legacy.capabilities.Capability;
import com.rekindled.embers.compat.legacy.LazyOptional;
import com.rekindled.embers.entity.EmberPacketEntity;
import com.rekindled.embers.power.DefaultEmberCapability;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class CaminiteTurngearBlockEntity extends KineticBlockEntity
		implements IManualAimController, IHammerInteraction, IHaveGoggleInformation, IEmberPacketReceiver {
	public static final double TRACKING_RANGE = 32.0D;
	public static final double EMBER_CAPACITY = 8000.0D;
	public static final int MAX_LINKED_SIGHTSTONES = 32;
	public static final String MOUNT_NAME_PREFIX = "embers_turngear|";

	private final Set<UUID> linkedSightstones = new LinkedHashSet<>();
	private ScrollOptionBehaviour<Mode> mode;
	private EmberSightstoneBlockEntity trackedSightstone;
	private long nextTargetScanTick;
	private UUID trackedSightstoneId;
	private double trackedEmber;
	private double trackedCapacity;
	private int syncedLinkedSightstoneCount;
	private Vec3 beamDirection = new Vec3(0.0D, 1.0D, 0.0D);
	private Vec3 manualDesiredDirection = beamDirection;
	private UUID controllingPlayerId;
	private UUID mountEntityId;
	private final TurngearCannonAssembly mountedAssembly = new TurngearCannonAssembly();
	private UUID mountedSubLevelId;
	private BlockPos mountedLocalPos;
	private boolean mountedAssemblyFaulted;
	private long nextMountedAssemblyTick;
	private final IEmberCapability emberCapability = new DefaultEmberCapability() {
		@Override
		public void onContentsChanged() {
			super.onContentsChanged();
			CaminiteTurngearBlockEntity.this.setChanged();
		}
	};

	public CaminiteTurngearBlockEntity(BlockPos pos, BlockState state) {
		super(AeronauticsCompat.CAMINITE_TURNGEAR_ENTITY.get(), pos, state);
		emberCapability.setEmberCapacity(EMBER_CAPACITY);
	}

	IEmberCapability getEmberCapability() {
		return emberCapability;
	}

	public <T> LazyOptional<T> getCapability(Capability<T> capability, net.minecraft.core.Direction side) {
		if (!isRemoved() && capability == EmbersCapabilities.EMBER_CAPABILITY) {
			return emberCapability instanceof DefaultEmberCapability defaultEmber
					? defaultEmber.getCapability(capability, side) : LazyOptional.empty();
		}
		return LazyOptional.empty();
	}

	public void invalidateCaps() {
		emberCapability.invalidate();
	}

	@Override
	public boolean hasRoomFor(double ember) {
		return emberCapability.getEmber() + Math.max(0.0D, ember) <= emberCapability.getEmberCapacity();
	}

	@Override
	public boolean onReceive(EmberPacketEntity packet) {
		return packet != null && emberCapability.getEmber() < emberCapability.getEmberCapacity();
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		mode = new ScrollOptionBehaviour<>(Mode.class,
				Component.translatable(Embers.MODID + ".turngear.mode"), this,
				new CenteredSideValueBoxTransform((state, side) -> side.getAxis().isHorizontal()));
		mode.withCallback(value -> onModeChanged());
		behaviours.add(mode);
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}
		if (!mountedAssemblyFaulted && mountedSubLevelId != null && !mountedAssembly.hasMountedBlock(this, serverLevel)) {
			mountedAssembly.clearInvalidAssembly(this, serverLevel);
			nextMountedAssemblyTick = level.getGameTime() + 1L;
		}
		if (!mountedAssemblyFaulted && mountedSubLevelId == null && level.getGameTime() >= nextMountedAssemblyTick) {
			try {
				if (!mountedAssembly.assemble(this, serverLevel)) {
					nextMountedAssemblyTick = level.getGameTime() + 10L;
				}
			} catch (RuntimeException error) {
				nextMountedAssemblyTick = level.getGameTime() + 10L;
				Embers.LOGGER.debug("Turngear assembly at {} will be retried", worldPosition, error);
			} catch (LinkageError error) {
				disableMountedAssembly(error);
			}
		}
		validateController(serverLevel);
		if (isManualMode()) {
			trackedSightstone = null;
			trackedSightstoneId = null;
			trackedEmber = 0.0D;
			trackedCapacity = 0.0D;
			if (getSpeed() != 0.0F) {
				beamDirection = turnToward(beamDirection, manualDesiredDirection, getTurnRateDegrees());
			}
		} else if (getSpeed() != 0.0F) {
			trackSightstone();
		} else {
			trackedSightstone = null;
			trackedSightstoneId = null;
			trackedEmber = 0.0D;
			trackedCapacity = 0.0D;
		}
		if (!mountedAssemblyFaulted && mountedSubLevelId != null
				&& level.getGameTime() >= nextMountedAssemblyTick) {
			try {
				boolean aligned = mountedAssembly.aim(this, serverLevel, beamDirection);
				if (aligned && shouldAutoFire()) {
					BeamCannonBlockEntity cannon = mountedAssembly.resolveCannon(this, serverLevel);
					if (cannon != null) {
						cannon.tryMountedFire();
					}
				}
			} catch (RuntimeException error) {
				mountedAssembly.releaseJoint();
				nextMountedAssemblyTick = level.getGameTime() + 10L;
				Embers.LOGGER.debug("Turngear assembly at {} will retry after an aiming failure", worldPosition, error);
			} catch (LinkageError error) {
				disableMountedAssembly(error);
			}
		}
		if (level.getGameTime() % 10L == 0L) {
			sendData();
		}
	}

	private void trackSightstone() {
		Vec3 pivot = mountedAssembly.getAimOrigin(this, (ServerLevel) level);
		if (trackedSightstone != null && !isValidTarget(trackedSightstone, pivot)) {
			trackedSightstone = null;
			nextTargetScanTick = 0L;
		}
		if (level.getGameTime() >= nextTargetScanTick) {
			trackedSightstone = SubLevelCompat
					.findBlockEntitiesInPhysicalRange(this, EmberSightstoneBlockEntity.class, TRACKING_RANGE)
					.stream()
					.filter(sightstone -> isValidTarget(sightstone, pivot))
					.min(Comparator.comparingDouble(sightstone -> pivot.distanceToSqr(sightstone.getTargetPosition())))
					.orElse(null);
			nextTargetScanTick = level.getGameTime() + 10L;
		}
		EmberSightstoneBlockEntity target = trackedSightstone;
		if (target == null) {
			trackedSightstoneId = null;
			trackedEmber = 0.0D;
			trackedCapacity = 0.0D;
			return;
		}
		trackedSightstoneId = target.getSightstoneId();
		trackedEmber = target.getEmber();
		trackedCapacity = target.getEmberCapacity();
		Vec3 targetPosition = target.getTargetPosition();
		Vec3 desired = targetPosition.subtract(pivot);
		if (desired.lengthSqr() > 1.0E-6D) {
			beamDirection = turnToward(beamDirection, desired.normalize(), getTurnRateDegrees());
		}
	}

	private boolean isValidTarget(EmberSightstoneBlockEntity sightstone, Vec3 pivot) {
		return sightstone != null && !sightstone.isRemoved() && sightstone.getFunnel() != null
				&& (getMode() != Mode.LINKED || linkedSightstones.contains(sightstone.getSightstoneId()))
				&& pivot.distanceToSqr(sightstone.getTargetPosition()) <= TRACKING_RANGE * TRACKING_RANGE;
	}

	private double getTurnRateDegrees() {
		return Mth.clamp(Math.abs(getSpeed()) / 24.0D, 0.5D, 8.0D);
	}

	private static Vec3 turnToward(Vec3 current, Vec3 target, double maximumDegrees) {
		Vec3 from = current.normalize();
		Vec3 to = target.normalize();
		double angle = Math.acos(Mth.clamp(from.dot(to), -1.0D, 1.0D));
		if (angle < 1.0E-5D) {
			return to;
		}
		double progress = Math.min(1.0D, Math.toRadians(maximumDegrees) / angle);
		Vec3 blended = from.scale(1.0D - progress).add(to.scale(progress));
		return blended.lengthSqr() < 1.0E-6D ? to : blended.normalize();
	}

	public boolean shouldAutoFire() {
		return !isManualMode() && getSpeed() != 0.0F && trackedSightstoneId != null
				&& trackedCapacity > 0.0D && trackedEmber < trackedCapacity;
	}

	public boolean isManualMode() {
		return getMode() == Mode.MANUAL;
	}

	public Mode getMode() {
		return mode == null ? Mode.AUTO_DETECT : mode.get();
	}

	@Override
	public AABB createRenderBoundingBox() {
		return new AABB(worldPosition).expandTowards(0.0D, 2.0D, 0.0D).inflate(1.0D);
	}

	public void tryMount(Player player) {
		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer) || !isManualMode()) {
			return;
		}
		validateController(serverLevel);
		if (controllingPlayerId != null && !controllingPlayerId.equals(player.getUUID())) {
			player.displayClientMessage(Component.translatable(Embers.MODID + ".turngear.occupied"), true);
			return;
		}
		ArmorStand mount = getMountEntity(serverLevel);
		if (mount == null) {
			Vec3 position = getMountWorldPosition();
			mount = new ArmorStand(serverLevel, position.x, position.y, position.z);
			mount.setNoGravity(true);
			mount.setInvulnerable(true);
			mount.setNoBasePlate(true);
			mount.setSilent(true);
			mount.setInvisible(true);
			mount.setCustomName(Component.literal(createMountName()));
			mount.setCustomNameVisible(false);
			if (!serverLevel.addFreshEntity(mount)) {
				return;
			}
			mountEntityId = mount.getUUID();
		}
		controllingPlayerId = player.getUUID();
		manualDesiredDirection = player.getLookAngle().normalize();
		serverPlayer.startRiding(mount, true);
		setChanged();
		sendData();
	}

	private String createMountName() {
		UUID subLevelId = SubLevelCompat.getContainingSubLevelId(this);
		return MOUNT_NAME_PREFIX + (subLevelId == null ? "world" : subLevelId) + "|" + worldPosition.asLong();
	}

	@Override
	public void handleManualControl(ServerPlayer player, int action, float pitch, float yaw) {
		if (!isManualMode() || getSpeed() == 0.0F || !player.getUUID().equals(controllingPlayerId)
				|| !Float.isFinite(pitch) || !Float.isFinite(yaw)) {
			return;
		}
		ArmorStand mount = level instanceof ServerLevel serverLevel ? getMountEntity(serverLevel) : null;
		if (mount == null || player.getVehicle() != mount) {
			clearController(player);
			return;
		}
		Vec3 cameraTarget = player.getEyePosition().add(Vec3.directionFromRotation(pitch, yaw)
				.scale(BeamCannonBlockEntity.MAX_DISTANCE));
		Vec3 pivot = mountedAssembly.getAimOrigin(this, (ServerLevel) level);
		Vec3 desired = cameraTarget.subtract(pivot);
		if (desired.lengthSqr() > 1.0E-6D) {
			manualDesiredDirection = desired.normalize();
		}
		if (action == IManualAimController.FIRE) {
			BeamCannonBlockEntity cannon = mountedAssembly.resolveCannon(this, (ServerLevel) level);
			if (cannon != null) {
				cannon.tryMountedFire();
			}
		}
	}

	private void validateController(ServerLevel serverLevel) {
		if (controllingPlayerId == null) {
			discardUnusedMount(serverLevel);
			return;
		}
		ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(controllingPlayerId);
		ArmorStand mount = getMountEntity(serverLevel);
		if (player == null || mount == null || player.getVehicle() != mount || !isManualMode()) {
			clearController(player);
			return;
		}
		Vec3 position = getMountWorldPosition();
		mount.teleportTo(position.x, position.y, position.z);
	}

	private Vec3 getMountWorldPosition() {
		Vec3 pivot = SubLevelCompat.toPhysicalPosition(this, Vec3.atCenterOf(worldPosition.above()));
		Vec3 horizontal = new Vec3(beamDirection.x, 0.0D, beamDirection.z);
		if (horizontal.lengthSqr() < 1.0E-6D) {
			horizontal = new Vec3(0.0D, 0.0D, 1.0D);
		}
		return pivot.subtract(horizontal.normalize().scale(0.55D)).add(0.0D, -1.75D, 0.0D);
	}

	private ArmorStand getMountEntity(ServerLevel serverLevel) {
		Entity entity = mountEntityId == null ? null : serverLevel.getEntity(mountEntityId);
		return entity instanceof ArmorStand armorStand && !armorStand.isRemoved() ? armorStand : null;
	}

	private void discardUnusedMount(ServerLevel serverLevel) {
		ArmorStand mount = getMountEntity(serverLevel);
		if (mount != null && mount.getPassengers().isEmpty()) {
			mount.discard();
			mountEntityId = null;
		}
	}

	private void clearController(ServerPlayer player) {
		if (player != null && player.getVehicle() != null) {
			player.stopRiding();
		}
		if (level instanceof ServerLevel serverLevel) {
			ArmorStand mount = getMountEntity(serverLevel);
			if (mount != null) {
				mount.ejectPassengers();
				mount.discard();
			}
		}
		controllingPlayerId = null;
		mountEntityId = null;
		setChanged();
	}

	private void onModeChanged() {
		trackedSightstone = null;
		nextTargetScanTick = 0L;
		trackedSightstoneId = null;
		trackedEmber = 0.0D;
		trackedCapacity = 0.0D;
		if (!isManualMode()) {
			clearController(null);
		}
	}

	@Override
	public InteractionResult onHammerUse(ItemStack hammer, UseOnContext context) {
		if (level == null) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}
		Player player = context.getPlayer();
		if (getMode() != Mode.LINKED) {
			if (player != null) {
				player.displayClientMessage(Component.translatable(Embers.MODID + ".turngear.linked_mode_required"), true);
			}
			return InteractionResult.SUCCESS;
		}
		TurngearHammerData.store(hammer, this);
		if (player != null) {
			player.displayClientMessage(Component.translatable(Embers.MODID + ".turngear.selected"), true);
		}
		return InteractionResult.SUCCESS;
	}

	public void toggleSightstone(UUID sightstoneId, Player player) {
		if (getMode() != Mode.LINKED) {
			player.displayClientMessage(Component.translatable(Embers.MODID + ".turngear.linked_mode_required"), true);
			return;
		}
		boolean added;
		if (linkedSightstones.remove(sightstoneId)) {
			added = false;
		} else if (linkedSightstones.size() >= MAX_LINKED_SIGHTSTONES) {
			player.displayClientMessage(Component.translatable(Embers.MODID + ".turngear.link_limit", MAX_LINKED_SIGHTSTONES), true);
			return;
		} else {
			linkedSightstones.add(sightstoneId);
			added = true;
		}
		syncedLinkedSightstoneCount = linkedSightstones.size();
		setChanged();
		sendData();
		player.displayClientMessage(Component.translatable(
				added ? Embers.MODID + ".turngear.link_added" : Embers.MODID + ".turngear.link_removed",
				linkedSightstones.size(), MAX_LINKED_SIGHTSTONES), true);
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.putDouble("beamX", beamDirection.x);
		tag.putDouble("beamY", beamDirection.y);
		tag.putDouble("beamZ", beamDirection.z);
		tag.putDouble("trackedEmber", trackedEmber);
		tag.putDouble("trackedCapacity", trackedCapacity);
		if (trackedSightstoneId != null) {
			tag.putUUID("trackedSightstone", trackedSightstoneId);
		}
		tag.putInt("linkedSightstoneCount", linkedSightstones.size());
		emberCapability.writeToNBT(tag);
		if (mountedSubLevelId != null) {
			tag.putUUID("mountedSubLevel", mountedSubLevelId);
		}
		if (mountedLocalPos != null) {
			tag.putLong("mountedLocalPos", mountedLocalPos.asLong());
		}
		if (!clientPacket) {
			ListTag linked = new ListTag();
			for (UUID id : linkedSightstones) {
				linked.add(StringTag.valueOf(id.toString()));
			}
			tag.put("linkedSightstones", linked);
		}
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		if (tag.contains("beamX") && tag.contains("beamY") && tag.contains("beamZ")) {
			beamDirection = new Vec3(tag.getDouble("beamX"), tag.getDouble("beamY"), tag.getDouble("beamZ")).normalize();
			manualDesiredDirection = beamDirection;
		}
		trackedEmber = tag.getDouble("trackedEmber");
		trackedCapacity = tag.getDouble("trackedCapacity");
		trackedSightstoneId = tag.hasUUID("trackedSightstone") ? tag.getUUID("trackedSightstone") : null;
		syncedLinkedSightstoneCount = tag.getInt("linkedSightstoneCount");
		emberCapability.deserializeNBT(tag);
		mountedSubLevelId = tag.hasUUID("mountedSubLevel") ? tag.getUUID("mountedSubLevel")
				: tag.hasUUID("cannonSubLevel") ? tag.getUUID("cannonSubLevel") : null;
		mountedLocalPos = tag.contains("mountedLocalPos") ? BlockPos.of(tag.getLong("mountedLocalPos"))
				: tag.contains("cannonLocalPos") ? BlockPos.of(tag.getLong("cannonLocalPos")) : null;
		if (tag.contains("linkedSightstones", Tag.TAG_LIST)) {
			linkedSightstones.clear();
			ListTag linked = tag.getList("linkedSightstones", Tag.TAG_STRING);
			for (int i = 0; i < linked.size() && linkedSightstones.size() < MAX_LINKED_SIGHTSTONES; i++) {
				try {
					linkedSightstones.add(UUID.fromString(linked.getString(i)));
				} catch (IllegalArgumentException ignored) {
				}
			}
		}
	}

	@Override
	public void destroy() {
		clearController(null);
		try {
			if (level instanceof ServerLevel serverLevel) {
				mountedAssembly.disassemble(this, serverLevel);
			} else {
				mountedAssembly.releaseJoint();
			}
		} catch (RuntimeException | LinkageError error) {
			Embers.LOGGER.error("Failed to disassemble the block mounted at {}", worldPosition, error);
		}
		super.destroy();
	}

	private void disableMountedAssembly(Throwable error) {
		mountedAssemblyFaulted = true;
		try {
			mountedAssembly.releaseJoint();
		} catch (RuntimeException | LinkageError ignored) {
		}
		Embers.LOGGER.error("Disabled the turngear mount at {} after an Aeronautics assembly failure", worldPosition, error);
	}

	UUID getMountedSubLevelId() {
		return mountedSubLevelId;
	}

	BlockPos getMountedLocalPos() {
		return mountedLocalPos;
	}

	void setMountedAssembly(UUID subLevelId, BlockPos localPos) {
		mountedSubLevelId = subLevelId;
		mountedLocalPos = localPos == null ? null : localPos.immutable();
		setChanged();
		sendData();
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		tooltip.add(Component.translatable(Embers.MODID + ".turngear.mode_value",
				Component.translatable(getMode().getTranslationKey())).withStyle(ChatFormatting.GRAY));
		if (getMode() == Mode.LINKED) {
			tooltip.add(Component.translatable(Embers.MODID + ".turngear.link_count",
					level != null && level.isClientSide ? syncedLinkedSightstoneCount : linkedSightstones.size(),
					MAX_LINKED_SIGHTSTONES).withStyle(ChatFormatting.DARK_GRAY));
		}
		if (trackedSightstoneId != null) {
			tooltip.add(Component.translatable(Embers.MODID + ".turngear.ember_status",
					Mth.floor(trackedEmber), Mth.floor(trackedCapacity)).withStyle(ChatFormatting.GOLD));
		} else if (!isManualMode()) {
			tooltip.add(Component.translatable(Embers.MODID + ".turngear.no_target").withStyle(ChatFormatting.DARK_GRAY));
		}
		return true;
	}

	public enum Mode implements INamedIconOptions {
		AUTO_DETECT(AllIcons.I_TARGET, "auto_detect"),
		MANUAL(AllIcons.I_MTD_USER_MODE, "manual"),
		LINKED(AllIcons.I_ATTACHED, "linked");

		private final AllIcons icon;
		private final String name;

		Mode(AllIcons icon, String name) {
			this.icon = icon;
			this.name = name;
		}

		@Override
		public AllIcons getIcon() {
			return icon;
		}

		@Override
		public String getTranslationKey() {
			return Embers.MODID + ".turngear.mode." + name;
		}
	}
}
