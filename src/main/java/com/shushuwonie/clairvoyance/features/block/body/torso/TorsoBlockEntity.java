package com.shushuwonie.clairvoyance.features.block.body.torso;

import com.shushuwonie.clairvoyance.entity.ModBlockEntities;
import com.shushuwonie.clairvoyance.features.block.body.BodyPartBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class TorsoBlockEntity extends BodyPartBlockEntity {
    public TorsoBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TORSO_BLOCK_ENTITY, pos, state);
    }
}
