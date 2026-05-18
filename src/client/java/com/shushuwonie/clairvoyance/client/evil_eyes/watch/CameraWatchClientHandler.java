package com.shushuwonie.clairvoyance.client.evil_eyes.watch;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class CameraWatchClientHandler {

    private static Entity dummyCamera = null;
    private static Vec3d targetPos = null;
    private static float targetYaw = 0, targetPitch = 0;
    private static long lastUpdateTime = 0;
    private static boolean hasValidTarget = false;

    public static void onCameraUpdate(Vec3d pos, float yaw, float pitch) {
        // 拒绝原点坐标，防止闪回 (0,0,0)
        if (pos.x == 0.0 && pos.y == 0.0 && pos.z == 0.0) return;
        targetPos = pos;
        targetYaw = yaw;
        targetPitch = pitch;
        lastUpdateTime = System.currentTimeMillis();
        hasValidTarget = true;
    }

    public static void onUnbind() {
        dummyCamera = null;
        hasValidTarget = false;
        targetPos = null;
        MinecraftClient.getInstance().cameraEntity = MinecraftClient.getInstance().player;
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null) return;
            if (!hasValidTarget) return;

            if (dummyCamera == null && targetPos != null) {
                dummyCamera = new Entity(EntityType.ARMOR_STAND, client.world) {
                    @Override protected void initDataTracker(DataTracker.Builder builder) {}
                    @Override public boolean damage(ServerWorld world, DamageSource source, float amount) { return false; }
                    @Override protected void readCustomData(ReadView view) {}
                    @Override protected void writeCustomData(WriteView view) {}
                };
                dummyCamera.setInvisible(true);
                dummyCamera.setPosition(targetPos);
                dummyCamera.setYaw(targetYaw);
                dummyCamera.setPitch(targetPitch);
                client.cameraEntity = dummyCamera;
                return;
            }
            if (dummyCamera == null) return;

            // 平滑插值
            long now = System.currentTimeMillis();
            float expectedInterval = 100.0f;
            float delta = Math.min(1.0f, (now - lastUpdateTime) / expectedInterval);
            delta = Math.max(0.05f, delta);
            Vec3d current = dummyCamera.getPos();
            Vec3d newPos = current.lerp(targetPos, delta);
            dummyCamera.setPosition(newPos);

            float maxStep = 15.0f;
            float yawDiff = targetYaw - dummyCamera.getYaw();
            yawDiff = MathHelper.clamp(yawDiff, -maxStep, maxStep);
            yawDiff = (yawDiff % 360 + 360) % 360;
            if (yawDiff > 180) yawDiff -= 360;
            dummyCamera.setYaw(dummyCamera.getYaw() + yawDiff * delta);

            float pitchDiff = targetPitch - dummyCamera.getPitch();
            pitchDiff = MathHelper.clamp(pitchDiff, -180, 180);
            dummyCamera.setPitch(dummyCamera.getPitch() + pitchDiff * delta);

            // 防止 cameraEntity 被外部重置
            if (client.cameraEntity != dummyCamera) {
                client.cameraEntity = dummyCamera;
            }
        });
    }
}