package com.shushuwonie.clairvoyance.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalConfigManager {
    private static final int STAGES = 7;
    private final Map<Integer, StageConfig> configs = new HashMap<>();
    private final Map<UUID, DailyUsage> dailyUsage = new ConcurrentHashMap<>();
    private final Map<UUID, DailyParrotUsage> parrotDailyUsage = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> activeParrots = new ConcurrentHashMap<>();
    private long lastResetDay = 0;

    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("clairvoyance_global.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // 阶段配置（不含鹦鹉限制）
    public record StageConfig(int dailyLimit, int maxMarks, int minScore, int maxScore, int watchRequiredTicks, int parrotDailyLimit, int maxActiveParrots) {}


    private static class DailyUsage {
        int[] used = new int[STAGES + 1];
    }
    private static class DailyParrotUsage {
        int[] used = new int[STAGES + 1];
    }

    public GlobalConfigManager() {
        load();
    }

    private void load() {
        if (CONFIG_PATH.toFile().exists()) {
            try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
                ConfigData data = GSON.fromJson(reader, ConfigData.class);
                for (int i = 1; i <= STAGES; i++) {
                    StageConfig cfg = data.configs.get(i);
                    configs.put(i, cfg != null ? cfg : getDefaultConfig(i));
                }
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (int i = 1; i <= STAGES; i++) configs.put(i, getDefaultConfig(i));
        save();
    }

    public void save() {
        ConfigData data = new ConfigData();
        data.configs = new HashMap<>(configs);
        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private StageConfig getDefaultConfig(int stage) {
        return switch (stage) {
            case 1 -> new StageConfig(10, 3, 0, 5, 40,2,2);
            case 2 -> new StageConfig(10, 4, 6, 20, 40,3,2);
            case 3 -> new StageConfig(8, 5, 21, 40, 40,5,2);
            case 4 -> new StageConfig(8, 6, 41, 60, 40,7,2);
            case 5 -> new StageConfig(6, 7, 61, 70, 40,8,2);
            case 6 -> new StageConfig(6, 8, 71, 80, 40,9,2);
            case 7 -> new StageConfig(5, 10, 81, 100, 40,10,2);
            default -> new StageConfig(10, 5, 0, 100, 40,12,2);
        };
    }

    public StageConfig getStageConfig(int stage) {
        return configs.getOrDefault(stage, getDefaultConfig(stage));
    }

    public void updateStageConfig(int stage, int dailyLimit, int maxMarks, int minScore, int maxScore, int watchRequiredTicks, int parrotDailyLimit, int maxActiveParrots) {
        configs.put(stage, new StageConfig(dailyLimit, maxMarks, minScore, maxScore, watchRequiredTicks,parrotDailyLimit,maxActiveParrots));
        save();
    }

    // 观看次数
    public boolean canMark(UUID playerUuid, int stage) {
        checkDailyReset();
        DailyUsage usage = dailyUsage.computeIfAbsent(playerUuid, k -> new DailyUsage());
        return usage.used[stage] < getStageConfig(stage).dailyLimit();
    }

    public void recordMark(UUID playerUuid, int stage) {
        checkDailyReset();
        DailyUsage usage = dailyUsage.computeIfAbsent(playerUuid, k -> new DailyUsage());
        usage.used[stage]++;
    }

    // 鹦鹉限制：硬编码（每日最多放置5次，同时最多存在3只）
    private static final int PARROT_DAILY_LIMIT = 5;
    private static final int MAX_ACTIVE_PARROTS = 3;

    // 修改 canPlaceParrot 方法，使用阶段限制
// 修改 canPlaceParrot，使用阶段配置中的最大同时存在数
    public boolean canPlaceParrot(UUID playerUuid, int stage) {
        checkDailyReset();
        DailyParrotUsage usage = parrotDailyUsage.computeIfAbsent(playerUuid, k -> new DailyParrotUsage());
        StageConfig cfg = getStageConfig(stage);
        int dailyLimit = cfg.parrotDailyLimit();
        int maxActive = cfg.maxActiveParrots();
        int currentActive = activeParrots.getOrDefault(playerUuid, 0);
        return usage.used[stage] < dailyLimit && currentActive < maxActive;
    }

    // 添加到 GlobalConfigManager 类中
    public String getConfigJson() {
        Map<String, StageConfig> stageMap = new LinkedHashMap<>();
        for (int i = 1; i <= STAGES; i++) {
            stageMap.put("stage" + i, configs.get(i));
        }
        return GSON.toJson(stageMap);
    }

    public void updateConfig(int stage, int dailyLimit, int maxMarks, int minScore, int maxScore,
                             int watchRequiredTicks, int parrotDailyLimit, int maxActiveParrots) {
        updateStageConfig(stage, dailyLimit, maxMarks, minScore, maxScore,
                watchRequiredTicks, parrotDailyLimit, maxActiveParrots);
    }

    public void recordPlaceParrot(UUID playerUuid, int stage) {
        checkDailyReset();
        DailyParrotUsage usage = parrotDailyUsage.computeIfAbsent(playerUuid, k -> new DailyParrotUsage());
        usage.used[stage]++;
        activeParrots.put(playerUuid, activeParrots.getOrDefault(playerUuid, 0) + 1);
    }

    public void removeActiveParrot(UUID playerUuid) {
        activeParrots.computeIfPresent(playerUuid, (k, v) -> v > 0 ? v - 1 : 0);
    }

    public int getStageByScore(int score) {
        for (int i = STAGES; i >= 1; i--) {
            StageConfig cfg = getStageConfig(i);
            if (score >= cfg.minScore() && score <= cfg.maxScore()) return i;
        }
        return 1;
    }

    private void checkDailyReset() {
        long currentDay = System.currentTimeMillis() / 86400000L;
        if (currentDay != lastResetDay) {
            dailyUsage.clear();
            parrotDailyUsage.clear();
            lastResetDay = currentDay;
        }
    }
}

class ConfigData {
    Map<Integer, GlobalConfigManager.StageConfig> configs;
}