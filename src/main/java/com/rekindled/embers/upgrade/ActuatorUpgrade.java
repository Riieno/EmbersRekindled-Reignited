package com.rekindled.embers.upgrade;

import java.text.DecimalFormat;
import java.util.List;

import com.rekindled.embers.Embers;
import com.rekindled.embers.api.event.DialInformationEvent;
import com.rekindled.embers.api.event.UpgradeEvent;
import com.rekindled.embers.api.tile.IMechanicalPowerProvider;
import com.rekindled.embers.api.tile.IMechanicallyPowered;
import com.rekindled.embers.api.upgrades.UpgradeContext;
import com.rekindled.embers.block.EmberDialBlock;
import com.rekindled.embers.util.DecimalFormats;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ActuatorUpgrade extends DefaultUpgradeProvider {
    public ActuatorUpgrade(BlockEntity tile) {
        super(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "actuator"), tile);
    }

    @Override
    public int getLimit(BlockEntity tile) {
        return tile instanceof IMechanicallyPowered ? 1 : 1;
    }

    @Override
    public double getSpeed(BlockEntity tile, double speed, int distance, int count) {
        double power = getPower();
        if(tile instanceof IMechanicallyPowered) {
            IMechanicallyPowered mechTile = (IMechanicallyPowered) tile;
            if(power > mechTile.getMinimumPower() && power <= mechTile.getMaximumPower())
                return mechTile.getMechanicalSpeed(power) * speed;
        }
        return speed;
    }

    @Override
    public double transformEmberConsumption(BlockEntity tile, double ember, int distance, int count) {
        if(tile instanceof IMechanicallyPowered) {
            return ((IMechanicallyPowered) tile).getStandardPowerRatio() * ember;
        }
        return ember;
    }

    @Override
    public double transformEmberProduction(BlockEntity tile, double ember, int distance, int count) {
        double power = getPower();
        if(power > 15)
            return ember * 1.5;
        else
            return ember;
    }

    @Override
    public double getOtherParameter(BlockEntity tile, String type, double value, int distance, int count) {
        if(tile instanceof IMechanicallyPowered) {
            if(type.equals("fuel_consumption"))
                return ((IMechanicallyPowered) tile).getStandardPowerRatio() * value;
        }
        return value;
    }

    @Override
    public boolean doWork(BlockEntity tile, List<UpgradeContext> upgrades, int distance, int count) {
        double power = getPower();
        if(tile instanceof IMechanicallyPowered) {
            IMechanicallyPowered mechTile = (IMechanicallyPowered) tile;
            return !(power > mechTile.getMinimumPower() && power <= mechTile.getMaximumPower());
        }
        return false;
    }

    @Override
    public void throwEvent(BlockEntity tile, List<UpgradeContext> upgrades, UpgradeEvent event, int distance, int count) {
        if(event instanceof DialInformationEvent) {
            DialInformationEvent dialEvent = (DialInformationEvent) event;
            if(EmberDialBlock.DIAL_TYPE.equals(dialEvent.getDialType())) {
                DecimalFormat multiplierFormat = DecimalFormats.getDecimalFormat("embers.decimal_format.speed_multiplier");
                if (tile instanceof IMechanicallyPowered) {
                    IMechanicallyPowered mechTile = (IMechanicallyPowered) tile;
                    double power = getPower();
                    double speedModifier = mechTile.getMechanicalSpeed(power) / mechTile.getNominalSpeed();
                    dialEvent.getInformation().add(Component.translatable("embers.tooltip.upgrade.actuator", multiplierFormat.format(speedModifier))); //Proxy this because it runs in shared code
                } else {
                    double power = getPower();
                    double productionModifier = power > 15 ? 1.5 : 1.0;
                    dialEvent.getInformation().add(Component.translatable("embers.tooltip.upgrade.actuator.other", multiplierFormat.format(productionModifier)));
                }
            }
        }
    }

    private double getPower() {
        return this.tile instanceof IMechanicalPowerProvider powerProvider ? powerProvider.getMechanicalPower() : 0;
    }
}
