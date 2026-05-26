package com.shushuwonie.clairvoyance.features.block.body.leg;

import com.shushuwonie.clairvoyance.entity.ModBlockEntities;
import com.shushuwonie.clairvoyance.features.block.body.BodyPartBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class LeftLegBlockEntity extends BodyPartBlockEntity {
    public LeftLegBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LEFT_LEG_BLOCK_ENTITY, pos, state);
    }

}
