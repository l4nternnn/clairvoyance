package com.shushuwonie.clairvoyance.item.modblock.moditems;

import com.shushuwonie.clairvoyance.item.modblock.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class Assembly_ModItems {
    public static final BlockItem TORSO_ITEM;
    public static final BlockItem LEFT_ARM_ITEM;
    public static final BlockItem RIGHT_ARM_ITEM;
    public static final BlockItem LEFT_LEG_ITEM;
    public static final BlockItem RIGHT_LEG_ITEM;

    static {
        TORSO_ITEM = createBlockItem("torso", ModBlocks.TORSO);
        LEFT_ARM_ITEM = createBlockItem("left_arm", ModBlocks.LEFT_ARM);
        RIGHT_ARM_ITEM = createBlockItem("right_arm", ModBlocks.RIGHT_ARM);
        LEFT_LEG_ITEM = createBlockItem("left_leg", ModBlocks.LEFT_LEG);
        RIGHT_LEG_ITEM = createBlockItem("right_leg", ModBlocks.RIGHT_LEG);
    }

    private static BlockItem createBlockItem(String path, net.minecraft.block.Block block) {
        Identifier id = Identifier.of("clairvoyance", path);
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
        Item.Settings settings = new Item.Settings()
                .registryKey(key)
                .maxCount(64); // 可根据需要修改堆叠上限
        return new BlockItem(block, settings);
    }


    public static void initialize() {

        // 注册所有 BlockItem
        register(TORSO_ITEM, "torso");
        register(LEFT_ARM_ITEM, "left_arm");
        register(RIGHT_ARM_ITEM, "right_arm");
        register(LEFT_LEG_ITEM, "left_leg");
        register(RIGHT_LEG_ITEM, "right_leg");


        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS)
                .register(entries -> {
                    entries.add(TORSO_ITEM);
                    entries.add(LEFT_ARM_ITEM);
                    entries.add(RIGHT_ARM_ITEM);
                    entries.add(LEFT_LEG_ITEM);
                    entries.add(RIGHT_LEG_ITEM);
                });
    }

    private static void register(BlockItem item, String path) {
        Identifier id = Identifier.of("clairvoyance", path);
        Registry.register(Registries.ITEM, id, item);
    }
}

