package com.rekindled.embers.blockentity;

import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.api.power.IEmberCapability;
import com.rekindled.embers.compat.create.CreateBlazeBurnerHelper;
import com.rekindled.embers.compat.create.EmberFueledBlazeBurner;
import com.rekindled.embers.datagen.EmbersSounds;
import com.rekindled.embers.entity.EmberPacketEntity;
import com.rekindled.embers.particle.SmokeParticleOptions;
import com.rekindled.embers.particle.SparkParticleOptions;
import com.rekindled.embers.particle.StarParticleOptions;
import com.rekindled.embers.compat.sublevel.SubLevelCompat;
import com.rekindled.embers.util.EmbersColors;
import com.rekindled.embers.util.SubLevelParticleUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EmberFunnelBlockEntity extends EmberReceiverBlockEntity {

	public static final int TRANSFER_RATE = 100;

	public EmberFunnelBlockEntity(BlockPos pPos, BlockState pBlockState) {
		super(RegistryManager.EMBER_FUNNEL_ENTITY.get(), pPos, pBlockState);
		capability.setEmberCapacity(2000);
		capability.setEmber(0);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, EmberFunnelBlockEntity blockEntity) {
		blockEntity.ticksExisted ++;
		Direction facing = state.getValue(BlockStateProperties.FACING);
		BlockEntity attachedTile = SubLevelCompat.findAdjacent(blockEntity, facing.getOpposite());
		if (blockEntity.ticksExisted % 2 == 0 && attachedTile != null){
			if (attachedTile instanceof EmberFueledBlazeBurner) {
				transferToBlazeBurners(level, attachedTile.getBlockPos(), blockEntity);
				return;
			}
			IEmberCapability cap = com.rekindled.embers.util.CapabilityCompat.getCapability(attachedTile, EmbersCapabilities.EMBER_CAPABILITY, facing).orElse(null);
			if (cap != null) {
				if (cap.getEmber() < cap.getEmberCapacity() && blockEntity.capability.getEmber() > 0){
					double added = cap.addAmount(Math.min(TRANSFER_RATE, blockEntity.capability.getEmber()), true);
					blockEntity.capability.removeAmount(added, true);
				}
			}
		}
	}

	private static void transferToBlazeBurners(Level level, BlockPos attachedPos, EmberFunnelBlockEntity blockEntity) {
		if (blockEntity.capability.getEmber() <= 0) {
			return;
		}

		List<EmberFueledBlazeBurner> burners = new ArrayList<>(CreateBlazeBurnerHelper.getConnectedBurners(level, attachedPos));
		if (burners.isEmpty()) {
			return;
		}

		double remainingTransfer = Math.min(TRANSFER_RATE, blockEntity.capability.getEmber());
		double transferred = 0;
		List<EmberFueledBlazeBurner> touchedBurners = new ArrayList<>();

		while (remainingTransfer > 0) {
			burners.sort(Comparator.comparingDouble(burner -> burner.embers$getEmberFuel().getEmber()));
			EmberFueledBlazeBurner target = null;
			for (EmberFueledBlazeBurner burner : burners) {
				if (burner.embers$getEmberFuel().getEmber() < burner.embers$getEmberFuel().getEmberCapacity()) {
					target = burner;
					break;
				}
			}
			if (target == null) {
				break;
			}

			double added = target.embers$addEmber(remainingTransfer, false);
			if (added <= 0) {
				break;
			}

			transferred += added;
			remainingTransfer -= added;
			if (!touchedBurners.contains(target)) {
				touchedBurners.add(target);
			}
		}

		if (transferred > 0) {
			blockEntity.capability.removeAmount(transferred, true);
			for (EmberFueledBlazeBurner burner : touchedBurners) {
				burner.embers$refreshEmberFuelState(false);
			}
		}
	}

	@Override
	public boolean onReceive(EmberPacketEntity packet) {
		if (level instanceof ServerLevel serverLevel) {
			Direction facing = getBlockState().getValue(BlockStateProperties.FACING);
			double offX = 0.5 + facing.getStepX() * 0.45;
			double offY = 0.5 + facing.getStepY() * 0.45;
			double offZ = 0.5 + facing.getStepZ() * 0.45;
			if (capability.getEmber() + packet.value > capability.getEmberCapacity()) {
				SubLevelParticleUtil.send(this, new SparkParticleOptions(EmbersColors.EMBER_ID, random.nextFloat() * 0.75f + 0.45f), getBlockPos().getX() + offX, getBlockPos().getY() + offY, getBlockPos().getZ() + offZ, 5, 0.125f * (random.nextFloat() - 0.5f), 0.125f * (random.nextFloat()), 0.125f * (random.nextFloat() - 0.5f), 1.0);
				SubLevelParticleUtil.send(this, new SmokeParticleOptions(EmbersColors.SMOKE_ID, 2.0f + random.nextFloat() * 2.0f), getBlockPos().getX() + offX, getBlockPos().getY() + offY, getBlockPos().getZ() + offZ, 15, 0.0625f * (random.nextFloat() - 0.5f), 0.0625f + 0.0625f * (random.nextFloat() - 0.5f), 0.0625f * (random.nextFloat() - 0.5f), 1.0);
			} else {
				SubLevelParticleUtil.send(this, new StarParticleOptions(EmbersColors.EMBER_ID, 3.5f + 0.5f * random.nextFloat()), getBlockPos().getX() + offX, getBlockPos().getY() + offY, getBlockPos().getZ() + offZ, 12, 0.0125f * (random.nextFloat() - 0.5f), 0.0125f * (random.nextFloat() - 0.5f), 0.0125f * (random.nextFloat() - 0.5f), 0.0);
			}
		}
		level.playLocalSound(packet.getX(), packet.getY(), packet.getZ(), packet.value >= 100 ? EmbersSounds.EMBER_RECEIVE_BIG.get() : EmbersSounds.EMBER_RECEIVE.get(), SoundSource.BLOCKS, 1.0f, 1.0f, false);
		return true;
	}
}
