package com.rekindled.embers.compat.aeronautics;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import com.rekindled.embers.blockentity.BeamCannonBlockEntity;
import com.rekindled.embers.compat.sublevel.SubLevelCompat;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.simulated_team.simulated.util.SimAssemblyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;

final class TurngearCannonAssembly {
	private static final Set<ConstraintJointAxis> MOUNT_JOINT_AXES = EnumSet.allOf(ConstraintJointAxis.class);

	private GenericConstraintHandle joint;
	private ServerSubLevel jointParent;
	private ServerSubLevel jointChild;

	boolean assemble(CaminiteTurngearBlockEntity turngear, ServerLevel level) {
		Vec3 mountPosition = SubLevelCompat.toPhysicalPosition(turngear,
				Vec3.atCenterOf(turngear.getBlockPos().above()));
		BlockEntity adjacent = SubLevelCompat.findAtPhysicalPosition(level, mountPosition);
		if (adjacent != null) {
			SubLevel mountedSubLevel = Sable.HELPER.getContaining(adjacent);
			SubLevel turngearSubLevel = Sable.HELPER.getContaining(turngear);
			if (mountedSubLevel instanceof ServerSubLevel child && mountedSubLevel != turngearSubLevel) {
				turngear.setMountedAssembly(child.getUniqueId(), adjacent.getBlockPos());
				return true;
			}
		}

		BlockPos mountedPos = turngear.getBlockPos().above();
		if (level.getBlockState(mountedPos).isAir()) {
			return false;
		}
		Set<BlockPos> mountedBlock = Set.of(mountedPos);
		ServerSubLevel child = SubLevelAssemblyHelper.assembleBlocks(level, mountedPos, mountedBlock,
				BoundingBox3i.from(mountedBlock));
		BlockPos childPos = child.getPlot().getCenterBlock();
		if (child.getPlot().getEmbeddedLevelAccessor().getBlockState(BlockPos.ZERO).isAir()) {
			return false;
		}
		turngear.setMountedAssembly(child.getUniqueId(), childPos);
		return true;
	}

	BeamCannonBlockEntity resolveCannon(CaminiteTurngearBlockEntity turngear, ServerLevel level) {
		BlockEntity blockEntity = resolveMountedBlockEntity(turngear, level);
		return blockEntity instanceof BeamCannonBlockEntity cannon ? cannon : null;
	}

	Vec3 getAimOrigin(CaminiteTurngearBlockEntity turngear, ServerLevel level) {
		BlockEntity mountedBlock = resolveMountedBlockEntity(turngear, level);
		return mountedBlock == null
				? SubLevelCompat.toPhysicalPosition(turngear, Vec3.atCenterOf(turngear.getBlockPos().above()))
				: SubLevelCompat.toPhysicalPosition(mountedBlock, Vec3.atCenterOf(mountedBlock.getBlockPos()));
	}

	boolean hasMountedBlock(CaminiteTurngearBlockEntity turngear, ServerLevel level) {
		return resolveMountedState(turngear, level) != null;
	}

	void clearInvalidAssembly(CaminiteTurngearBlockEntity turngear, ServerLevel level) {
		ServerSubLevel child = findChild(turngear, level);
		BlockPos mountedPos = turngear.getMountedLocalPos();
		releaseJoint();
		if (child != null && !child.isRemoved()) {
			boolean mountedBlockMissing = mountedPos == null
					|| child.getPlot().getEmbeddedLevelAccessor()
							.getBlockState(mountedPos.subtract(child.getPlot().getCenterBlock())).isAir();
			if (mountedBlockMissing) {
				ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
				if (container != null) {
					container.removeSubLevel(child, SubLevelRemovalReason.REMOVED);
				}
			}
		}
		turngear.setMountedAssembly(null, null);
	}

	boolean aim(CaminiteTurngearBlockEntity turngear, ServerLevel level, Vec3 physicalDirection) {
		ServerSubLevel child = resolveChild(turngear, level);
		BlockPos mountedPos = turngear.getMountedLocalPos();
		BlockState mountedState = resolveMountedState(turngear, level);
		if (child == null || mountedPos == null || mountedState == null || physicalDirection.lengthSqr() < 1.0E-6D) {
			releaseJoint();
			return false;
		}
		ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
		if (container == null) {
			return false;
		}
		SubLevel containing = Sable.HELPER.getContaining(turngear);
		ServerSubLevel parent = containing instanceof ServerSubLevel serverSubLevel ? serverSubLevel : null;
		PhysicsPipeline pipeline = container.physicsSystem().getPipeline();

		Direction facing = getMountedFacing(mountedState);
		Vector3d localFacing = new Vector3d(facing.getStepX(), facing.getStepY(), facing.getStepZ());
		Vector3d desiredWorld = getAimDirection(physicalDirection);
		Quaterniond parentOrientation = parent == null
				? new Quaterniond()
				: new Quaterniond(parent.logicalPose().orientation());
		Vector3d desiredParent = new Quaterniond(parentOrientation).conjugate()
				.transform(new Vector3d(desiredWorld)).normalize();
		Quaterniond tilt = new Quaterniond().rotationTo(new Vector3d(0.0D, 1.0D, 0.0D), desiredParent);
		Quaterniond relativeOrientation = new Quaterniond(Direction.UP.getRotation())
				.mul(tilt)
				.mul(new Quaterniond(facing.getRotation()).conjugate());
		Quaterniond desiredOrientation = new Quaterniond(parentOrientation).mul(relativeOrientation);

		ensureJoint(turngear, mountedPos, pipeline, parent, child, relativeOrientation);
		retargetJoint(turngear, mountedPos, relativeOrientation);
		pipeline.teleport(child, child.logicalPose().position(), desiredOrientation);
		child.updateLastPose();

		Vector3d actualDirection = desiredOrientation.transform(new Vector3d(localFacing)).normalize();
		return actualDirection.dot(desiredWorld) > 0.999D;
	}

	private Vector3d getAimDirection(Vec3 physicalDirection) {
		Vector3d direction = new Vector3d(physicalDirection.x, physicalDirection.y, physicalDirection.z).normalize();
		double horizontalLength = Math.hypot(direction.x, direction.z);
		if (horizontalLength < 1.0E-8D) {
			return new Vector3d(0.0D, Math.copySign(1.0D, direction.y), 0.0D);
		}
		double pitch = Math.atan2(direction.y, horizontalLength);
		double horizontalScale = Math.cos(pitch) / horizontalLength;
		return new Vector3d(direction.x * horizontalScale, Math.sin(pitch),
				direction.z * horizontalScale).normalize();
	}

	private Direction getMountedFacing(BlockState state) {
		if (state.hasProperty(BlockStateProperties.FACING)) {
			return state.getValue(BlockStateProperties.FACING);
		}
		if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
		}
		if (state.hasProperty(BlockStateProperties.AXIS)) {
			return Direction.get(Direction.AxisDirection.POSITIVE, state.getValue(BlockStateProperties.AXIS));
		}
		if (state.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
			return Direction.get(Direction.AxisDirection.POSITIVE, state.getValue(BlockStateProperties.HORIZONTAL_AXIS));
		}
		return Direction.UP;
	}

	private void ensureJoint(CaminiteTurngearBlockEntity turngear, BlockPos mountedPos,
			PhysicsPipeline pipeline, ServerSubLevel parent, ServerSubLevel child, Quaterniond relativeOrientation) {
		if (joint != null && joint.isValid() && jointParent == parent && jointChild == child) {
			return;
		}
		releaseJoint();
		joint = pipeline.addConstraint(parent, child, new GenericConstraintConfiguration(
				getBaseAnchor(turngear), getChildAnchor(mountedPos), relativeOrientation,
				new Quaterniond(), MOUNT_JOINT_AXES));
		if (joint != null) {
			joint.setContactsEnabled(false);
			jointParent = parent;
			jointChild = child;
		}
	}

	private void retargetJoint(CaminiteTurngearBlockEntity turngear, BlockPos mountedPos,
			Quaterniond relativeOrientation) {
		if (joint == null || !joint.isValid()) {
			return;
		}
		joint.setFrame1(getBaseAnchor(turngear), relativeOrientation);
		joint.setFrame2(getChildAnchor(mountedPos), new Quaterniond());
		joint.setContactsEnabled(false);
	}

	private Vector3d getBaseAnchor(CaminiteTurngearBlockEntity turngear) {
		return new Vector3d(turngear.getBlockPos().getX() + 0.5D,
				turngear.getBlockPos().getY() + 1.0D, turngear.getBlockPos().getZ() + 0.5D);
	}

	private Vector3d getChildAnchor(BlockPos mountedPos) {
		return new Vector3d(mountedPos.getX() + 0.5D, mountedPos.getY(), mountedPos.getZ() + 0.5D);
	}

	void disassemble(CaminiteTurngearBlockEntity turngear, ServerLevel level) {
		ServerSubLevel child = resolveChild(turngear, level);
		BlockPos localPos = turngear.getMountedLocalPos();
		releaseJoint();
		if (child != null && localPos != null && !child.isRemoved()) {
			ServerSubLevel parent = Sable.HELPER.getContaining(turngear) instanceof ServerSubLevel serverParent
					? serverParent : null;
			Quaterniond orientation = parent == null ? new Quaterniond() : new Quaterniond(parent.logicalPose().orientation());
			ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
			if (container != null) {
				container.physicsSystem().getPipeline().teleport(child, child.logicalPose().position(), orientation);
				child.updateLastPose();
			}
			SimAssemblyHelper.disassembleSubLevel(level, child, localPos, turngear.getBlockPos().above(), Rotation.NONE, true);
		}
		turngear.setMountedAssembly(null, null);
	}

	void releaseJoint() {
		if (joint != null && joint.isValid()) {
			joint.remove();
		}
		joint = null;
		jointParent = null;
		jointChild = null;
	}

	private BlockState resolveMountedState(CaminiteTurngearBlockEntity turngear, ServerLevel level) {
		ServerSubLevel child = resolveChild(turngear, level);
		BlockPos localPos = turngear.getMountedLocalPos();
		if (child == null || localPos == null) {
			return null;
		}
		BlockPos relativePos = localPos.subtract(child.getPlot().getCenterBlock());
		BlockState state = child.getPlot().getEmbeddedLevelAccessor().getBlockState(relativePos);
		return state.isAir() ? null : state;
	}

	private BlockEntity resolveMountedBlockEntity(CaminiteTurngearBlockEntity turngear, ServerLevel level) {
		ServerSubLevel child = resolveChild(turngear, level);
		BlockPos localPos = turngear.getMountedLocalPos();
		if (child == null || localPos == null) {
			return null;
		}
		BlockPos relativePos = localPos.subtract(child.getPlot().getCenterBlock());
		BlockEntity blockEntity = child.getPlot().getEmbeddedLevelAccessor().getBlockEntity(relativePos);
		return blockEntity != null && Sable.HELPER.getContaining(blockEntity) == child ? blockEntity : null;
	}

	private ServerSubLevel resolveChild(CaminiteTurngearBlockEntity turngear, ServerLevel level) {
		ServerSubLevel child = findChild(turngear, level);
		return child != null && !child.isRemoved() ? child : null;
	}

	private ServerSubLevel findChild(CaminiteTurngearBlockEntity turngear, ServerLevel level) {
		UUID id = turngear.getMountedSubLevelId();
		SubLevelContainer container = SubLevelContainer.getContainer(level);
		SubLevel subLevel = id == null || container == null ? null : container.getSubLevel(id);
		return subLevel instanceof ServerSubLevel serverSubLevel ? serverSubLevel : null;
	}
}
