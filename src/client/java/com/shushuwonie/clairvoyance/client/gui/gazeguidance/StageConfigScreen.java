//package com.shushuwonie.clairvoyance.client.gui.gazeguidance;
//
//import com.shushuwonie.clairvoyance.client.gui.CombinedConfigScreen;
//import com.shushuwonie.clairvoyance.client.gui.evil_eyes.GlobalConfigScreen;
//import com.shushuwonie.clairvoyance.item.config.GazeConfig;
//import com.shushuwonie.clairvoyance.network.gazeguidance.*;
//import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//import net.minecraft.client.gui.screen.Screen;
//import net.minecraft.client.gui.tooltip.Tooltip;
//import net.minecraft.client.gui.widget.ButtonWidget;
//import net.minecraft.client.gui.widget.TextFieldWidget;
//import net.minecraft.client.gui.widget.TextWidget;
//import net.minecraft.text.Text;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class StageConfigScreen extends Screen {
//    private static final int STAGES = 7;
//    private final Screen parent;
//    private List<TextFieldWidget[]> stageColumns = new ArrayList<>();
//    private GazeConfig currentConfig;
//
//    private static final int COLUMN_WIDTH = 70;
//    private static final int INPUT_WIDTH = 54;
//    private static final int ROW_HEIGHT = 22;
//
//    public StageConfigScreen(Screen parent) {
//        super(Text.literal("阶段配置"));
//        this.parent = parent;
//        // 请求服务端发送当前配置
//        ClientPlayNetworking.send(new RequestConfigC2SPacket());
//    }
//
//    @Override
//    protected void init() {
//        super.init();
//        // 等待 rebuildUi 被调用
//    }
//
//    public void rebuildUi(GazeConfig config) {
//        this.currentConfig = config;
//        this.clearChildren();
//        stageColumns.clear();
//
//        int totalWidth = STAGES * COLUMN_WIDTH;
//        int startX = (width - totalWidth) / 2;
//        int startY = 35;
//
//        // 左侧标签（TextWidget 自动渲染）
//        int labelX = startX - 70;
//        addDrawableChild(new TextWidget(labelX, startY + 4, textRenderer.getWidth("消耗速率(/秒/标记)"), 9, Text.literal("消耗速率(/秒/标记)"), textRenderer));
//        addDrawableChild(new TextWidget(labelX, startY + ROW_HEIGHT + 4, textRenderer.getWidth("回复速率(/秒)"), 9, Text.literal("回复速率(/秒)"), textRenderer));
//        addDrawableChild(new TextWidget(labelX, startY + 2 * ROW_HEIGHT + 4, textRenderer.getWidth("范围(格)"), 9, Text.literal("范围(格)"), textRenderer));
//        addDrawableChild(new TextWidget(labelX, startY + 3 * ROW_HEIGHT + 4, textRenderer.getWidth("最大标记"), 9, Text.literal("最大标记"), textRenderer));
//
//        for (int stage = 1; stage <= STAGES; stage++) {
//            int x = startX + (stage - 1) * COLUMN_WIDTH;
//
//            TextFieldWidget drain = new TextFieldWidget(textRenderer, x, startY, INPUT_WIDTH, 18, Text.empty());
//            drain.setText(String.valueOf(config.getEnergyDrain(stage)));
//            drain.setMaxLength(6);
//            drain.setTooltip(Tooltip.of(Text.literal("阶段" + stage + " - 每个标记实体每秒消耗的能量值")));
//
//            TextFieldWidget regen = new TextFieldWidget(textRenderer, x, startY + ROW_HEIGHT, INPUT_WIDTH, 18, Text.empty());
//            regen.setText(String.valueOf(config.getEnergyRegen(stage)));
//            regen.setMaxLength(6);
//            regen.setTooltip(Tooltip.of(Text.literal("阶段" + stage + " - 非诱导期间每秒回复的能量值")));
//
//            TextFieldWidget radius = new TextFieldWidget(textRenderer, x, startY + 2 * ROW_HEIGHT, INPUT_WIDTH, 18, Text.empty());
//            radius.setText(String.valueOf(config.getRadius(stage)));
//            radius.setMaxLength(6);
//            radius.setTooltip(Tooltip.of(Text.literal("阶段" + stage + " - 诱导焦点的吸引范围（单位：格）")));
//
//            TextFieldWidget marks = new TextFieldWidget(textRenderer, x, startY + 3 * ROW_HEIGHT, INPUT_WIDTH, 18, Text.empty());
//            marks.setText(String.valueOf(config.getMaxMarks(stage)));
//            marks.setMaxLength(3);
//            marks.setTooltip(Tooltip.of(Text.literal("阶段" + stage + " - 同时存在的最大标记实体数量")));
//
//            stageColumns.add(new TextFieldWidget[]{drain, regen, radius, marks});
//            addDrawableChild(drain);
//            addDrawableChild(regen);
//            addDrawableChild(radius);
//            addDrawableChild(marks);
//        }
//
//        // 阶段编号
//        int stageNumberY = startY + 4 * ROW_HEIGHT + 2;
//        for (int stage = 1; stage <= STAGES; stage++) {
//            int x = startX + (stage - 1) * COLUMN_WIDTH + INPUT_WIDTH / 2;
//            String text = "阶段" + stage;
//            int textWidth = textRenderer.getWidth(text);
//            TextWidget stageLabel = new TextWidget(x - textWidth / 2, stageNumberY, textWidth, 9, Text.literal(text), textRenderer);
//            stageLabel.setTooltip(Tooltip.of(Text.literal("阶段" + stage + "的各项配置")));
//            addDrawableChild(stageLabel);
//        }
//
//        // 保存和返回按钮
//        addDrawableChild(ButtonWidget.builder(Text.literal("保存"), btn -> saveConfig())
//                .dimensions(width / 2 - 50, height - 30, 100, 20).build());
//        addDrawableChild(ButtonWidget.builder(Text.literal("返回"), btn -> close())
//                .dimensions(width / 2 - 50, height - 60, 100, 20).build());
//    }
//
//    private void saveConfig() {
//        // 新建配置对象，并从 currentConfig 复制阶段名称，然后更新数值
//        GazeConfig newConfig = new GazeConfig();
//        newConfig.maxEnergy = currentConfig.maxEnergy;
//        for (int i = 0; i < STAGES; i++) {
//            newConfig.stages[i] = new GazeConfig.StageConfig();
//            newConfig.stages[i].name = currentConfig.stages[i].name;
//        }
//        for (int i = 0; i < stageColumns.size(); i++) {
//            int stage = i + 1;
//            TextFieldWidget[] col = stageColumns.get(i);
//            try {
//                double drain = Double.parseDouble(col[0].getText());
//                double regen = Double.parseDouble(col[1].getText());
//                double radius = Double.parseDouble(col[2].getText());
//                int marks = Integer.parseInt(col[3].getText());
//                newConfig.setEnergyDrain(stage, drain);
//                newConfig.setEnergyRegen(stage, regen);
//                newConfig.setRadius(stage, radius);
//                newConfig.setMaxMarks(stage, marks);
//            } catch (NumberFormatException ignored) {}
//        }
//        // 发送 JSON 字符串到服务端
//        ClientPlayNetworking.send(new UpdateConfigC2SPacket(newConfig.toJson()));
//        if (client != null && client.player != null)
//            client.player.sendMessage(Text.literal("§e配置已提交，正在同步..."), true);
//    }
//
//    @Override
//    public void close() {
//        if (client != null) client.setScreen(parent);
//    }
//
//    @Override
//    public boolean shouldPause() {
//        return false;
//    }
//
//    // 注册同步包接收器，需在客户端初始化时调用一次
//    public static void registerSyncReceiver() {
//        ClientPlayNetworking.registerGlobalReceiver(SyncConfigS2CPacket.ID, (packet, context) -> {
//            context.client().execute(() -> {
//                GazeConfig config = GazeConfig.fromJson(packet.json());
//                if (context.client().currentScreen instanceof StageConfigScreen screen) {
//                    screen.rebuildUi(config);
//                }
//            });
//        });
//    }
//}