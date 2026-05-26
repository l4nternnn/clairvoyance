package com.shushuwonie.clairvoyance.item.mirror;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class mirror_of_then_and_now {
    public static final String ITEM_ID = "mirror_of_then_and_now";
    public static Item MIRROR_ITEM;

    public static void initialize() {
        Identifier id = Identifier.of("clairvoyance", ITEM_ID);
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
        Item.Settings settings = new Item.Settings()
                .registryKey(key)
                .maxCount(1);
        MIRROR_ITEM = new MirrorOfThenAndNowItem(settings);

        Registry.register(Registries.ITEM, id, MIRROR_ITEM);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
                .register(entries -> entries.add(MIRROR_ITEM));
    }
}
