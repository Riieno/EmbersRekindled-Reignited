package com.rekindled.embers.mixin;

import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.api.power.IEmberCapability;
import com.rekindled.embers.util.CapabilityCompat;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public abstract class EmberBlockEntityPersistenceMixin {

	@Inject(method = "loadWithComponents", at = @At("TAIL"))
	private void embers$restoreEmberAfterReconstruction(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo callback) {
		if (!tag.contains(IEmberCapability.EMBER) && !tag.contains(IEmberCapability.EMBER_CAPACITY)) {
			return;
		}
		BlockEntity blockEntity = (BlockEntity) (Object) this;
		IEmberCapability capability = CapabilityCompat.getCapability(blockEntity, EmbersCapabilities.EMBER_CAPABILITY).orElse(null);
		if (capability != null) {
			capability.deserializeNBT(tag);
		}
	}
}
