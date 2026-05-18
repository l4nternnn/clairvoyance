# 千里眼 Clairvoyance

[![Release](https://img.shields.io/github/v/release/l4nternnn/clairvoyance)](https://github.com/l4nternnn/clairvoyance/releases)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8-brightgreen)](https://www.minecraft.net/)
[![Loader](https://img.shields.io/badge/Fabric-0.18.3-blue)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-CC0--1.0-lightgrey)](LICENSE)

Minecraft 1.21.8 Fabric 模组，为服务器管理提供多种辅助功能。

## 功能

### 千里眼 (Evil Eyes)
多阶段标记系统。手持千里眼物品可标记实体，观察被标记目标的视角。阶段越高，功能和限制越宽松（共 7 个阶段）。

- 按键：`V`（默认），手持千里眼物品时右键方块放置鹦鹉锚点
- 配置：`U` 键打开配置界面（创造模式）

### 视线诱导 (Gaze Guidance)
基于能量的实体标记与聚焦系统。手持魔法棒对准生物按 `V` 即可标记，通过计分板自动提升阶段。

### 打开背包
允许查看其他玩家的背包。需要 `kaibao` 标签或创造模式，对着玩家按 `V` 即可打开。

### 抱起实体
抱起并搬运活体生物或玩家。需要 `kebao` 标签或创造模式。
- 抱起：潜行时空手按 `V` 对准目标
- 放下：右键或切物品栏

## 指令

- `/watch start <玩家>` — 开始观看玩家视角
- `/watch stop` — 停止观看
- `/clairvoyance` — 查看可用的配置指令

## 构建

```bash
./gradlew build          # 构建模组
./gradlew runClient      # 启动客户端测试
./gradlew runServer      # 启动服务端测试
```

要求：JDK 21、Gradle（自动通过 wrapper 下载）

## 依赖

- Fabric Loader >= 0.18.3
- Fabric API
- Minecraft 1.21.8

## 许可证

CC0-1.0
