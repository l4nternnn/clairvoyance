package com.shushuwonie.clairvoyance.features.block.body.arm;

import com.shushuwonie.clairvoyance.entity.ModBlockEntities;
import com.shushuwonie.clairvoyance.features.block.body.BodyPartBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class RightArmBlockEntity extends BodyPartBlockEntity{
    public RightArmBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RIGHT_ARM_BLOCK_ENTITY, pos, state);
    }

}
