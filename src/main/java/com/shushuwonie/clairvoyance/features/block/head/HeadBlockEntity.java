package com.shushuwonie.clairvoyance.features.block.head;

import com.shushuwonie.clairvoyance.entity.ModBlockEntities;
import com.shushuwonie.clairvoyance.features.block.BodyPartBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class HeadBlockEntity extends BodyPartBlockEntity {
    public HeadBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEAD_BLOCK_ENTITY, pos, state);
    }
}
