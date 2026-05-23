package com.shushuwonie.clairvoyance.item.modblock;

import com.shushuwonie.clairvoyance.features.block.arm.LeftArmBlock;
import com.shushuwonie.clairvoyance.features.block.arm.RightArmBlock;
import com.shushuwonie.clairvoyance.features.block.leg.LeftLegBlock;
import com.shushuwonie.clairvoyance.features.block.head.HeadBlock;
import com.shushuwonie.clairvoyance.features.block.leg.RightLegBlock;
import com.shushuwonie.clairvoyance.features.block.torso.TorsoBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class ModBlocks {
    // 定义 5 个方块实例
    public static final Block TORSO = register("torso",
            TorsoBlock::new,
            AbstractBlock.Settings.create().mapColor(MapColor.OAK_TAN).strength(1.0F, 1.0F).nonOpaque()
    );

    public static final Block LEFT_ARM = register("left_arm",
            LeftArmBlock::new,
            AbstractBlock.Settings.create().mapColor(MapColor.OAK_TAN).strength(1.0F, 1.0F).pistonBehavior(PistonBehavior.DESTROY));

    public static final Block RIGHT_ARM = register("right_arm",
            RightArmBlock::new,
            AbstractBlock.Settings.create().mapColor(MapColor.OAK_TAN).strength(1.0F, 1.0F).pistonBehavior(PistonBehavior.DESTROY));

    public static final Block LEFT_LEG = register("left_leg",
            LeftLegBlock::new,
            AbstractBlock.Settings.create().mapColor(MapColor.OAK_TAN).strength(1.0F, 1.0F).pistonBehavior(PistonBehavior.DESTROY));

    public static final Block RIGHT_LEG = register("right_leg",
            RightLegBlock::new,
            AbstractBlock.Settings.create().mapColor(MapColor.OAK_TAN).strength(1.0F, 1.0F).pistonBehavior(PistonBehavior.DESTROY));

    public static final Block HEAD = register("head",
            HeadBlock::new,
            AbstractBlock.Settings.create().mapColor(MapColor.OAK_TAN).strength(1.0F, 1.0F).pistonBehavior(PistonBehavior.DESTROY));

    // 通用注册方法（模仿原版 Blocks 中的 register）
    private static Block register(String path, Function<AbstractBlock.Settings, Block> factory, AbstractBlock.Settings settings) {
        RegistryKey<Block> key = RegistryKey.of(RegistryKeys.BLOCK, Identifier.of("clairvoyance", path)); // 使用你的模组 ID
        Block block = factory.apply(settings.registryKey(key));
        return Registry.register(Registries.BLOCK, key, block);
    }

    // 供模组主类调用，触发 static 初始化
    public static void initialize() {
        // static 字段会在类加载时自动初始化，此空方法仅用于确保类被加载
    }
}