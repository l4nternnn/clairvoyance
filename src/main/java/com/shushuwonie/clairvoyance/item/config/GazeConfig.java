package com.shushuwonie.clairvoyance.item.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public class GazeConfig {
    private static final int STAGES = 7;  // 添加常量
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("gazeguidance.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static GazeConfig instance;

    public StageConfig[] stages = new StageConfig[STAGES];
    public double maxEnergy = 100.0;

    public static class StageConfig {
        public String name = "";
        public double energyDrainPerMark = 1.0;
        public double energyRegenPerSecond = 2.0;
        public double radius = 5.0;
        public int maxMarks = 3;
    }

    public GazeConfig() {
        stages = new StageConfig[STAGES];
        for (int i = 0; i < STAGES; i++) {
            stages[i] = new StageConfig();
        }
        // 其他初始化...
    }

    public static GazeConfig getInstance() {
        if (instance == null) instance = load();
        return instance;
    }

    public static void setInstance(GazeConfig config) {
        instance = config;
        instance.save();
    }

    private static GazeConfig load() {
        if (CONFIG_PATH.toFile().exists()) {
            try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
                GazeConfig config = GSON.fromJson(reader, GazeConfig.class);
                if (config.stages == null || config.stages.length != STAGES) return createDefault();
                return config;
            } catch (IOException e) { e.printStackTrace(); }
        }
        return createDefault();
    }

    private static GazeConfig createDefault() {
        GazeConfig config = new GazeConfig();
        for (int i = 0; i < STAGES; i++) {
            int stage = i + 1;
            config.stages[i] = new StageConfig();
            config.stages[i].name = "阶段 " + stage;
            config.stages[i].energyDrainPerMark = stage;
            config.stages[i].energyRegenPerSecond = 2 + stage;
            config.stages[i].radius = 5 + (stage - 1) * 2.0;
            config.stages[i].maxMarks = stage <= 2 ? 3 : (stage <= 4 ? 6 : 15);
        }
        return config;
    }

    public void save() {
        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(this, writer);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static GazeConfig fromJson(String json) {
        return GSON.fromJson(json, GazeConfig.class);
    }

    // 便捷方法
    public double getEnergyDrain(int stage) { return stages[stage-1].energyDrainPerMark; }
    public double getEnergyRegen(int stage) { return stages[stage-1].energyRegenPerSecond; }
    public double getRadius(int stage) { return stages[stage-1].radius; }
    public int getMaxMarks(int stage) { return stages[stage-1].maxMarks; }

    public void setEnergyDrain(int stage, double val) { stages[stage-1].energyDrainPerMark = val; save(); }
    public void setEnergyRegen(int stage, double val) { stages[stage-1].energyRegenPerSecond = val; save(); }
    public void setRadius(int stage, double val) { stages[stage-1].radius = val; save(); }
    public void setMaxMarks(int stage, int val) { stages[stage-1].maxMarks = val; save(); }
}