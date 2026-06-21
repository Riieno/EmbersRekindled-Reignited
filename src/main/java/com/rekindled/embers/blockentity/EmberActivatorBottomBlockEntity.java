package com.rekindled.embers.blockentity;

import java.util.ArrayList;
import java.util.List;

import com.rekindled.embers.Embers;
import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.api.event.DialInformationEvent;
import com.rekindled.embers.api.event.EmberEvent;
import com.rekindled.embers.api.tile.IExtraCapabilityInformation;
import com.rekindled.embers.api.tile.IExtraDialInformation;
import com.rekindled.embers.api.tile.IUpgradeable;
import com.rekindled.embers.api.upgrades.UpgradeContext;
import com.rekindled.embers.api.upgrades.UpgradeUtil;
import com.rekindled.embers.datagen.EmbersSounds;
import com.rekindled.embers.particle.GlowParticleOptions;
import com.rekindled.embers.particle.SmokeParticleOptions;
import com.rekindled.embers.recipe.IEmberActivationRecipe;
import com.rekindled.embers.recipe.SingleItemContainer;
import com.rekindled.embers.util.EmbersColors;
import com.rekindled.embers.util.Misc;
import com.rekindled.embers.util.SubLevelParticleUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.rekindled.embers.compat.legacy.capabilities.Capability;
import com.rekindled.embers.compat.legacy.capabilities.ForgeCapabilities;
import com.rekindled.embers.compat.legacy.LazyOptional;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

public class EmberActivatorBottomBlockEntity extends BlockEntity implements IExtraDialInformation, IExtraCapabilityInformation, IUpgradeable {

	public static final int PROCESS_TIME = 40;
	int progress = -1;
	public ItemStackHandler inventory = new ItemStackHandler(1) {
		@Override
		protected void onContentsChanged(int slot) {
			EmberActivatorBottomBlockEntity.this.setChanged();
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			// Accept items - serverTick() will validate if they have recipes
			return super.insertItem(slot, stack, simulate);
		}
	};
	public LazyOptional<IItemHandler> holder = LazyOptional.of(() -> inventory);
	protected List<UpgradeContext> upgrades = new ArrayList<>();
	public IEmberActivationRecipe cachedRecipe = null;

	public EmberActivatorBottomBlockEntity(BlockPos pPos, BlockState pBlockState) {
		super(RegistryManager.EMBER_ACTIVATOR_BOTTOM_ENTITY.get(), pPos, pBlockState);
	}

	@Override
	public void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.loadAdditional(nbt, registries);
		inventory.deserializeNBT(registries, nbt.getCompound("inventory"));
		progress = nbt.getInt("progress");
	}

	@Override
	public void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.saveAdditional(nbt, registries);
		nbt.put("inventory", inventory.serializeNBT(registries));
		nbt.putInt("progress", progress);
	}

	public static void clientTick(Level level, BlockPos pos, BlockState state, EmberActivatorBottomBlockEntity blockEntity) {
		blockEntity.upgrades = UpgradeUtil.getUpgrades(level, pos, Misc.horizontals);
		UpgradeUtil.verifyUpgrades(blockEntity, blockEntity.upgrades);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, EmberActivatorBottomBlockEntity blockEntity) {
		blockEntity.upgrades = UpgradeUtil.getUpgrades(level, pos, Misc.horizontals);
		UpgradeUtil.verifyUpgrades(blockEntity, blockEntity.upgrades);
		if(UpgradeUtil.doTick(blockEntity, blockEntity.upgrades))
			return;

		if (!blockEntity.inventory.getStackInSlot(0).isEmpty()) {
			BlockEntity tile = level.getBlockEntity(pos.above());
			boolean cancel = UpgradeUtil.doWork(blockEntity, blockEntity.upgrades);

			if (!cancel && tile instanceof EmberActivatorTopBlockEntity) {
				EmberActivatorTopBlockEntity top = (EmberActivatorTopBlockEntity) tile;
				blockEntity.progress++;
				if (blockEntity.progress > UpgradeUtil.getWorkTime(blockEntity, PROCESS_TIME, blockEntity.upgrades)) {
					blockEntity.progress = 0;
					if (blockEntity.inventory != null) {
						RecipeWrapper wrapper = new RecipeWrapper(blockEntity.inventory);
						blockEntity.cachedRecipe = Misc.getRecipe(blockEntity.cachedRecipe, RegistryManager.EMBER_ACTIVATION.get(), wrapper, level);
						if (blockEntity.cachedRecipe != null) {
							double emberValue = blockEntity.cachedRecipe.getOutput(wrapper);
							double ember = UpgradeUtil.getTotalEmberProduction(blockEntity, emberValue, blockEntity.upgrades);
							if ((ember > 0 || emberValue == 0) && top.capability.getEmber() + ember <= top.capability.getEmberCapacity()) {
								level.playSound(null, pos, EmbersSounds.ACTIVATOR.get(), SoundSource.BLOCKS, 1.0f, 1.0f);

								if (level instanceof ServerLevel serverLevel) {
									SubLevelParticleUtil.send(blockEntity, new GlowParticleOptions(EmbersColors.EMBER_ID, new Vec3(0, 0.65f, 0), 4.7f), pos.getX() + 0.5f, pos.getY() + 1.5f, pos.getZ() + 0.5f, 80, 0.1, 0.1, 0.1, 1.0);
									SubLevelParticleUtil.send(blockEntity, SmokeParticleOptions.BIG_SMOKE, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 20, 0.1, 0.1, 0.1, 1.0);
								}
								UpgradeUtil.throwEvent(blockEntity, new EmberEvent(blockEntity, EmberEvent.EnumType.PRODUCE, ember), blockEntity.upgrades);
								top.capability.addAmount(ember, true);

								//the recipe is responsible for taking items from the inventory
								blockEntity.cachedRecipe.process(wrapper);
							}
						}
					}
				}
				blockEntity.setChanged();
			}
		}
	}

	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER) {
			return ForgeCapabilities.ITEM_HANDLER.orEmpty(cap, holder);
		}
		return LazyOptional.empty();
	}
	
	// Direct method for NeoForge's capability provider lambda
	public IItemHandler getItemHandler(Direction side) {
		if (!this.isRemoved() && side != Direction.DOWN) {
			return inventory;
		}
		return null;
	}

	public void invalidateCaps() {
		
		holder.invalidate();
	}

	@Override
	public boolean hasCapabilityDescription(Capability<?> capability) {
		return capability == ForgeCapabilities.ITEM_HANDLER;
	}

	@Override
	public void addCapabilityDescription(List<Component> strings, Capability<?> capability, Direction facing) {
		if (capability == ForgeCapabilities.ITEM_HANDLER)
			strings.add(IExtraCapabilityInformation.formatCapability(EnumIOType.INPUT, Embers.MODID + ".tooltip.goggles.item", Component.translatable(Embers.MODID + ".tooltip.goggles.item.ember")));
	}

	@Override
	public void addDialInformation(Direction facing, List<Component> information, String dialType) {
		UpgradeUtil.throwEvent(this, new DialInformationEvent(this, information, dialType), upgrades);
	}

	@Override
	public boolean isSideUpgradeSlot(Direction face) {
		return face.getAxis() != Axis.Y;
	}
}
