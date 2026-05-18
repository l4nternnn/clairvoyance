package com.shushuwonie.clairvoyance.screen;

import com.shushuwonie.clairvoyance.Clairvoyance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class OtherPlayerInventoryScreenHandler extends ScreenHandler {
    private final Inventory otherInventory;
    private final PlayerEntity viewer;

    // 客户端使用的构造器（必须正确初始化槽位）
    public OtherPlayerInventoryScreenHandler(int syncId, PlayerInventory viewerInv) {
        this(syncId, viewerInv, new SimpleInventory(41)); // 41 个槽位，与服务端目标玩家物品栏数量一致
    }

    // 服务端使用的构造器
    public OtherPlayerInventoryScreenHandler(int syncId, PlayerInventory viewerInv, Inventory targetInventory) {
        super(Clairvoyance.OTHER_INVENTORY_HANDLER, syncId);
        this.otherInventory = targetInventory;
        this.viewer = viewerInv.player;
        targetInventory.onOpen(viewerInv.player);

        // 目标玩家的背包槽位 (41 格)
        // 主背包 27 格 (索引 9-35)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(targetInventory, col + row * 9 + 9, 8 + col * 18, 46+35 + row * 18));
            }
        }
        // 快捷栏 9 格 (索引 0-8)
//        for (int col = 0; col < 9; ++col) {
//            this.addSlot(new Slot(targetInventory, col, 8 + col * 18, 142));
//        }
        // 盔甲栏 4 格 (索引 36-39)
//        for (int i = 0; i < 4; ++i) {
//            final EquipmentSlot slotType = EquipmentSlot.values()[2 + i];
//            final int slotIndex = 36 + i;
//            this.addSlot(new Slot(targetInventory, slotIndex, 8, 8 + i * 18) {
//                @Override
//                public boolean canInsert(ItemStack stack) {
//                    EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
//                    return equippable != null && equippable.slot() == slotType;
//                }
//                @Override
//                public int getMaxItemCount() {
//                    return 1;
//                }
//            });
//        }
        // 副手 (索引 40)
//        this.addSlot(new Slot(targetInventory, 40, 77, 62));

//        // 查看者自己的背包 (36 格)
//        for (int row = 0; row < 3; ++row) {
//            for (int col = 0; col < 9; ++col) {
//                this.addSlot(new Slot(viewerInv, col + row * 9 + 9, 8 + col * 18, 166 + row * 18));
//            }
//        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(viewerInv, col, 8 + col * 18, 176+38));
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        // 禁止快速移动（可根据需要实现）
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return otherInventory.canPlayerUse(player);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        otherInventory.onClose(player);
    }
}