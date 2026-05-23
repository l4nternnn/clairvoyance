package com.shushuwonie.clairvoyance.client.gui.evil_eyes;

import com.shushuwonie.clairvoyance.network.clairvoyance.GlobalConfigS2CPacket;
import com.shushuwonie.clairvoyance.network.clairvoyance.RequestGlobalConfigC2SPacket;
import com.shushuwonie.clairvoyance.network.clairvoyance.UpdateGlobalConfigC2SPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;

public class GlobalConfigScreen extends Screen {
    private static final int STAGES = 7;
    private int currentStage = 1;
    private TextFieldWidget dailyField, marksField, minScoreField, maxScoreField, watchTimeField;
    private TextFieldWidget parrotDailyField, maxActiveParrotsField;  // 新增
    private ButtonWidget saveButton;
    private int panelWidth, panelHeight, panelX, panelY;
    private int leftWidth, rightWidth;

    private GlobalConfigS2CPacket.StageConfig[] configs = new GlobalConfigS2CPacket.StageConfig[STAGES + 1];

    public GlobalConfigScreen() {
        super(Text.literal("全局阶段配置"));
        ClientPlayNetworking.send(new RequestGlobalConfigC2SPacket());
    }

    public void receiveConfigs(GlobalConfigS2CPacket.StageConfig[] configs) {
        for (int i = 0; i < configs.length && i < STAGES; i++) {
            this.configs[i + 1] = configs[i];
        }
        loadStageUI();
    }

    @Override
    protected void init() {
        super.init();
        int sw = this.client.getWindow().getScaledWidth();
        int sh = this.client.getWindow().getScaledHeight();
        panelWidth = sw / 2;
        panelHeight = (int)(panelWidth * 9f / 16f);
        panelX = (sw - panelWidth) / 2;
        panelY = (sh - panelHeight) / 2;
        leftWidth = panelWidth / 5;
        rightWidth = panelWidth - leftWidth - 10;

        // 左侧阶段按钮
        for (int i = 1; i <= STAGES; i++) {
            int stage = i;
            ButtonWidget btn = ButtonWidget.builder(Text.literal("阶段 " + i), button -> {
                currentStage = stage;
                loadStageUI();
            }).dimensions(panelX + 5, panelY + 10 + (i-1)*25, leftWidth - 10, 20).build();
            addDrawableChild(btn);
        }

        // 右侧配置区域
        int rightX = panelX + leftWidth + 5;
        int rowY = panelY + 10;
        int rowHeight = 22;
        int labelWidth = 100;
        int inputWidth = 60;

        // 原有字段
        addDrawableChild(new TextWidget(rightX, rowY + 4, labelWidth, 9, Text.literal("每日可使用次数:"), textRenderer));
        dailyField = new TextFieldWidget(textRenderer, rightX + labelWidth, rowY, inputWidth, 18, Text.empty());
        dailyField.setMaxLength(3);
        dailyField.setTooltip(Tooltip.of(Text.literal("该阶段每天最多可标记的次数")));
        addDrawableChild(dailyField);

        addDrawableChild(new TextWidget(rightX, rowY + rowHeight + 4, labelWidth, 9, Text.literal("最大标记数量:"), textRenderer));
        marksField = new TextFieldWidget(textRenderer, rightX + labelWidth, rowY + rowHeight, inputWidth, 18, Text.empty());
        marksField.setMaxLength(3);
        marksField.setTooltip(Tooltip.of(Text.literal("同时存在的最大标记实体数")));
        addDrawableChild(marksField);

        addDrawableChild(new TextWidget(rightX, rowY + 2*rowHeight + 4, labelWidth, 9, Text.literal("触发区间(下限):"), textRenderer));
        minScoreField = new TextFieldWidget(textRenderer, rightX + labelWidth, rowY + 2*rowHeight, inputWidth, 18, Text.empty());
        minScoreField.setMaxLength(3);
        minScoreField.setTooltip(Tooltip.of(Text.literal("达到此分数（含）进入该阶段")));
        addDrawableChild(minScoreField);

        addDrawableChild(new TextWidget(rightX, rowY + 3*rowHeight + 4, labelWidth, 9, Text.literal("触发区间(上限):"), textRenderer));
        maxScoreField = new TextFieldWidget(textRenderer, rightX + labelWidth, rowY + 3*rowHeight, inputWidth, 18, Text.empty());
        maxScoreField.setMaxLength(3);
        maxScoreField.setTooltip(Tooltip.of(Text.literal("分数不超过此值（含）时处于该阶段")));
        addDrawableChild(maxScoreField);



        saveButton = ButtonWidget.builder(Text.literal("保存"), btn -> saveConfig())
                .dimensions(rightX, rowY + 7*rowHeight + 10, 80, 20)
                .build();
        addDrawableChild(saveButton);
    }

    private void loadStageUI() {
        if (configs[currentStage] == null) return;
        dailyField.setText(String.valueOf(configs[currentStage].dailyLimit()));
        marksField.setText(String.valueOf(configs[currentStage].maxMarks()));
        minScoreField.setText(String.valueOf(configs[currentStage].minScore()));
        maxScoreField.setText(String.valueOf(configs[currentStage].maxScore()));
        watchTimeField.setText(String.valueOf(configs[currentStage].watchRequiredTicks() / 20));
        parrotDailyField.setText(String.valueOf(configs[currentStage].parrotDailyLimit()));
        maxActiveParrotsField.setText(String.valueOf(configs[currentStage].maxActiveParrots()));
    }

    private void saveConfig() {
        try {
            int daily = Integer.parseInt(dailyField.getText().trim());
            int marks = Integer.parseInt(marksField.getText().trim());
            int minScore = Integer.parseInt(minScoreField.getText().trim());
            int maxScore = Integer.parseInt(maxScoreField.getText().trim());
            int watchSec = Integer.parseInt(watchTimeField.getText().trim());
            int watchTicks = watchSec * 20;
            int parrotDaily = Integer.parseInt(parrotDailyField.getText().trim());
            int maxActive = Integer.parseInt(maxActiveParrotsField.getText().trim());

            ClientPlayNetworking.send(new UpdateGlobalConfigC2SPacket(currentStage, daily, marks, minScore, maxScore, watchTicks,parrotDaily, maxActive
            ));

            if (client != null && client.player != null)
                client.player.sendMessage(Text.literal("§a配置已提交"), true);
        } catch (NumberFormatException e) {
            if (client != null && client.player != null)
                client.player.sendMessage(Text.literal("§c请输入有效数字"), true);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int delta = (int)Math.signum(verticalAmount);
        int newStage = currentStage - delta;
        if (newStage >= 1 && newStage <= STAGES) {
            currentStage = newStage;
            loadStageUI();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xAA000000);
        context.fill(panelX - 1, panelY - 1, panelX + panelWidth + 1, panelY + panelHeight + 1, 0xFF444444);
        context.fill(panelX + leftWidth, panelY, panelX + leftWidth + 1, panelY + panelHeight, 0xFF444444);
        context.fill(panelX, panelY, panelX + leftWidth, panelY + panelHeight, 0xAA222222);
        context.fill(panelX + leftWidth + 1, panelY, panelX + panelWidth, panelY + panelHeight, 0xAA222222);
        context.drawText(textRenderer, Text.literal("当前编辑: 阶段 " + currentStage), panelX + leftWidth + 5, panelY + 5, 0xFFFFFF, false);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() { return false; }
}