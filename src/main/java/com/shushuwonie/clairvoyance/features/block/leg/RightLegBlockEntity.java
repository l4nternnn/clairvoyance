package com.shushuwonie.clairvoyance.features.block.leg;

import com.shushuwonie.clairvoyance.entity.ModBlockEntities;
import com.shushuwonie.clairvoyance.features.block.BodyPartBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class RightLegBlockEntity extends BodyPartBlockEntity{
    public RightLegBlockEntity(BlockPos pos, BlockState state) {
            super(ModBlockEntities.RIGHT_LEG_BLOCK_ENTITY, pos, state);
        }
}

