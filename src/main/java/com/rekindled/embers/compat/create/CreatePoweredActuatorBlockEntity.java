package com.rekindled.embers.compat.create;

import com.rekindled.embers.ConfigManager;
import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.api.tile.IMechanicalPowerProvider;
import com.rekindled.embers.compat.legacy.LazyOptional;
import com.rekindled.embers.compat.legacy.capabilities.Capability;
import com.rekindled.embers.upgrade.ActuatorUpgrade;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class CreatePoweredActuatorBlockEntity extends KineticBlockEntity implements IMechanicalPowerProvider {

    private final ActuatorUpgrade upgrade;

    public CreatePoweredActuatorBlockEntity(BlockPos pos, BlockState state) {
        super(CreateCompat.CREATE_POWERED_ACTUATOR_ENTITY.get(), pos, state);
        this.upgrade = new ActuatorUpgrade(this);
    }

    @Override
    public double getMechanicalPower() {
        return this.getSpeed() * ConfigManager.KINETIC_ACTUATOR_RATIO.get();
    }

	public Direction getShaftSide() {
		return getBlockState().getValue(CreatePoweredActuatorBlock.FACING);
	}

	public Direction getUpgradeSide() {
		return getShaftSide().getOpposite();
	}

	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if (isRemoved()) {
			return LazyOptional.empty();
		}
		if (cap == EmbersCapabilities.UPGRADE_PROVIDER_CAPABILITY && isUpgradeProviderSide(side)) {
			return upgrade.getCapability(cap, side);
		}
		return LazyOptional.empty();
	}

	public void invalidateCaps() {
		upgrade.invalidate();
	}

	private boolean isUpgradeProviderSide(Direction side) {
		return side == null || side == getUpgradeSide();
	}
    
}
