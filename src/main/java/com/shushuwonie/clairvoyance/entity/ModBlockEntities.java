package com.shushuwonie.clairvoyance.entity;

import com.shushuwonie.clairvoyance.Clairvoyance;
import com.shushuwonie.clairvoyance.features.block.body.arm.LeftArmBlockEntity;
import com.shushuwonie.clairvoyance.features.block.body.arm.RightArmBlockEntity;
import com.shushuwonie.clairvoyance.features.block.body.leg.LeftLegBlockEntity;
import com.shushuwonie.clairvoyance.features.block.body.leg.RightLegBlockEntity;
import com.shushuwonie.clairvoyance.features.block.body.head.HeadBlockEntity;
import com.shushuwonie.clairvoyance.features.block.body.torso.TorsoBlockEntity;
import com.shushuwonie.clairvoyance.item.modblock.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {
    public static final BlockEntityType<TorsoBlockEntity> TORSO_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(Clairvoyance.MOD_ID, "torso"),
            FabricBlockEntityTypeBuilder.create(TorsoBlockEntity::new, ModBlocks.TORSO).build()
    );

    public static final BlockEntityType<LeftArmBlockEntity> LEFT_ARM_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(Clairvoyance.MOD_ID, "left_arm"),
            FabricBlockEntityTypeBuilder.create(LeftArmBlockEntity::new, ModBlocks.LEFT_ARM).build()
    );

    public static final BlockEntityType<RightArmBlockEntity> RIGHT_ARM_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(Clairvoyance.MOD_ID, "right_arm"),
            FabricBlockEntityTypeBuilder.create(RightArmBlockEntity::new, ModBlocks.RIGHT_ARM).build()
    );
    public static final BlockEntityType<LeftLegBlockEntity> LEFT_LEG_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(Clairvoyance.MOD_ID, "left_leg"),
            FabricBlockEntityTypeBuilder.create(LeftLegBlockEntity::new, ModBlocks.LEFT_LEG).build()
    );
    public static final BlockEntityType<RightLegBlockEntity> RIGHT_LEG_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(Clairvoyance.MOD_ID, "right_leg"),
            FabricBlockEntityTypeBuilder.create(RightLegBlockEntity::new, ModBlocks.RIGHT_LEG).build()
    );

    public static final BlockEntityType<HeadBlockEntity> HEAD_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(Clairvoyance.MOD_ID, "head"),
            FabricBlockEntityTypeBuilder.create(HeadBlockEntity::new, ModBlocks.HEAD).build()
    );

    public static void initialize() {
//        System.out.println("Registered TORSO_BLOCK_ENTITY: " + TORSO_BLOCK_ENTITY.getRegistryEntry().getIdAsString());
    }
}
