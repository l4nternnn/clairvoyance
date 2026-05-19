package com.shushuwonie.clairvoyance.features.evil_eyes.server;

import com.shushuwonie.clairvoyance.network.camerawatch.CameraUpdateS2CPacket;
import com.shushuwonie.clairvoyance.network.camerawatch.CameraWatchUnbindS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CameraWatchManager {
    private static final Map<UUID, CameraSession> SESSIONS = new ConcurrentHashMap<>();

    private record CameraSession(UUID targetUuid, long startTick) {}

    public static boolean isWatching(ServerPlayerEntity player) {
        return SESSIONS.containsKey(player.getUuid());
    }

    public static void startWatching(ServerPlayerEntity viewer, UUID targetUuid, MinecraftServer server) {
        ServerWorld world = viewer.getWorld();
        Entity target = world.getEntity(targetUuid);
        if (target == null || !target.isAlive()) {
            viewer.sendMessage(Text.literal("§c目标不存在或已死亡"), true);
            return;
        }
        stopWatching(viewer, server);
        SESSIONS.put(viewer.getUuid(), new CameraSession(targetUuid, world.getTime()));
        viewer.sendMessage(Text.literal("§a正在观看 " + target.getName().getString()), true);
    }

    public static void stopWatching(ServerPlayerEntity viewer, MinecraftServer server) {
        SESSIONS.remove(viewer.getUuid());
        ServerPlayNetworking.send(viewer, new CameraWatchUnbindS2CPacket());
    }

    public static void tick(MinecraftServer server) {
        for (Map.Entry<UUID, CameraSession> entry : SESSIONS.entrySet()) {
            ServerPlayerEntity viewer = server.getPlayerManager().getPlayer(entry.getKey());
            if (viewer == null) continue;
            updateCameraForViewer(viewer, server);
        }
    }

    private static void updateCameraForViewer(ServerPlayerEntity viewer, MinecraftServer server) {
        CameraSession session = SESSIONS.get(viewer.getUuid());
        if (session == null) return;
        ServerWorld world = viewer.getWorld();
        Entity target = world.getEntity(session.targetUuid);
        if (target == null || !target.isAlive()) {
            stopWatching(viewer, server);
            viewer.sendMessage(Text.literal("§c目标已消失"), true);
            return;
        }

        CameraOffset offset = getOffset(viewer);
        double distance = offset.distance;

        // 基于实体实际视线方向计算摄像机位置，而非固定世界坐标偏移
        Vec3d targetEye = target.getEyePos();
        Vec3d lookDir = target.getRotationVec(1.0f);
        Vec3d behindDir = lookDir.multiply(-1); // 实体背后方向
        Vec3d idealEnd = targetEye.add(behindDir.multiply(distance));

        RaycastContext ctx = new RaycastContext(targetEye, idealEnd,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, target);
        BlockHitResult hit = world.raycast(ctx);
        Vec3d camPos;
        if (hit.getType() == HitResult.Type.BLOCK) {
            double hitDistance = hit.getPos().distanceTo(targetEye);
            double finalDistance = Math.max(0.5, Math.min(distance, hitDistance - 0.2));
            camPos = targetEye.add(behindDir.multiply(finalDistance));
        } else {
            camPos = idealEnd;
        }

        // 摄像机方向与被观看实体视线方向一致
        float yaw = target.getYaw();
        float pitch = target.getPitch();

        ServerPlayNetworking.send(viewer, new CameraUpdateS2CPacket(camPos, yaw, pitch));
    }

    // 个性化偏移存储
    private static final Map<UUID, CameraOffset> PLAYER_OFFSETS = new ConcurrentHashMap<>();

    public static class CameraOffset {
        public float yaw;
        public float pitch;
        public double distance;

        public CameraOffset(float yaw, float pitch, double distance) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.distance = distance;
        }
    }

    public static void setOffset(ServerPlayerEntity player, float yaw, float pitch, double distance) {
        PLAYER_OFFSETS.put(player.getUuid(), new CameraOffset(yaw, pitch, distance));
    }

    public static void resetOffset(ServerPlayerEntity player) {
        PLAYER_OFFSETS.remove(player.getUuid());
    }

    private static CameraOffset getOffset(ServerPlayerEntity player) {
        return PLAYER_OFFSETS.getOrDefault(player.getUuid(),
                new CameraOffset(0f, 0f, 4.0f)); // 默认背后4格，方向随实体视线
    }
}