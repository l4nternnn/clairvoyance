package com.shushuwonie.clairvoyance.features.evil_eyes;

import com.shushuwonie.clairvoyance.config.GlobalConfigManager;
import com.shushuwonie.clairvoyance.features.evil_eyes.server.CameraWatchManager;
import com.shushuwonie.clairvoyance.item.evil_eyes.ClairvoyanceItem;
import com.shushuwonie.clairvoyance.network.clairvoyance.*;
//import com.shushuwonie.client.network.clairvoyance.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Evil_Eyes {
    public static final int MAX_MARKED_ENTITIES = 5;
    public static Item CLAIRVOYANCE_ITEM;




    // 玩家标记列表：玩家UUID -> (目标UUID -> 过期时间)
    private static final Map<UUID, Map<UUID, Long>> playerMarkedEntities = new ConcurrentHashMap<>();
    // 手动取消标记冷却：被取消的实体在100 tick内不会被锚点/自动标记重新加回
    private static final Map<UUID, Long> recentlyUnmarked = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger("Clairvoyance");



    // 正在观看的玩家信息
    private static class WatchingInfo {
        UUID targetUuid;
        long startTick;
        boolean counted;
        WatchingInfo(UUID targetUuid, long startTick) {
            this.targetUuid = targetUuid;
            this.startTick = startTick;
            this.counted = false;
        }
    }
    private static final Map<UUID, WatchingInfo> watchingPlayers = new ConcurrentHashMap<>();

    // 盔甲架锚点相关映射
    public static final Map<UUID, UUID> armorStandOwner = new ConcurrentHashMap<>();      // 盔甲架UUID -> 主人UUID
    public static final Map<UUID, Long> armorStandSpawnTick = new ConcurrentHashMap<>();   // 盔甲架UUID -> 生成时刻(游戏刻)

    private static int particleTickCounter = 0;

    public static GlobalConfigManager configManager;

//    static {
//        Identifier id = Identifier.of("clairvoyance", "clairvoyance_item");
//        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
//        Item.Settings settings = new Item.Settings()
//                .registryKey(key)
//                .translationKey("item.clairvoyance.clairvoyance_item")
//                .maxCount(1);
//        CLAIRVOYANCE_ITEM = new ClairvoyanceItem(settings);
//    }
//
//    static {
//        CLAIRVOYANCE_ITEM = new ClairvoyanceItem(new Item.Settings().maxCount(1));
//    }


    // ========== 辅助方法 ==========
    private static final UUID CLEAR_SIGNAL = new UUID(0, 0);

    private static void sendMarkedListToClient(ServerPlayerEntity player, Map<UUID, Long> marks, int maxDisplayCount) {
        // 先发清空信号，客户端收到后清空本地列表
        ServerPlayNetworking.send(player, new EntityMarkedPayload(CLEAR_SIGNAL, ""));
        // 发送全部标记实体
        for (Map.Entry<UUID, Long> entry : marks.entrySet()) {
            UUID uuid = entry.getKey();
            Entity e = player.getWorld().getEntity(uuid);
            String name = (e != null) ? e.getName().getString() : "未知";
            ServerPlayNetworking.send(player, new EntityMarkedPayload(uuid, name));
        }
    }

    private static int getCurrentMarkCount(UUID playerUuid) {
        Map<UUID, Long> marks = playerMarkedEntities.get(playerUuid);
        return marks == null ? 0 : marks.size();
    }

    public static int getPlayerStage(ServerPlayerEntity player, GlobalConfigManager mgr) {
        Scoreboard scoreboard = player.getScoreboard();
        ScoreboardObjective objective = scoreboard.getNullableObjective("monvhua");
        if (objective == null) return 1;
        var score = scoreboard.getScore(player, objective);
        if (score == null) return 1;
        int rawScore = Math.max(0, Math.min(100, score.getScore()));
        return mgr.getStageByScore(rawScore);
    }

    // 保留视线检测方法（虽然后面不用，但其他功能可能调用）
    private static boolean hasLineOfSight(Entity entity, Entity target) {
        World world = entity.getWorld();
        Vec3d start = entity.getEyePos();
        Vec3d end = target.getEyePos();
        RaycastContext context = new RaycastContext(start, end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                entity);
        HitResult result = world.raycast(context);
        return result.getType() == HitResult.Type.MISS ||
                (result.getType() == HitResult.Type.ENTITY && ((EntityHitResult) result).getEntity() == target);
    }

    public static void sendExplosionToNearbyPlayers(Entity stand, MinecraftServer server) {
        if (stand == null) return;
        Vec3d pos = stand.getPos();
        // 向半径 64 格内的所有玩家发送爆炸包
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.squaredDistanceTo(pos) < 4096) {
                ServerPlayNetworking.send(player, new ExplosionParticleS2CPacket(pos));
            }
        }
    }

    public static final Map<UUID, AnchorInfo> anchors = new ConcurrentHashMap<>();

    public static class AnchorInfo {
        public Vec3d pos;
        public long lastSeenTime;
    }

    public static boolean isWatching(ServerPlayerEntity player) {
        return watchingPlayers.containsKey(player.getUuid());
    }

    public static void forceStopWatching(ServerPlayerEntity player, MinecraftServer server) {
        watchingPlayers.remove(player.getUuid());
        ServerPlayNetworking.send(player, new ForceExitViewPayload());
    }

    /**
     * 清除指定玩家的所有锚点盔甲架
     * @param playerUuid 玩家 UUID
     * @return 清除的锚点数量
     */
    public static int clearAnchorsForPlayer(UUID playerUuid, MinecraftServer server) {
        int count = 0;
        World world = server.getWorld(World.OVERWORLD);
        if (world == null) return 0;
        for (Map.Entry<UUID, UUID> entry : armorStandOwner.entrySet().stream().toList()) {
            if (entry.getValue().equals(playerUuid)) {
                UUID standId = entry.getKey();
                Entity stand = world.getEntity(standId);
                if (stand != null) {
                    stand.remove(Entity.RemovalReason.DISCARDED);
                }
                armorStandOwner.remove(standId);
                armorStandSpawnTick.remove(standId);
                count++;
            }
        }
        if (count > 0) {
            for (int i = 0; i < count; i++) {
                configManager.removeActiveParrot(playerUuid);
            }
        }
        return count;
    }

    // ========== 初始化 ==========
    public static void initialize(GlobalConfigManager configManager) {
        Evil_Eyes.configManager = configManager;


//        SelectViewPayload.register();      // C2S
//        MarkEntityPayload.register();      // C2S
//        ExitViewPayload.register();        // C2S
//        ForceExitViewPayload.register();   // C2S

        Identifier id = Identifier.of("clairvoyance", "clairvoyance_item");
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
        Item.Settings settings = new Item.Settings()
                .registryKey(key)
                .maxCount(1);
        CLAIRVOYANCE_ITEM = new ClairvoyanceItem(settings);


        Registry.register(Registries.ITEM, id, CLAIRVOYANCE_ITEM);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(CLAIRVOYANCE_ITEM));



        // 处理客户端请求全局配置
        ServerPlayNetworking.registerGlobalReceiver(RequestGlobalConfigC2SPacket.ID, (packet, context) -> {
            ServerPlayerEntity player = context.player();
            String configJson = configManager.getConfigJson(); // 你需要实现这个方法，将当前配置序列化为 JSON
            ServerPlayNetworking.send(player, new GlobalConfigS2CPacket(configJson));
        });

// 处理客户端更新配置（仅允许 OP 或创造模式玩家执行）
        ServerPlayNetworking.registerGlobalReceiver(UpdateGlobalConfigC2SPacket.ID, (packet, context) -> {
            ServerPlayerEntity player = context.player();
            // 权限检查：建议只允许服主或拥有特定权限的玩家修改
            if (!player.hasPermissionLevel(2)) {
                player.sendMessage(Text.literal("§c你没有权限修改全局配置"), true);
                return;
            }
            // 更新配置管理器中的数据
            configManager.updateConfig(
                    packet.stage(),
                    packet.dailyLimit(),
                    packet.maxMarks(),
                    packet.minScore(),
                    packet.maxScore(),
                    packet.watchRequiredTicks(),
                    packet.parrotDailyLimit(),
                    packet.maxActiveParrots()
            );
            // 保存配置到文件（如果需要持久化）
            configManager.save();
            // 广播给所有在线玩家
            String newConfigJson = configManager.getConfigJson();
            GlobalConfigS2CPacket configPacket = new GlobalConfigS2CPacket(newConfigJson);
            for (ServerPlayerEntity onlinePlayer : player.getServer().getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(onlinePlayer, configPacket);
            }
            player.sendMessage(Text.literal("§a全局配置已更新"), true);
        });



        // 1. 手动标记/取消标记（不消耗每日次数）
        ServerPlayNetworking.registerGlobalReceiver(MarkEntityPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            World world = player.getWorld();
            Entity target = world.getEntityById(payload.entityId());
            if (!(target instanceof LivingEntity) || player.getMainHandStack().getItem() != CLAIRVOYANCE_ITEM) return;

            int stage = getPlayerStage(player, configManager);
            int maxMarks = configManager.getStageConfig(stage).maxMarks();
            if (getCurrentMarkCount(player.getUuid()) >= maxMarks) {
                player.sendMessage(Text.literal("§c已达当前阶段最大标记数量 (" + maxMarks + ")"), true);
                return;
            }

            Map<UUID, Long> marks = playerMarkedEntities.computeIfAbsent(player.getUuid(), k -> new ConcurrentHashMap<>());
            UUID targetUuid = target.getUuid();
            long now = world.getTime();

            if (marks.containsKey(targetUuid)) {
                marks.remove(targetUuid);
                recentlyUnmarked.put(targetUuid, world.getTime() + 100); // 5秒冷却
                player.sendMessage(Text.literal("§e已取消标记 " + target.getName().getString()), true);
                sendMarkedListToClient(player, marks, maxMarks);
            } else {
                if (marks.size() >= MAX_MARKED_ENTITIES) {
                    player.sendMessage(Text.literal("§c标记已达绝对上限 (" + MAX_MARKED_ENTITIES + " 个)"), true);
                    return;
                }
                long expire = now + 40; // 2秒
                marks.put(targetUuid, expire);
                player.sendMessage(Text.literal("§a你感觉 " + target.getName().getString() + " 在§6注视§r你"), true);
                sendMarkedListToClient(player, marks, maxMarks);
            }
        });

// 2. 观看计时消耗每日次数 + 超时强制退出
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long now = server.getWorld(World.OVERWORLD).getTime();
            // 遍历副本避免并发修改
            for (Map.Entry<UUID, WatchingInfo> entry : watchingPlayers.entrySet().stream().toList()) {
                UUID playerUuid = entry.getKey();
                WatchingInfo info = entry.getValue();
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
                if (player == null) continue;

                int stage = getPlayerStage(player, configManager);
                int requiredTicks = configManager.getStageConfig(stage).watchRequiredTicks();
                long elapsed = now - info.startTick;

                if (elapsed >= requiredTicks) {
                    if (!info.counted) {
                        // 第一次达到时长，扣除每日次数
                        configManager.recordMark(playerUuid, stage);
                        player.sendMessage(Text.literal("§e魔力流失，观看消耗次数"), true);
                        info.counted = true;
                    }
                    // 无论是否已扣除，只要达到时长就强制退出观看
                    CameraWatchManager.stopWatching(player, server);
                    ServerPlayNetworking.send(player, new ForceExitViewPayload());
                    watchingPlayers.remove(playerUuid);
                    player.sendMessage(Text.literal("§8头昏脑胀，看来只能看到这了"), true);
                }
            }
        });

        // 3. 退出观看
        ServerPlayNetworking.registerGlobalReceiver(ExitViewPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            playerMarkedEntities.remove(player.getUuid());
            CameraWatchManager.stopWatching(player, player.getServer());
            ServerPlayNetworking.send(player, new ForceExitViewPayload());
            player.sendMessage(Text.literal("§e已退出观看"), true);
        });

        // 4. 选择观看实体
        ServerPlayNetworking.registerGlobalReceiver(SelectViewPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            UUID selected = payload.entityUuid();
            Map<UUID, Long> marks = playerMarkedEntities.get(player.getUuid());
            if (marks == null || !marks.containsKey(selected)) return;
            Long expire = marks.get(selected);
            if (expire == null || expire <= player.getWorld().getTime()) return;

            int stage = getPlayerStage(player, configManager);
            if (!configManager.canMark(player.getUuid(), stage)) {
                player.sendMessage(Text.literal("§c今日观看次数已达上限"), true);
                return;
            }
            watchingPlayers.remove(player.getUuid());
            watchingPlayers.put(player.getUuid(), new WatchingInfo(selected, player.getWorld().getTime()));
            // 启动新相机系统，实现真正的视角跟随
            CameraWatchManager.startWatching(player, selected, player.getServer());
        });

        // 5. 受伤退出
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                if (playerMarkedEntities.containsKey(player.getUuid())) {
                    playerMarkedEntities.remove(player.getUuid());
                    CameraWatchManager.stopWatching(player, player.getServer());
                    ServerPlayNetworking.send(player, new ForceExitViewPayload());
                    player.sendMessage(Text.literal("§c受到攻击，观看中断"), true);
                }
            }
            return true;
        });

        // 6. 清理过期标记和死亡实体
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long now = server.getWorld(World.OVERWORLD).getTime();
            for (Map.Entry<UUID, Map<UUID, Long>> playerEntry : playerMarkedEntities.entrySet()) {
                UUID playerUuid = playerEntry.getKey();
                Map<UUID, Long> marks = playerEntry.getValue();
                boolean changed = false;
                Iterator<Map.Entry<UUID, Long>> it = marks.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<UUID, Long> entry = it.next();
                    UUID entityUuid = entry.getKey();
                    long expire = entry.getValue();
                    Entity entity = server.getWorld(World.OVERWORLD).getEntity(entityUuid);
                    boolean dead = (entity == null || !entity.isAlive());
                    if (expire <= now || dead) {
                        it.remove();
                        changed = true;
                    }
                }
                if (changed) {
                    ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
                    if (player != null) {
                        int stage = getPlayerStage(player, configManager);
                        int maxMarks = configManager.getStageConfig(stage).maxMarks();
                        sendMarkedListToClient(player, marks, maxMarks);
                    }
                }
            }
            playerMarkedEntities.entrySet().removeIf(entry -> entry.getValue().isEmpty());
            recentlyUnmarked.entrySet().removeIf(e -> e.getValue() <= now);
        });

        // 7. 自动标记原有：实体看向手持千里眼的玩家
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 5 != 0) return;
            World world = server.getWorld(World.OVERWORLD);
            if (world == null) return;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (player.getMainHandStack().getItem() != CLAIRVOYANCE_ITEM &&
                        player.getOffHandStack().getItem() != CLAIRVOYANCE_ITEM) continue;
                int stage = getPlayerStage(player, configManager);
                int maxMarks = configManager.getStageConfig(stage).maxMarks();
                Map<UUID, Long> marks = playerMarkedEntities.computeIfAbsent(player.getUuid(), k -> new ConcurrentHashMap<>());
                double range = 30.0;
                Box searchBox = player.getBoundingBox().expand(range);
                for (Entity entity : world.getOtherEntities(player, searchBox, e -> e instanceof LivingEntity && e.isAlive())) {
                    if (marks.containsKey(entity.getUuid())) continue;
                    if (recentlyUnmarked.getOrDefault(entity.getUuid(), 0L) > world.getTime()) continue;
                    if (marks.size() >= maxMarks) continue;
                    if (!hasLineOfSight(entity, player)) continue;
                    Vec3d toPlayer = player.getPos().subtract(entity.getPos()).normalize();
                    Vec3d entityLook = entity.getRotationVec(1.0f);
                    if (entityLook.dotProduct(toPlayer) < 0.5) continue;
                    long now = world.getTime();
                    marks.put(entity.getUuid(), now + 40);
                    sendMarkedListToClient(player, marks, maxMarks);
                    player.sendMessage(Text.literal( entity.getName().getString()+"在注视着你"), true);
                }
            }
        });

        // ==================== 盔甲架锚点功能 ====================
        ServerPlayNetworking.registerGlobalReceiver(PlaceParrotC2SPacket.ID, (packet, context) -> {
            ServerPlayerEntity player = context.player();
            World world = player.getWorld();
            Vec3d pos = packet.pos();

//            LOGGER.info("收到放置盔甲架请求，玩家：{}，位置：{}", player.getName().getString(), pos);
            BlockPos blockPos = BlockPos.ofFloored(pos);
            if (!world.getBlockState(blockPos).isAir() && !world.getBlockState(blockPos).isReplaceable()) {
                player.sendMessage(Text.literal("§c位置不可用"), true);
                return;
            }

            int stage = getPlayerStage(player, configManager);
            if (!configManager.canPlaceParrot(player.getUuid(), stage)) {
                player.sendMessage(Text.literal("§c今日放置锚点次数已达上限或已达最大同时存在数"), true);
//                LOGGER.warn("放置被拒绝：次数或活跃数达上限");
                return;
            }

            ArmorStandEntity armorStand = new ArmorStandEntity(EntityType.ARMOR_STAND, world);
            armorStand.setPosition(pos.x, pos.y, pos.z);
            float angle = 90.0F; // 角度制，需转弧度
            EulerAngle rotation = new EulerAngle((float)Math.toRadians(angle), 0.0F, 0.0F);
// 先设置所有属性（包括反射设置小体型）
            armorStand.getAttributeInstance(EntityAttributes.SCALE).setBaseValue(0.3);   // 缩小到30%
            armorStand.setHideBasePlate(true);
            armorStand.setShowArms(false);
            armorStand.setNoGravity(true);
            armorStand.setSilent(true);
            armorStand.setInvulnerable(false);
            armorStand.setInvisible(false);
            armorStand.setCustomName(Text.literal("clairvoyance_evil_eyes"));
            armorStand.setCustomNameVisible(false);
            armorStand.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(1.0);
            armorStand.setHealth(1.0f);
//            armorStand.setMarker(true);
// 反射设置小体型
            try {
                Method setSmall = ArmorStandEntity.class.getDeclaredMethod("setSmall", boolean.class);
                setSmall.setAccessible(true);
                setSmall.invoke(armorStand, true);
            } catch (Exception e) {
//                LOGGER.warn("无法设置小体型", e);
            }
// 只生成一次
            if (!world.spawnEntity(armorStand)) {
//                LOGGER.error("盔甲架生成失败！");
                player.sendMessage(Text.literal("§c锚点生成失败"), true);
                return;
            }
// 生成成功后记录
            UUID standUuid = armorStand.getUuid();
            UUID ownerUuid = player.getUuid();
            armorStandOwner.put(standUuid, ownerUuid);
            armorStandSpawnTick.put(standUuid, world.getTime());
            configManager.recordPlaceParrot(ownerUuid, stage);
            player.sendMessage(Text.literal("§a锚点已放置"), true);
//            LOGGER.info("盔甲架 {} 已生成，存活: {}", standUuid, armorStand.isAlive());



        });

        // ==================== 盔甲架锚点扫描逻辑（增加视线检测） ====================
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            World world = server.getWorld(World.OVERWORLD);
            if (world == null) return;
            // 在扫描循环中，对每个活着的盔甲架：
            particleTickCounter++;
            if (particleTickCounter % 10 == 0) { // 每 10 tick 发送一次（0.5秒）
                for (UUID standId : armorStandOwner.keySet()) {
                    Entity stand = world.getEntity(standId);
                    if (stand == null || !stand.isAlive()) continue;
                    // 计算胸口位置：盔甲架位置 + 向上 0.6 格（因为小体型时胸口较低，可调整）
                    Vec3d chestPos = stand.getPos().add(0, 0.6, 0);
                    // 发送给附近玩家（以盔甲架为中心半径 64 格内）
                    // 广播给所有附近玩家
                    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                        if (player.squaredDistanceTo(stand) < 4096) { // 64格半径
                            ServerPlayNetworking.send(player, new AnchorParticleS2CPacket(standId, chestPos, 0));
                            ServerPlayNetworking.send(player, new AnchorParticleS2CPacket(standId, chestPos, 1));
                        }
                    }
                    // 也可广播给所有玩家，根据需要
                }
            }


            for (UUID standId : armorStandOwner.keySet().stream().toList()) {
                Entity stand = world.getEntity(standId);
                if (stand == null || !stand.isAlive()) {
                    if (stand != null) sendExplosionToNearbyPlayers(stand, server);
                    UUID owner = armorStandOwner.remove(standId);
                    if (owner != null) configManager.removeActiveParrot(owner);
                    armorStandSpawnTick.remove(standId);
                    continue;
                }

                // 在遍历 armorStandOwner 的循环中，检查超时
                Long spawnTick = armorStandSpawnTick.get(standId);
                if (spawnTick != null && world.getTime() - spawnTick >= 3600) { // 180秒 = 36000 ticks
                    sendExplosionToNearbyPlayers(stand, server);

                    stand.remove(Entity.RemovalReason.DISCARDED);   // 销毁盔甲架
                    UUID owner = armorStandOwner.remove(standId);
                    if (owner != null) configManager.removeActiveParrot(owner);
                    armorStandSpawnTick.remove(standId);
                    // 可选：通知玩家
                    ServerPlayerEntity player = server.getPlayerManager().getPlayer(owner);
                    if (player != null) player.sendMessage(Text.literal("§c锚点已失去联系"), true);
                    continue; // 跳过后续扫描
                }

                UUID ownerId = armorStandOwner.get(standId);
                if (ownerId == null) continue;
                ServerPlayerEntity owner = server.getPlayerManager().getPlayer(ownerId);
                if (owner == null) continue;

                Map<UUID, Long> marks = playerMarkedEntities.computeIfAbsent(ownerId, k -> new ConcurrentHashMap<>());
                int stage = getPlayerStage(owner, configManager);
                int maxMarks = configManager.getStageConfig(stage).maxMarks();
                if (marks.size() >= maxMarks) continue;

                double range = 30.0;
                Box searchBox = stand.getBoundingBox().expand(range);
                List<LivingEntity> nearby = world.getEntitiesByClass(LivingEntity.class, searchBox, e -> e.isAlive() && e != owner && e != stand);

                for (LivingEntity living : nearby) {
                    if (marks.containsKey(living.getUuid())) continue;
                    if (recentlyUnmarked.getOrDefault(living.getUuid(), 0L) > world.getTime()) continue;
                    if (marks.size() >= maxMarks) break;

                    // 1. 视线检测：从实体眼睛到盔甲架是否有遮挡
                    if (!hasLineOfSight(living, stand)) continue;

                    // 2. 方向检测：实体是否面向盔甲架（点积 > 0.5 表示大致看向目标）
                    Vec3d toStand = stand.getPos().subtract(living.getEyePos()).normalize();
                    Vec3d lookVec = living.getRotationVec(1.0f);
                    if (lookVec.dotProduct(toStand) < 0.5) continue;

                    // 通过检测，标记该实体
                    long now = world.getTime();
                    long expire = now + 400;  // 20秒
                    marks.put(living.getUuid(), expire);
                    sendMarkedListToClient(owner, marks, maxMarks);
                    owner.sendMessage(Text.literal("§a[锚点] " + living.getName().getString() + " 在注视着你"), true);
                }
            }
        });

        // 在 Evil_Eyes.initialize 末尾添加
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 == 0) { // 每秒同步一次阶段
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    int stage = getPlayerStage(player, configManager);
                    ServerPlayNetworking.send(player, new PlayerStageS2CPacket(stage));
                }
            }
        });

        // 全局锚点死亡处理器（替代之前的逐个注册方式，避免监听器泄漏）
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ArmorStandEntity)) return;
            UUID standId = entity.getUuid();
            if (!armorStandOwner.containsKey(standId)) return;
            UUID ownerId = armorStandOwner.remove(standId);
            armorStandSpawnTick.remove(standId);
            if (ownerId != null) {
                configManager.removeActiveParrot(ownerId);
            }
            MinecraftServer server = entity.getServer();
            if (server != null) sendExplosionToNearbyPlayers(entity, server);
        });
    }

}