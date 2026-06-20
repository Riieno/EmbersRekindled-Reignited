package com.rekindled.embers.compat.aeronautics;

import java.util.UUID;

import com.rekindled.embers.Embers;
import com.rekindled.embers.api.block.IHammerInteraction;
import com.rekindled.embers.blockentity.EmberFunnelBlockEntity;
import com.rekindled.embers.compat.sublevel.SubLevelCompat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class EmberSightstoneBlockEntity extends BlockEntity implements IHammerInteraction {
	private UUID sightstoneId = UUID.randomUUID();

	public EmberSightstoneBlockEntity(BlockPos pos, BlockState state) {
		super(AeronauticsCompat.EMBER_SIGHTSTONE_ENTITY.get(), pos, state);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		if (tag.hasUUID("sightstoneId")) {
			sightstoneId = tag.getUUID("sightstoneId");
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putUUID("sightstoneId", sightstoneId);
	}

	public UUID getSightstoneId() {
		return sightstoneId;
	}

	public EmberFunnelBlockEntity getFunnel() {
		if (level == null || !getBlockState().hasProperty(EmberSightstoneBlock.FACING)) {
			return null;
		}
		BlockEntity support = SubLevelCompat.findAdjacent(this, getBlockState().getValue(EmberSightstoneBlock.FACING).getOpposite());
		return support instanceof EmberFunnelBlockEntity funnel ? funnel : null;
	}

	public Vec3 getTargetPosition() {
		EmberFunnelBlockEntity funnel = getFunnel();
		if (funnel == null) {
			return SubLevelCompat.toPhysicalPosition(this, Vec3.atCenterOf(worldPosition));
		}
		Vec3 receivingFace = Vec3.atCenterOf(funnel.getBlockPos()).add(Vec3.atLowerCornerOf(
				funnel.getBlockState().getValue(BlockStateProperties.FACING).getNormal()).scale(0.45D));
		return SubLevelCompat.toPhysicalPosition(funnel, receivingFace);
	}

	public double getEmber() {
		EmberFunnelBlockEntity funnel = getFunnel();
		return funnel == null ? 0.0D : funnel.capability.getEmber();
	}

	public double getEmberCapacity() {
		EmberFunnelBlockEntity funnel = getFunnel();
		return funnel == null ? 0.0D : funnel.capability.getEmberCapacity();
	}

	@Override
	public InteractionResult onHammerUse(ItemStack hammer, UseOnContext context) {
		if (level == null) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}
		CaminiteTurngearBlockEntity turngear = TurngearHammerData.resolve(hammer, level);
		if (turngear == null) {
			if (context.getPlayer() != null) {
				context.getPlayer().displayClientMessage(Component.translatable(Embers.MODID + ".turngear.no_selection"), true);
			}
			return InteractionResult.SUCCESS;
		}
		if (context.getPlayer() != null) {
			turngear.toggleSightstone(sightstoneId, context.getPlayer());
		}
		return InteractionResult.SUCCESS;
	}
}
