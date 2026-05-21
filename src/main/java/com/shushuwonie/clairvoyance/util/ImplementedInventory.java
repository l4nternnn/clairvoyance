package com.shushuwonie.clairvoyance.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

/**
 * 一个简单的 Inventory 实现，只要求提供物品列表，其他方法自动实现。
 */
public interface ImplementedInventory extends Inventory {
    DefaultedList<ItemStack> getItems();

    @Override
    default int size() {
        return getItems().size();
    }

    @Override
    default boolean isEmpty() {
        return getItems().stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    default ItemStack getStack(int slot) {
        return getItems().get(slot);
    }

    @Override
    default ItemStack removeStack(int slot, int count) {
        ItemStack result = getItems().get(slot).split(count);
        if (getItems().get(slot).isEmpty()) {
            getItems().set(slot, ItemStack.EMPTY);
        }
        return result;
    }

    @Override
    default ItemStack removeStack(int slot) {
        ItemStack stack = getItems().get(slot);
        getItems().set(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    default void setStack(int slot, ItemStack stack) {
        getItems().set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }
    }

    @Override
    default void clear() {
        getItems().clear();
    }

    @Override
    default boolean canPlayerUse(PlayerEntity player) {
        return true; // 可以根据需要添加距离检查
    }

}