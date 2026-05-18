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
    private static boolean hasValidTarget = false;

    public static void onCameraUpdate(Vec3d pos, float yaw, float pitch) {
        targetPos = pos;
        targetYaw = yaw;
        targetPitch = pitch;
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
            if (client.world == null || client.player == null) return;
            if (!hasValidTarget || targetPos == null) return;

            if (dummyCamera == null) {
                dummyCamera = new Entity(EntityType.ARMOR_STAND, client.world) {
                    @Override protected void initDataTracker(DataTracker.Builder builder) {}
                    @Override public boolean damage(ServerWorld world, DamageSource source, float amount) { return false; }
                    @Override protected void readCustomData(ReadView view) {}
                    @Override protected void writeCustomData(WriteView view) {}
                };
                dummyCamera.setInvisible(true);
                // refreshPositionAndAngles 同时设置 prevX/Y/Z，避免 getLerpedPos 在 (0,0,0) 和当前位置之间插值
                dummyCamera.refreshPositionAndAngles(targetPos.x, targetPos.y, targetPos.z, targetYaw, targetPitch);
                client.cameraEntity = dummyCamera;
                return;
            }

            // 固定步长平滑
            Vec3d current = dummyCamera.getPos();
            Vec3d newPosXZ = new Vec3d(
                    MathHelper.lerp(0.3, current.x, targetPos.x),
                    MathHelper.lerp(0.3, current.y, targetPos.y),
                    MathHelper.lerp(0.3, current.z, targetPos.z)
            );
            float newYaw = dummyCamera.getYaw() + MathHelper.wrapDegrees(targetYaw - dummyCamera.getYaw()) * 0.3f;
            float newPitch = dummyCamera.getPitch() + MathHelper.clamp(targetPitch - dummyCamera.getPitch(), -180, 180) * 0.3f;

            // 使用 refreshPositionAndAngles 确保 prev 坐标同步，消除渲染抖动
            dummyCamera.refreshPositionAndAngles(newPosXZ.x, newPosXZ.y, newPosXZ.z, newYaw, newPitch);

            if (client.cameraEntity != dummyCamera) {
                client.cameraEntity = dummyCamera;
            }
        });
    }
}