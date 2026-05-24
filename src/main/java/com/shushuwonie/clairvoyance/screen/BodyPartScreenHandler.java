package com.shushuwonie.clairvoyance.screen;

import com.shushuwonie.clairvoyance.features.block.BodyPartBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class BodyPartScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    int diy_x = 20;
    int diy_y = 20;

    // 服务端构造器（传入真实的方块实体）
    public BodyPartScreenHandler(int syncId, PlayerInventory playerInventory, BodyPartBlockEntity blockEntity) {
        super(ModScreenHandlers.BODY_PART_SCREEN_HANDLER, syncId);
        this.inventory = blockEntity;
        addBlockEntitySlots();          // 添加 9 个槽位
        addPlayerInventorySlots(playerInventory);
    }

    // 客户端构造器（使用临时 SimpleInventory 占位）
    public BodyPartScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(ModScreenHandlers.BODY_PART_SCREEN_HANDLER, syncId);
        this.inventory = new SimpleInventory(9);
        addBlockEntitySlots();          // 添加 9 个槽位（用于显示）
        addPlayerInventorySlots(playerInventory);
    }

    // 通用构造器（用于展示实体背包）
    public BodyPartScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(ModScreenHandlers.BODY_PART_SCREEN_HANDLER, syncId);
        this.inventory = inventory;
        addBlockEntitySlots();
        addPlayerInventorySlots(playerInventory);
    }

    private void addBlockEntitySlots() {
        // 3×3 网格，位置 (62,17) 开始，间隔 18
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(inventory, col + row * 3, diy_x+62 + col * 18, diy_y+17 + row * 18));
            }
        }
    }

    private void addPlayerInventorySlots(PlayerInventory playerInventory) {
        // 玩家主背包 9×3 行，位置 (8,84) 开始
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, diy_x+ 8 + col * 18, diy_y+84 + row * 18));
            }
        }
        // 快捷栏 9 格，位置 (8,142)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, diy_x+8 + col * 18, diy_y+142));
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        ItemStack original = ItemStack.EMPTY;
        Slot slotObj = this.slots.get(slot);
        if (slotObj != null && slotObj.hasStack()) {
            ItemStack stack = slotObj.getStack();
            original = stack.copy();
            if (slot < 9) { // 从方块实体转移到玩家背包
                if (!this.insertItem(stack, 9, this.slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else { // 从玩家背包转移到方块实体
                if (!this.insertItem(stack, 0, 9, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slotObj.setStack(ItemStack.EMPTY);
            } else {
                slotObj.markDirty();
            }
        }
        return original;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }
}