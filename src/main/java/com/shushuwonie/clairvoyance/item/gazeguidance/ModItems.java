package com.shushuwonie.clairvoyance.item.gazeguidance;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModItems {
    public static final MagicStickItem MAGIC_STICK;

    static {
        Identifier id = Identifier.of("clairvoyance", "magic_stick");
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
        Item.Settings settings = new Item.Settings()
                .registryKey(key)
                .maxCount(1);
        MAGIC_STICK = new MagicStickItem(settings);
    }

    public static void initialize() {
        Registry.register(Registries.ITEM, Identifier.of("clairvoyance", "magic_stick"), MAGIC_STICK);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
                .register(entries -> entries.add(MAGIC_STICK));
    }
}