package com.rekindled.embers.blockentity;

import java.util.Random;

import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.api.power.IEmberCapability;
import com.rekindled.embers.api.power.IEmberPacketReceiver;
import com.rekindled.embers.api.tile.IEmberInputHint;
import com.rekindled.embers.datagen.EmbersBlockTags;
import com.rekindled.embers.datagen.EmbersSounds;
import com.rekindled.embers.entity.EmberPacketEntity;
import com.rekindled.embers.particle.SmokeParticleOptions;
import com.rekindled.embers.particle.SparkParticleOptions;
import com.rekindled.embers.particle.StarParticleOptions;
import com.rekindled.embers.power.DefaultEmberCapability;
import com.rekindled.embers.util.EmbersColors;
import com.rekindled.embers.util.SubLevelParticleUtil;
import com.rekindled.embers.compat.sublevel.SubLevelCompat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import com.rekindled.embers.compat.legacy.capabilities.Capability;
import com.rekindled.embers.compat.legacy.LazyOptional;

public class EmberReceiverBlockEntity extends BlockEntity implements IEmberPacketReceiver, IEmberInputHint {

	public IEmberCapability capability = new DefaultEmberCapability() {
		@Override
		public void onContentsChanged() {
			super.onContentsChanged();
			EmberReceiverBlockEntity.this.setChanged();
		}

		@Override
		public boolean acceptsVolatile() {
			return false;
		}
	};

	public static final int TRANSFER_RATE = 32;

	public long ticksExisted = 0;
	public Random random = new Random();

	public EmberReceiverBlockEntity(BlockPos pPos, BlockState pBlockState) {
		super(RegistryManager.EMBER_RECEIVER_ENTITY.get(), pPos, pBlockState);
		capability.setEmberCapacity(2000);
	}

	public EmberReceiverBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
		super(pType, pPos, pBlockState);
	}

	@Override
	public void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.loadAdditional(nbt, registries);
		capability.deserializeNBT(nbt);
	}

	@Override
	public void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.saveAdditional(nbt, registries);
		capability.writeToNBT(nbt);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, EmberReceiverBlockEntity blockEntity) {
		blockEntity.ticksExisted ++;
		Direction facing = state.getValue(BlockStateProperties.FACING);
		BlockEntity attachedTile = SubLevelCompat.findAdjacent(blockEntity, facing.getOpposite());
		if (attachedTile != null){
			IEmberCapability cap = com.rekindled.embers.util.CapabilityCompat.getCapability(attachedTile, EmbersCapabilities.EMBER_CAPABILITY, facing).orElse(null);
			if (cap != null) {
				if (cap.getEmber() < cap.getEmberCapacity() && blockEntity.capability.getEmber() > 0){
					double added = cap.addAmount(Math.min(TRANSFER_RATE, blockEntity.capability.getEmber()), true);
					blockEntity.capability.removeAmount(added, true);
				}
			}
		}
	}

	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if (!this.isRemoved() && cap == EmbersCapabilities.EMBER_CAPABILITY) {
			return capability.getCapability(cap, side);
		}
		return LazyOptional.empty();
	}

	/*@Override
	public boolean isFull() {
		return capability.getEmber() >= capability.getEmberCapacity();
	}*/

	@Override
	public boolean hasRoomFor(double ember) {
		return capability.getEmber() * 2 <= capability.getEmberCapacity();
	}

	@Override
	public boolean onReceive(EmberPacketEntity packet) {
		if (level instanceof ServerLevel serverLevel) {
			if (capability.getEmber() + packet.value > capability.getEmberCapacity()) {
				SubLevelParticleUtil.send(this, new SparkParticleOptions(EmbersColors.EMBER_ID, random.nextFloat() * 0.75f + 0.45f), getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5, 5, 0.125f * (random.nextFloat() - 0.5f), 0.125f * (random.nextFloat()), 0.125f * (random.nextFloat() - 0.5f), 1.0);
				SubLevelParticleUtil.send(this, new SmokeParticleOptions(EmbersColors.SMOKE_ID, 2.0f + random.nextFloat() * 2.0f), getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5, 15, 0.0625f * (random.nextFloat() - 0.5f), 0.0625f + 0.0625f * (random.nextFloat() - 0.5f), 0.0625f * (random.nextFloat() - 0.5f), 1.0);
			} else {
				SubLevelParticleUtil.send(this, new StarParticleOptions(EmbersColors.EMBER_ID, 3.5f + 0.5f * random.nextFloat()), getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5, 12, 0.0125f * (random.nextFloat() - 0.5f), 0.0125f * (random.nextFloat() - 0.5f), 0.0125f * (random.nextFloat() - 0.5f), 0.0);
			}
		}
		level.playLocalSound(packet.getX(), packet.getY(), packet.getZ(), packet.value >= 100 ? EmbersSounds.EMBER_RECEIVE_BIG.get() : EmbersSounds.EMBER_RECEIVE.get(), SoundSource.BLOCKS, 1.0f, 1.0f, false);
		return true;
	}

	public void invalidateCaps() {
		
		capability.invalidate();
	}

	@Override
	public boolean shouldShowHintTooltip() {
		Direction facing = getBlockState().getValue(BlockStateProperties.FACING);
		BlockPos attachedPos = worldPosition.relative(facing, -1);
		BlockState attachedState = SubLevelCompat.findBlockState(this, attachedPos);
		if (attachedState != null && attachedState.is(EmbersBlockTags.EMBER_WRONG_INPUT_HINTER)) {
			BlockEntity entity = SubLevelCompat.findAdjacent(this, facing.getOpposite());
			return entity == null || !com.rekindled.embers.util.CapabilityCompat.getCapability(entity, EmbersCapabilities.EMBER_CAPABILITY, facing).isPresent();
		}
		return false;
	}
}
