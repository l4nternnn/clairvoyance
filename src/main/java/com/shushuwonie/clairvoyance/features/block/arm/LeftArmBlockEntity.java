package com.shushuwonie.clairvoyance.features.block.arm;

import com.shushuwonie.clairvoyance.entity.ModBlockEntities;
import com.shushuwonie.clairvoyance.features.block.BodyPartBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class LeftArmBlockEntity extends BodyPartBlockEntity {
    public LeftArmBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LEFT_ARM_BLOCK_ENTITY, pos, state);
    }
}