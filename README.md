# 千里眼 Clairvoyance

[![Release](https://img.shields.io/github/v/release/l4nternnn/clairvoyance)](https://github.com/l4nternnn/clairvoyance/releases)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8-brightgreen)](https://www.minecraft.net/)
[![Loader](https://img.shields.io/badge/Fabric-0.18.3-blue)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-CC0--1.0-lightgrey)](LICENSE)

Minecraft 1.21.8 Fabric 模组，提供千里眼实体追踪、视线诱导、视角跟随等功能。

## 快速开始

1. 安装 Fabric Loader 0.18.3+ 和 Fabric API
2. 将模组 JAR 放入 `mods/` 文件夹
3. 服务端和客户端都需要安装

## 功能

### 千里眼 Evil Eyes

七阶段计分板成长型实体标记追踪系统。

| 操作 | 方法 |
|---|---|
| 获取物品 | 创造模式 "工具" 分类中找到千里眼物品 |
| 标记实体 | 手持千里眼，准星对准生物按 `V` |
| 取消标记 | 对已标记实体再按一次 `V` |
| 放置锚点 | 手持千里眼，准星对准方块表面按 `V`（锚点自动标记注视它的实体） |
| 打开标记列表 | 手持千里眼按**右键** |
| 观看实体视角 | 在列表中**点击实体名** |
| 退出观看 | 按 `Shift` |
| 破坏锚点 | 攻击名为 `clairvoyance_evil_eyes` 的小盔甲架 |
| 配置界面 | 按 `U`（仅创造模式），编辑各阶段参数 |

阶段通过计分板 `monvhua` 提升（0-100 分 → 阶段 1-7），分数越高限制越宽松。

### 视线诱导 Gaze Guidance

基于能量的聚焦标记系统，被标记实体受焦点吸引。

| 操作 | 方法 |
|---|---|
| 获取物品 | 创造模式 "工具" 分类中找到魔法棒 |
| 标记实体 | 手持魔法棒，对准生物按 `V` |
| 取消标记 | 对已标记实体再按一次 `V` |
| 启动诱导 | 手持魔法棒**按住右键**，准星对准实体/方块 |
| 结束诱导 | 松开右键 |
| 配置界面 | 按 `U`（创造模式），切换到"视线诱导配置" |

能量条在屏幕底部显示，标记越多消耗越快，能量耗尽自动停止。

### /watch 视角跟随

独立于千里眼物品的视角跟随指令。

```
/watch                 观看准星指向的实体（再次执行停止）
/watch <玩家名>         观看指定玩家
/watch angle <yaw> <pitch> <dist>  调整摄像机偏移
/watch angle reset     重置偏移
```

- 第三人称视角，摄像机在目标背后，镜头始终指向目标
- 实体转身时摄像机平滑跟随，不绕圈
- 按 `Shift` 退出
- 观看模式下攻击/使用/破坏被禁止，手部模型自动隐藏

### 打开背包

查看和操作其他玩家的背包。

| 条件 | 操作 |
|---|---|
| 创造模式 | 空手对准玩家按 `V` |
| 非创造模式 | 需要 `kaibao` 标签（`/tag <玩家> add kaibao`），空手对准玩家按 `V` |

### 抱起实体

抱起并搬运活体生物或玩家。

| 条件 | 操作 |
|---|---|
| 创造模式 | 潜行 + 空手 + 对准目标按 `V` |
| 非创造模式 | 需要 `kebao` 标签，同上 |

放下：右键 / 手里拿物品 / 打开背包。被抱玩家按潜行可挣脱。

## 指令参考

```
/watch                 观看/停止观看
/watch <玩家名>         观看指定玩家
/watch angle <yaw> <pitch> <dist>  调整偏移
/watch angle reset     重置偏移

/clairvoyance clearanchors_清除千里眼锚点           清除自己的锚点
/clairvoyance clearanchors_清除千里眼锚点 <玩家>     清除指定玩家锚点（OP）
/clairvoyance clearanchors_清除千里眼锚点 all        清除全部锚点（OP）

/clairvoyance resetcooldown_重置冷却 [玩家]          重置诱导冷却
/clairvoyance clearmarks_清除诱导标记实体            清除全部标记（OP）
/clairvoyance toggleimages__开|关图片 <玩家>         开关背部贴图（OP）
/clairvoyance toggleparticles__开|关粒子 <玩家>      开关粒子环（OP）
```

## 构建

```bash
./gradlew build          # 构建模组
./gradlew runClient      # 启动客户端测试
./gradlew runServer      # 启动服务端测试
```

要求：JDK 21

## 依赖

- Fabric Loader >= 0.18.3
- Fabric API
- Minecraft 1.21.8

## 许可

CC0-1.0
