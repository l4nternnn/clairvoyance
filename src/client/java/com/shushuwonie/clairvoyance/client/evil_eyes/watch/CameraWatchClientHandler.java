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

import java.lang.reflect.Field;

public class CameraWatchClientHandler {

    private static Entity dummyCamera = null;
    private static Vec3d targetPos = null;
    private static float targetYaw = 0, targetPitch = 0;
    private static boolean hasValidTarget = false;

    private static Field prevXField, prevYField, prevZField, prevYawField, prevPitchField;

    static {
        try {
            prevXField = Entity.class.getDeclaredField("prevX");
            prevYField = Entity.class.getDeclaredField("prevY");
            prevZField = Entity.class.getDeclaredField("prevZ");
            prevYawField = Entity.class.getDeclaredField("prevYaw");
            prevPitchField = Entity.class.getDeclaredField("prevPitch");
            prevXField.setAccessible(true);
            prevYField.setAccessible(true);
            prevZField.setAccessible(true);
            prevYawField.setAccessible(true);
            prevPitchField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            // 字段名可能不同，静默处理
        }
    }

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
                dummyCamera.refreshPositionAndAngles(targetPos.x, targetPos.y, targetPos.z, targetYaw, targetPitch);
                client.cameraEntity = dummyCamera;
                return;
            }

            // 保存当前值作为新的 prev，让 Minecraft 的 getLerpedPos 做帧间插值
            double oldX = dummyCamera.getX();
            double oldY = dummyCamera.getY();
            double oldZ = dummyCamera.getZ();
            float oldYaw = dummyCamera.getYaw();
            float oldPitch = dummyCamera.getPitch();

            // 计算新位置（高响应 lerp）
            Vec3d current = dummyCamera.getPos();
            double nx = current.x + (targetPos.x - current.x) * 0.7;
            double ny = current.y + (targetPos.y - current.y) * 0.7;
            double nz = current.z + (targetPos.z - current.z) * 0.7;
            float nyaw = oldYaw + MathHelper.wrapDegrees(targetYaw - oldYaw) * 0.6f;
            float npitch = oldPitch + MathHelper.clamp(targetPitch - oldPitch, -180, 180) * 0.6f;

            // 只设当前值，通过反射设 prev 为旧值
            dummyCamera.setPos(nx, ny, nz);
            dummyCamera.setYaw(nyaw);
            dummyCamera.setPitch(npitch);
            setPrev(oldX, oldY, oldZ, oldYaw, oldPitch);

            if (client.cameraEntity != dummyCamera) {
                client.cameraEntity = dummyCamera;
            }
        });
    }

    private static void setPrev(double x, double y, double z, float yaw, float pitch) {
        try {
            if (prevXField != null) { prevXField.setDouble(dummyCamera, x); prevYField.setDouble(dummyCamera, y); prevZField.setDouble(dummyCamera, z); }
        } catch (Exception ignored) {}
        try {
            if (prevYawField != null) { prevYawField.setFloat(dummyCamera, yaw); prevPitchField.setFloat(dummyCamera, pitch); }
        } catch (Exception ignored) {}
    }
}