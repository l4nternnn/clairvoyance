package com.shushuwonie.clairvoyance.client.gui.evil_eyes;

import com.shushuwonie.clairvoyance.network.clairvoyance.SelectViewPayload;
import com.shushuwonie.clairvoyance.network.clairvoyance.UnmarkEntityPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;

import net.minecraft.client.gui.Element;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Evil_eyesScreen extends Screen {
    private static Map<UUID, Long> currentMarks = new ConcurrentHashMap<>();
    private int panelWidth, panelHeight, panelX, panelY;
    private int leftWidth, rightWidth;

    public Evil_eyesScreen() {
        super(Text.empty());
    }

    public static void updateMarkedList(Map<UUID, Long> marks) {
        currentMarks.clear();
        currentMarks.putAll(marks);
        refreshCurrentScreen();
    }

    // 立即从本地列表中移除指定实体并刷新GUI（无需等待服务器回包）
    public static void removeFromLocalList(UUID entityUuid) {
        currentMarks.remove(entityUuid);
        refreshCurrentScreen();
    }

    private static void refreshCurrentScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof Evil_eyesScreen screen) {
            screen.refreshEntityButtons();
        }
    }

    @Override
    protected void init() {
        super.init();
        int sw = this.client.getWindow().getScaledWidth();
        int sh = this.client.getWindow().getScaledHeight();
        panelWidth = sw / 2;
        panelHeight = (int) (panelWidth * 9f / 16f);
        panelX = (sw - panelWidth) / 2;
        panelY = (sh - panelHeight) / 2;
        leftWidth = panelWidth / 2;
        rightWidth = panelWidth - leftWidth;

        addDrawableChild(new TextWidget(panelX + 5, panelY + 10, leftWidth - 10, 20,
                Text.literal("已标记实体列表"), textRenderer));
        addDrawableChild(new TextWidget(panelX + leftWidth + 5, panelY + 10, rightWidth - 10, 20,
                Text.literal("点击左侧实体名称"), textRenderer));
        addDrawableChild(new TextWidget(panelX + leftWidth + 5, panelY + 35, rightWidth - 10, 20,
                Text.literal("即可切换到该实体的"), textRenderer));
        addDrawableChild(new TextWidget(panelX + leftWidth + 5, panelY + 60, rightWidth - 10, 20,
                Text.literal("第二人称视角"), textRenderer));

        refreshEntityButtons();
    }

    private void refreshEntityButtons() {
        // 隐藏旧的按钮并从children移除（drawables无法直接访问，通过visible=false避免渲染）
        java.util.List<Element> oldBtns = new java.util.ArrayList<>();
        for (Element child : children()) {
            if (child instanceof ButtonWidget btn) {
                btn.visible = false;
                oldBtns.add(child);
            }
        }
        children().removeAll(oldBtns);
        int yOffset = 40;
        for (UUID uuid : currentMarks.keySet()) {
            String name = getEntityName(uuid);
            int btnWidth = leftWidth - 10 - 20;
            // 实体名称按钮 - 点击选择观看
            ButtonWidget nameBtn = ButtonWidget.builder(Text.literal(name), button -> {
                        ClientPlayNetworking.send(new SelectViewPayload(uuid));
                        if (client != null && client.player != null) {
                            client.player.sendMessage(Text.literal("§a正在切换到 " + name), true);
                        }
                    })
                    .dimensions(panelX + 5, panelY + yOffset, btnWidth, 20)
                    .build();
            addDrawableChild(nameBtn);
            // 删除按钮 - 点击取消标记（立即本地移除 + 服务端同步）
            ButtonWidget delBtn = ButtonWidget.builder(Text.literal("✕"), button -> {
                        removeFromLocalList(uuid);
                        ClientPlayNetworking.send(new UnmarkEntityPayload(uuid));
                    })
                    .dimensions(panelX + 5 + btnWidth + 2, panelY + yOffset, 18, 20)
                    .build();
            addDrawableChild(delBtn);
            yOffset += 25;
            if (yOffset > panelHeight - 30) break;
        }
    }

    private String getEntityName(UUID uuid) {
        if (client != null && client.world != null) {
            var entity = client.world.getEntity(uuid);
            if (entity != null) return entity.getName().getString();
        }
        return "未知实体";
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xAA000000);
        context.fill(panelX - 1, panelY - 1, panelX + panelWidth + 1, panelY + panelHeight + 1, 0xFF444444);
        context.fill(panelX + leftWidth, panelY, panelX + leftWidth + 1, panelY + panelHeight, 0xFF444444);
        context.fill(panelX, panelY, panelX + leftWidth, panelY + panelHeight, 0xAA222222);
        context.fill(panelX + leftWidth + 1, panelY, panelX + panelWidth, panelY + panelHeight, 0xAA222222);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}