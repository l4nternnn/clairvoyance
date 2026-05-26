package com.shushuwonie.clairvoyance.client.features.evil_eyes;

import com.shushuwonie.clairvoyance.client.gui.evil_eyes.Evil_eyesScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.mojang.text2speech.Narrator.LOGGER;

public class Evil_EyesClient {
    private static UUID currentViewingEntity = null;
    private static ArmorStandEntity cameraEntity = null;
    private static long uiOpenTime = 0;
    private static final long UI_MAX_TICKS = 1200;
    private static boolean isViewing = false;
    private static boolean lastRightClickState = false;
    private static long lastRightClickTime = 0;
    private static final long RIGHT_CLICK_DELAY_TICKS = 10;
    private static final double CAM_DISTANCE = 3.0;
    private static final float CAM_YAW = 45.0f;
    private static final float CAM_PITCH = 30.0f;

    // 公开的标记实体映射（供其他类使用）
    public static final Map<UUID, Long> localMarkedEntities = new ConcurrentHashMap<>();

    // 供外部调用的方法（由网络接收器调用）
    public static void onSelectView(UUID entityUuid) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        currentViewingEntity = entityUuid;
        Entity target = client.world.getEntity(currentViewingEntity);
        if (target == null) return;

        cameraEntity = new ArmorStandEntity(EntityType.ARMOR_STAND, client.world);
        cameraEntity.setInvisible(true);
        cameraEntity.setInvulnerable(true);
        cameraEntity.setNoGravity(true);
        cameraEntity.setSilent(true);
        cameraEntity.setShowArms(false);
        cameraEntity.getDataTracker().set(
                ArmorStandEntity.ARMOR_STAND_FLAGS,
                (byte)(cameraEntity.getDataTracker().get(ArmorStandEntity.ARMOR_STAND_FLAGS) | ArmorStandEntity.MARKER_FLAG)
        );
        client.world.addEntity(cameraEntity);
        LOGGER.info("创建相机实体，ID = {}, 位置 = {}", cameraEntity.getUuid(), cameraEntity.getPos());

        updateCameraPosition(target);
        lookAtTarget(cameraEntity, target);
        client.cameraEntity = cameraEntity;

        if (client.player != null)
            client.player.sendMessage(Text.literal("正在注视 " + target.getName().getString()), true);

        if (client.currentScreen instanceof Evil_eyesScreen) client.setScreen(null);
        isViewing = true;
    }

    public static void exitViewMode(MinecraftClient client) {
        if (cameraEntity != null) {
            cameraEntity.discard();
            cameraEntity = null;
        }
        if (client.player != null) client.cameraEntity = client.player;
        currentViewingEntity = null;
        isViewing = false;
        uiOpenTime = 0;
        if (client.currentScreen instanceof Evil_eyesScreen) client.setScreen(null);
    }

    // 内部方法
    private static void updateCameraPosition(Entity target) {
        if (cameraEntity == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        Vec3d targetEyePos = target.getEyePos();
        Vec3d idealDir = new Vec3d(
                Math.sin(Math.toRadians(-CAM_YAW)) * Math.cos(Math.toRadians(CAM_PITCH)),
                Math.sin(Math.toRadians(CAM_PITCH)),
                Math.cos(Math.toRadians(-CAM_YAW)) * Math.cos(Math.toRadians(CAM_PITCH))
        ).normalize();
        Vec3d idealEnd = targetEyePos.add(idealDir.multiply(CAM_DISTANCE));

        RaycastContext context = new RaycastContext(
                targetEyePos, idealEnd,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                target
        );
        BlockHitResult hit = client.world.raycast(context);
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3d hitPos = hit.getPos();
            Vec3d direction = idealDir;
            Vec3d safePos = hitPos.subtract(direction.multiply(0.5));
            cameraEntity.setPosition(safePos);
        } else {
            cameraEntity.setPosition(idealEnd);
        }
    }

    private static void lookAtTarget(Entity entity, Entity target) {
        double dx = target.getX() - entity.getX();
        double dy = target.getEyeY() - entity.getEyeY();
        double dz = target.getZ() - entity.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0f);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontalDistance)));
        entity.setYaw(yaw);
        entity.setPitch(pitch);
        entity.setHeadYaw(yaw);
    }

    // 初始化（保留空，所有接收器已在 ClairvoyanceClient 中注册）
    public static void initialize() {
        // 所有接收器已移至 ClairvoyanceClient，此处不再注册
    }
}