package com.shushuwonie.clairvoyance.client.evil_eyes.watch;

import com.shushuwonie.clairvoyance.network.camerawatch.CameraWatchStopC2SPacket;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
    private static boolean lastSneak = false;

    public static void onCameraUpdate(Vec3d pos, float yaw, float pitch) {
        if (!hasValidTarget) {
            com.shushuwonie.clairvoyance.client.evil_eyes.Evil_EyesClient.exitViewMode(MinecraftClient.getInstance());
        }
        targetPos = pos;
        targetYaw = yaw;
        targetPitch = pitch;
        hasValidTarget = true;
    }

    public static void onUnbind() {
        dummyCamera = null;
        hasValidTarget = false;
        targetPos = null;
        lastSneak = false;
        MinecraftClient.getInstance().cameraEntity = MinecraftClient.getInstance().player;
    }

    public static boolean isActive() {
        return hasValidTarget && dummyCamera != null;
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null || client.player == null) return;

            // Shift 退出观看（上升沿触发，防重复发送）
            if (hasValidTarget && client.player.isSneaking() && !lastSneak) {
                lastSneak = true;
                ClientPlayNetworking.send(new CameraWatchStopC2SPacket());
                return;
            }
            lastSneak = client.player.isSneaking();

            if (!hasValidTarget || targetPos == null) return;

            if (dummyCamera == null) {
                dummyCamera = new Entity(EntityType.ARMOR_STAND, client.world) {
                    @Override protected void initDataTracker(DataTracker.Builder builder) {}
                    @Override public boolean damage(ServerWorld world, DamageSource source, float amount) { return false; }
                    @Override protected void readCustomData(ReadView view) {}
                    @Override protected void writeCustomData(WriteView view) {}
                };
                dummyCamera.setInvisible(true);
                dummyCamera.refreshPositionAndAngles(targetPos.x, targetPos.y, targetPos.z, targetYaw, targetPitch);
                client.cameraEntity = dummyCamera;
                return;
            }

            Vec3d current = dummyCamera.getPos();
            double nx = current.x + (targetPos.x - current.x) * 0.7;
            double ny = current.y + (targetPos.y - current.y) * 0.7;
            double nz = current.z + (targetPos.z - current.z) * 0.7;
            float nyaw = dummyCamera.getYaw() + MathHelper.wrapDegrees(targetYaw - dummyCamera.getYaw()) * 0.6f;
            float npitch = dummyCamera.getPitch() + MathHelper.clamp(targetPitch - dummyCamera.getPitch(), -180, 180) * 0.6f;

            dummyCamera.refreshPositionAndAngles(nx, ny, nz, nyaw, npitch);

            if (client.cameraEntity != dummyCamera) {
                client.cameraEntity = dummyCamera;
            }
        });
    }
}