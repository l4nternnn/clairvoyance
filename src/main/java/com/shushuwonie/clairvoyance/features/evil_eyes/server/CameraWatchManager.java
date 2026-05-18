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

        // 获取个性化偏移
        CameraOffset offset = getOffset(viewer);
        double distance = offset.distance;
        float yawOffset = offset.yaw;
        float pitchOffset = offset.pitch;

        Vec3d targetEye = target.getEyePos();
        Vec3d idealDir = new Vec3d(
                Math.sin(Math.toRadians(-yawOffset)) * Math.cos(Math.toRadians(pitchOffset)),
                Math.sin(Math.toRadians(pitchOffset)),
                Math.cos(Math.toRadians(-yawOffset)) * Math.cos(Math.toRadians(pitchOffset))
        ).normalize();
        Vec3d idealEnd = targetEye.add(idealDir.multiply(distance));

        RaycastContext ctx = new RaycastContext(targetEye, idealEnd,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, target);
        BlockHitResult hit = world.raycast(ctx);
        Vec3d camPos;
        if (hit.getType() == HitResult.Type.BLOCK) {
            double hitDistance = hit.getPos().distanceTo(targetEye);
            double finalDistance = Math.max(0.5, Math.min(distance, hitDistance - 0.2));
            camPos = targetEye.add(idealDir.multiply(finalDistance));
        } else {
            camPos = idealEnd;
        }
        // 不做服务端平滑，客户端负责全部插值，避免双层平滑振荡
        double dx = target.getX() - camPos.x;
        double dy = target.getEyeY() - camPos.y;
        double dz = target.getZ() - camPos.z;
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);
        yaw = MathHelper.wrapDegrees(yaw);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
        pitch = MathHelper.clamp(pitch, -90, 90);

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
                new CameraOffset(180.0f, 10.0f, 4.0f)); // 默认背后跟随
    }
}