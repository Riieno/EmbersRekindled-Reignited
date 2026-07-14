package com.rekindled.embers.compat.create;

import com.rekindled.embers.ConfigManager;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CreatePoweredActuatorBlock extends DirectionalKineticBlock implements IBE<CreatePoweredActuatorBlockEntity> {

    public CreatePoweredActuatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    public Direction getUpgradeSide(BlockState state) {
		return state.getValue(FACING).getOpposite();
	}

	@Override
	public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
		return face == state.getValue(FACING);
	}

	@Override
	public Direction.Axis getRotationAxis(BlockState state) {
		return state.getValue(FACING).getAxis();
	}

    @Override
    public Class<CreatePoweredActuatorBlockEntity> getBlockEntityClass() {
        return CreatePoweredActuatorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CreatePoweredActuatorBlockEntity> getBlockEntityType() {
        return CreateCompat.CREATE_POWERED_ACTUATOR_ENTITY.get();
    }

    public double stressImpactPerRpm() {
		return ConfigManager.KINETIC_ACTUATOR_STRESS_IMPACT.get();
	}
    
}
