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
    private static final Map<UUID, Float> smoothYaws = new ConcurrentHashMap<>();

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
        UUID uuid = viewer.getUuid();
        SESSIONS.remove(uuid);
        smoothYaws.remove(uuid);
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

        // 对实体 yaw 做平滑，水平方向跟随实体朝向
        UUID viewerId = viewer.getUuid();
        float targetYaw = target.getYaw();
        float prevSmoothYaw = smoothYaws.getOrDefault(viewerId, targetYaw);
        float lerpFactor = 0.3f;
        float smoothYaw = prevSmoothYaw + MathHelper.wrapDegrees(targetYaw - prevSmoothYaw) * lerpFactor;
        smoothYaws.put(viewerId, smoothYaw);

        // 摄像机位置：实体背后水平方向 * 距离 + 固定高度偏移
        Vec3d targetEye = target.getEyePos();
        double radYaw = Math.toRadians(smoothYaw);
        double behindX = -Math.sin(radYaw);   // lookDir.x 的反方向
        double behindZ = -Math.cos(radYaw);   // lookDir.z 的反方向
        Vec3d posBase = target.getPos().add(-behindX * distance, 1.5, -behindZ * distance);
        Vec3d idealEnd = posBase;

        // 射线检测（从实体位置到摄像机位置）
        RaycastContext ctx = new RaycastContext(targetEye, idealEnd,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, target);
        BlockHitResult hit = world.raycast(ctx);
        Vec3d camPos;
        if (hit.getType() == HitResult.Type.BLOCK) {
            double hitDist = hit.getPos().distanceTo(targetEye);
            double finalDist = Math.max(0.5, Math.min(distance, hitDist - 0.2));
            camPos = target.getPos().add(-behindX * finalDist, 1.5, -behindZ * finalDist);
        } else {
            camPos = idealEnd;
        }

        // 摄像机始终看向实体眼睛
        double dx = targetEye.x - camPos.x;
        double dy = targetEye.y - camPos.y;
        double dz = targetEye.z - camPos.z;
        float camYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);
        camYaw = MathHelper.wrapDegrees(camYaw);
        float camPitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
        camPitch = MathHelper.clamp(camPitch, -90, 90);

        ServerPlayNetworking.send(viewer, new CameraUpdateS2CPacket(camPos, camYaw, camPitch));
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