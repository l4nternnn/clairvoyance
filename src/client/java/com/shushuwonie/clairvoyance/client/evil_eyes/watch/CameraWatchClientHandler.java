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
    private static boolean reflectOk = false;
    private static Field px, py, pz, pyw, pp;

    static {
        for (Field f : Entity.class.getDeclaredFields()) {
            String n = f.getName();
            if (!reflectOk && n.endsWith("prevX")) { px = f; px.setAccessible(true); }
            if (!reflectOk && n.endsWith("prevY")) { py = f; py.setAccessible(true); }
            if (!reflectOk && n.endsWith("prevZ")) { pz = f; pz.setAccessible(true); }
            if (!reflectOk && n.endsWith("prevYaw")) { pyw = f; pyw.setAccessible(true); }
            if (!reflectOk && n.endsWith("prevPitch")) { pp = f; pp.setAccessible(true); }
            if (px != null && py != null && pz != null && pyw != null && pp != null) {
                reflectOk = true;
                break;
            }
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

            Vec3d current = dummyCamera.getPos();
            double nx = current.x + (targetPos.x - current.x) * 0.7;
            double ny = current.y + (targetPos.y - current.y) * 0.7;
            double nz = current.z + (targetPos.z - current.z) * 0.7;
            float nyaw = dummyCamera.getYaw() + MathHelper.wrapDegrees(targetYaw - dummyCamera.getYaw()) * 0.6f;
            float npitch = dummyCamera.getPitch() + MathHelper.clamp(targetPitch - dummyCamera.getPitch(), -180, 180) * 0.6f;

            if (reflectOk) {
                double ox = dummyCamera.getX(), oy = dummyCamera.getY(), oz = dummyCamera.getZ();
                float oyaw = dummyCamera.getYaw(), opitch = dummyCamera.getPitch();
                dummyCamera.setPos(nx, ny, nz);
                dummyCamera.setYaw(nyaw);
                dummyCamera.setPitch(npitch);
                try { px.setDouble(dummyCamera, ox); py.setDouble(dummyCamera, oy); pz.setDouble(dummyCamera, oz); } catch (Exception ignored) {}
                try { pyw.setFloat(dummyCamera, oyaw); pp.setFloat(dummyCamera, opitch); } catch (Exception ignored) {}
            } else {
                dummyCamera.refreshPositionAndAngles(nx, ny, nz, nyaw, npitch);
            }

            if (client.cameraEntity != dummyCamera) {
                client.cameraEntity = dummyCamera;
            }
        });
    }
}