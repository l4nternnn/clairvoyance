package com.shushuwonie.clairvoyance.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class OtherPlayerInventoryScreenHandlerFactory implements NamedScreenHandlerFactory {
    private final PlayerInventory targetInventory;

    public OtherPlayerInventoryScreenHandlerFactory(ServerPlayerEntity targetPlayer) {
        this.targetInventory = targetPlayer.getInventory(); // 直接使用引用，实时同步
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("目标玩家的背包");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new OtherPlayerInventoryScreenHandler(syncId, inv, targetInventory);
    }
}