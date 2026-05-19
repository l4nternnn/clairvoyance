package com.shushuwonie.clairvoyance.client.gui;

import com.shushuwonie.clairvoyance.item.config.GazeConfig;
import com.shushuwonie.clairvoyance.network.clairvoyance.GlobalConfigS2CPacket;
import com.shushuwonie.clairvoyance.network.clairvoyance.RequestGlobalConfigC2SPacket;
import com.shushuwonie.clairvoyance.network.clairvoyance.UpdateGlobalConfigC2SPacket;
import com.shushuwonie.clairvoyance.network.gazeguidance.RequestConfigC2SPacket;
import com.shushuwonie.clairvoyance.network.gazeguidance.UpdateConfigC2SPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class CombinedConfigScreen extends Screen {
    private enum ConfigType { EVIL_EYES, GAZE_GUIDANCE }
    private ConfigType currentType = ConfigType.EVIL_EYES;

    private ButtonWidget btnEvilEyes, btnGaze;
    private int panelX, panelY, panelWidth, panelHeight;
    private int leftWidth, rightWidth;

    // 千里眼组件
    private final List<ButtonWidget> stageButtons = new ArrayList<>();
    private int currentStage = 1;
    private TextFieldWidget dailyField, marksField, minScoreField, maxScoreField,watchTimeField,parrotDailyField,maxActiveField;
    private ButtonWidget saveEvilButton;
    private static final GlobalConfigS2CPacket.StageConfig[] CACHED_EVIL_CONFIGS = new GlobalConfigS2CPacket.StageConfig[8];
    private final List<TextWidget> evilLabels = new ArrayList<>();

    // 视线诱导组件
    private final List<ButtonWidget> gazeStageButtons = new ArrayList<>();
    private int gazeCurrentStage = 1;
    private TextFieldWidget gazeDrainField, gazeRegenField, gazeRadiusField, gazeMarksField;
    private ButtonWidget saveGazeButton;
    private final List<TextWidget> gazeLabels = new ArrayList<>();
    private static GazeConfig cachedGazeConfig = null;

    public CombinedConfigScreen() {
        super(Text.literal("双物品配置"));
        ClientPlayNetworking.send(new RequestGlobalConfigC2SPacket());
        ClientPlayNetworking.send(new RequestConfigC2SPacket());
        if (cachedGazeConfig == null) {
            cachedGazeConfig = createDefaultGazeConfig();
        }
    }

    private GazeConfig createDefaultGazeConfig() {
        GazeConfig config = new GazeConfig();
        for (int i = 0; i < 7; i++) {
            int stage = i + 1;
            config.stages[i] = new GazeConfig.StageConfig();
            config.stages[i].name = "阶段 " + stage;
            config.stages[i].energyDrainPerMark = stage;
            config.stages[i].energyRegenPerSecond = 2 + stage;
            config.stages[i].radius = 5 + (stage - 1) * 2.0;
            config.stages[i].maxMarks = stage <= 2 ? 3 : (stage <= 4 ? 6 : 15);
        }
        return config;
    }

    @Override
    protected void init() {
        super.init();
        int sw = this.client.getWindow().getScaledWidth();
        int sh = this.client.getWindow().getScaledHeight();
        panelWidth = sw * 3 / 5;
        panelHeight = (int)(panelWidth * 9f / 16f);
        panelX = (sw - panelWidth) / 2 - 100;
        panelY = (sh - panelHeight) / 2;

        leftWidth = 140;
        rightWidth = panelWidth - leftWidth - 15;

        int btnWidth = 100, btnHeight = 20;
        int btnY = panelY - btnHeight - 5;
        btnEvilEyes = ButtonWidget.builder(Text.literal("千里眼配置"), btn -> switchTo(ConfigType.EVIL_EYES))
                .dimensions(panelX + panelWidth/2 - btnWidth - 5, btnY, btnWidth, btnHeight).build();
        btnGaze = ButtonWidget.builder(Text.literal("视线诱导配置"), btn -> switchTo(ConfigType.GAZE_GUIDANCE))
                .dimensions(panelX + panelWidth/2 + 5, btnY, btnWidth, btnHeight).build();
        addDrawableChild(btnEvilEyes);
        addDrawableChild(btnGaze);

        buildEvilEyesUI();
        buildGazeGuidanceUI();

        switchTo(ConfigType.EVIL_EYES);
    }

    private void buildEvilEyesUI() {
        int leftX = panelX + 5;
        for (int i = 1; i <= 7; i++) {
            int stage = i;
            ButtonWidget btn = ButtonWidget.builder(Text.literal("阶段 " + i), button -> {
                currentStage = stage;
                loadEvilStageUI();
            }).dimensions(leftX, panelY + 10 + (i-1)*25, leftWidth - 10, 20).build();
            addDrawableChild(btn);
            stageButtons.add(btn);
        }

        int rightX = panelX + leftWidth + 5;
        int rowY = panelY + 10;
        int rowHeight = 22;
        int labelWidth = 100;
        int inputWidth = 60;

        String[] labels = {
                "每日可使用次数:", "最大标记数量:", "触发区间(下限):", "触发区间(上限):","观看时长:","每日锚点放置次数:","锚点同存数:"
        };
        for (int i = 0; i < labels.length; i++) {
            TextWidget label = new TextWidget(rightX, rowY + i * rowHeight + 4, labelWidth, 9, Text.literal(labels[i]), textRenderer);
            addDrawableChild(label);
            evilLabels.add(label);
        }

        dailyField = createField(rightX + labelWidth, rowY, inputWidth);
        marksField = createField(rightX + labelWidth, rowY + rowHeight, inputWidth);
        minScoreField = createField(rightX + labelWidth, rowY + 2*rowHeight, inputWidth);
        maxScoreField = createField(rightX + labelWidth, rowY + 3*rowHeight, inputWidth);
        watchTimeField = createField(rightX + labelWidth, rowY + 4*rowHeight, inputWidth);
        parrotDailyField = createField(rightX + labelWidth, rowY + 5*rowHeight, inputWidth);
        maxActiveField = createField(rightX + labelWidth, rowY + 6*rowHeight, inputWidth);
        saveEvilButton = ButtonWidget.builder(Text.literal("保存"), btn -> saveEvilConfig())

                .dimensions(rightX, rowY + 7*rowHeight + 10, 80, 20).build();
        addDrawableChild(saveEvilButton);
    }

    private TextFieldWidget createField(int x, int y, int width) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, width, 18, Text.empty());
        field.setMaxLength(3);
        addDrawableChild(field);
        return field;
    }

    private void buildGazeGuidanceUI() {
        int leftX = panelX + 5;
        for (int i = 1; i <= 7; i++) {
            int stage = i;
            ButtonWidget btn = ButtonWidget.builder(Text.literal("阶段 " + i), button -> {
                gazeCurrentStage = stage;
                loadGazeStageUI();
            }).dimensions(leftX, panelY + 10 + (i-1)*25, leftWidth - 10, 20).build();
            addDrawableChild(btn);
            gazeStageButtons.add(btn);
        }

        int gazeRightX = panelX + leftWidth + 5;
        int gazeRowY = panelY + 40;
        int gazeRowHeight = 22;
        int gazeLabelWidth = 120;
        int gazeInputWidth = 80;

        String[] gazeLabelsArr = {"消耗速率(/秒/标记):", "回复速率(/秒):", "范围(格):", "最大标记数量:"};
        for (int i = 0; i < gazeLabelsArr.length; i++) {
            TextWidget label = new TextWidget(gazeRightX, gazeRowY + i * gazeRowHeight + 4, gazeLabelWidth, 9, Text.literal(gazeLabelsArr[i]), textRenderer);
            addDrawableChild(label);
            gazeLabels.add(label);
        }

        gazeDrainField = new TextFieldWidget(textRenderer, gazeRightX + gazeLabelWidth, gazeRowY, gazeInputWidth, 18, Text.empty());
        gazeDrainField.setMaxLength(6);
        gazeRegenField = new TextFieldWidget(textRenderer, gazeRightX + gazeLabelWidth, gazeRowY + gazeRowHeight, gazeInputWidth, 18, Text.empty());
        gazeRegenField.setMaxLength(6);
        gazeRadiusField = new TextFieldWidget(textRenderer, gazeRightX + gazeLabelWidth, gazeRowY + 2*gazeRowHeight, gazeInputWidth, 18, Text.empty());
        gazeRadiusField.setMaxLength(6);
        gazeMarksField = new TextFieldWidget(textRenderer, gazeRightX + gazeLabelWidth, gazeRowY + 3*gazeRowHeight, gazeInputWidth, 18, Text.empty());
        gazeMarksField.setMaxLength(3);
        addDrawableChild(gazeDrainField);
        addDrawableChild(gazeRegenField);
        addDrawableChild(gazeRadiusField);
        addDrawableChild(gazeMarksField);

        saveGazeButton = ButtonWidget.builder(Text.literal("保存"), btn -> saveGazeConfig())
                .dimensions(gazeRightX + 20, gazeRowY + 4*gazeRowHeight + 10, 80, 20).build();
        addDrawableChild(saveGazeButton);
    }

    private void switchTo(ConfigType type) {
        currentType = type;
        btnEvilEyes.setMessage(currentType == ConfigType.EVIL_EYES ? Text.literal("§l§a千里眼配置") : Text.literal("千里眼配置"));
        btnGaze.setMessage(currentType == ConfigType.GAZE_GUIDANCE ? Text.literal("§l§a视线诱导配置") : Text.literal("视线诱导配置"));
        setEvilComponentsVisible(currentType == ConfigType.EVIL_EYES);
        setGazeComponentsVisible(currentType == ConfigType.GAZE_GUIDANCE);
        if (currentType == ConfigType.EVIL_EYES) {
            loadEvilStageUI();
        } else {
            loadGazeStageUI();
        }
    }

    private void setEvilComponentsVisible(boolean visible) {
        for (ButtonWidget btn : stageButtons) btn.visible = visible;
        for (TextWidget label : evilLabels) label.visible = visible;
        if (dailyField != null) dailyField.visible = visible;
        if (marksField != null) marksField.visible = visible;
        if (minScoreField != null) minScoreField.visible = visible;
        if (maxScoreField != null) maxScoreField.visible = visible;
        if (watchTimeField != null) watchTimeField.visible = visible;
        if (saveEvilButton != null) saveEvilButton.visible = visible;
        if (parrotDailyField != null) parrotDailyField.visible = visible;
        if (maxActiveField != null) maxActiveField.visible = visible;
    }

    private void setGazeComponentsVisible(boolean visible) {
        for (ButtonWidget btn : gazeStageButtons) btn.visible = visible;
        for (TextWidget label : gazeLabels) label.visible = visible;
        if (gazeDrainField != null) gazeDrainField.visible = visible;
        if (gazeRegenField != null) gazeRegenField.visible = visible;
        if (gazeRadiusField != null) gazeRadiusField.visible = visible;
        if (gazeMarksField != null) gazeMarksField.visible = visible;
        if (saveGazeButton != null) saveGazeButton.visible = visible;
    }

    // ========== 千里眼配置 ==========
    private void loadEvilStageUI() {
        GlobalConfigS2CPacket.StageConfig cfg = CACHED_EVIL_CONFIGS[currentStage];
        if (cfg == null) {
            dailyField.setText("10");
            marksField.setText("3");
            minScoreField.setText("0");
            maxScoreField.setText("5");
            watchTimeField.setText("2");
            parrotDailyField.setText("5");
            maxActiveField.setText("1");
            return;
        }
        dailyField.setText(String.valueOf(cfg.dailyLimit()));
        marksField.setText(String.valueOf(cfg.maxMarks()));
        minScoreField.setText(String.valueOf(cfg.minScore()));
        maxScoreField.setText(String.valueOf(cfg.maxScore()));
        watchTimeField.setText(String.valueOf(cfg.watchRequiredTicks() / 20));
        parrotDailyField.setText(String.valueOf(cfg.parrotDailyLimit()));
        maxActiveField.setText(String.valueOf(cfg.maxActiveParrots()));
    }

    private void saveEvilConfig() {
        try {
            int daily = Integer.parseInt(dailyField.getText().trim());
            int marks = Integer.parseInt(marksField.getText().trim());
            int minScore = Integer.parseInt(minScoreField.getText().trim());
            int maxScore = Integer.parseInt(maxScoreField.getText().trim());
            int watchSec = Integer.parseInt(watchTimeField.getText().trim());
            int parrotDaily = Integer.parseInt(parrotDailyField.getText().trim());
            int maxActive = (Integer.parseInt(maxActiveField.getText().trim()));
            int watchTicks = watchSec * 20;

            CACHED_EVIL_CONFIGS[currentStage] = new GlobalConfigS2CPacket.StageConfig(
                    daily, marks, minScore, maxScore, watchTicks, parrotDaily,maxActive
            );

            ClientPlayNetworking.send(new UpdateGlobalConfigC2SPacket(
                    currentStage, daily, marks, minScore, maxScore, watchTicks, parrotDaily,maxActive
            ));

            if (client != null && client.player != null)
                client.player.sendMessage(Text.literal("§a千里眼配置已提交"), true);
        } catch (NumberFormatException e) {
            if (client != null && client.player != null)
                client.player.sendMessage(Text.literal("§c请输入有效的整数数字"), true);
        }
    }

    public void receiveEvilConfigs(GlobalConfigS2CPacket.StageConfig[] configsArray) {
        if (configsArray == null) return;
        for (int i = 0; i < configsArray.length; i++) {
            CACHED_EVIL_CONFIGS[i + 1] = configsArray[i];
        }
        if (currentType == ConfigType.EVIL_EYES) {
            loadEvilStageUI();
        }
    }

    // ========== 视线诱导配置 ==========
    private void loadGazeStageUI() {
        if (cachedGazeConfig == null) {
            gazeDrainField.setText("");
            gazeRegenField.setText("");
            gazeRadiusField.setText("");
            gazeMarksField.setText("");
            return;
        }
        gazeDrainField.setText(String.valueOf(cachedGazeConfig.getEnergyDrain(gazeCurrentStage)));
        gazeRegenField.setText(String.valueOf(cachedGazeConfig.getEnergyRegen(gazeCurrentStage)));
        gazeRadiusField.setText(String.valueOf(cachedGazeConfig.getRadius(gazeCurrentStage)));
        gazeMarksField.setText(String.valueOf(cachedGazeConfig.getMaxMarks(gazeCurrentStage)));
    }

    private void saveGazeConfig() {
        try {
            double drain = Double.parseDouble(gazeDrainField.getText().trim());
            double regen = Double.parseDouble(gazeRegenField.getText().trim());
            double radius = Double.parseDouble(gazeRadiusField.getText().trim());
            int marks = Integer.parseInt(gazeMarksField.getText().trim());

            if (cachedGazeConfig == null) cachedGazeConfig = new GazeConfig();
            cachedGazeConfig.setEnergyDrain(gazeCurrentStage, drain);
            cachedGazeConfig.setEnergyRegen(gazeCurrentStage, regen);
            cachedGazeConfig.setRadius(gazeCurrentStage, radius);
            cachedGazeConfig.setMaxMarks(gazeCurrentStage, marks);

            ClientPlayNetworking.send(new UpdateConfigC2SPacket(cachedGazeConfig.toJson()));
            if (client != null && client.player != null)
                client.player.sendMessage(Text.literal("§e阶段 " + gazeCurrentStage + " 配置已提交，正在同步..."), true);
        } catch (NumberFormatException e) {
            if (client != null && client.player != null)
                client.player.sendMessage(Text.literal("§c请输入有效的数字"), true);
        }
    }

    public void receiveGazeConfig(GazeConfig config) {
        if (config == null) return;
        cachedGazeConfig = config;
        if (currentType == ConfigType.GAZE_GUIDANCE) {
            loadGazeStageUI();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xAA000000);
        context.fill(panelX - 1, panelY - 1, panelX + panelWidth + 1, panelY + panelHeight + 1, 0xFF444444);
        context.fill(panelX + leftWidth, panelY, panelX + leftWidth + 1, panelY + panelHeight, 0xFF444444);
        context.fill(panelX, panelY, panelX + leftWidth, panelY + panelHeight, 0xAA222222);
        context.fill(panelX + leftWidth + 1, panelY, panelX + panelWidth, panelY + panelHeight, 0xAA222222);

        if (currentType == ConfigType.EVIL_EYES) {
            context.drawText(textRenderer, "千里眼阶段配置", panelX + 5, panelY + 5, 0xFFFFFF, false);
            context.drawText(textRenderer, "当前编辑: 阶段 " + currentStage, panelX + leftWidth + 5, panelY + 5, 0xFFFFFF, false);
        } else {
            context.drawText(textRenderer, "诱导阶段配置 - 当前阶段: " + gazeCurrentStage, panelX + leftWidth + 5, panelY + 5, 0xFFFFFF, false);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}