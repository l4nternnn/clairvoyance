package com.shushuwonie.clairvoyance.util;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class RaycastHelper {

    /**
     * 获取玩家准星指向的实体（服务端版本）
     * @param player 玩家
     * @param maxDistance 最大距离
     * @return 命中的实体，没有则 null
     */
    @Nullable
    public static Entity getTargetEntity(ServerPlayerEntity player, double maxDistance) {
        ServerWorld world = player.getWorld();
        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVec(1.0f);
        Vec3d end = start.add(direction.multiply(maxDistance));

        Entity closestEntity = null;
        double closestDistance = maxDistance;

        // 使用 iterateEntities() 遍历所有实体
        for (Entity entity : world.iterateEntities()) {
            if (entity == player) continue;
            if (!entity.isAlive()) continue;

            double distanceToPlayerSq = entity.squaredDistanceTo(start);
            if (distanceToPlayerSq > maxDistance * maxDistance) continue;

            Box boundingBox = entity.getBoundingBox();
            var hit = boundingBox.raycast(start, end);
            if (hit.isPresent()) {
                double distance = start.distanceTo(hit.get());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestEntity = entity;
                }
            }
        }
        return closestEntity;
    }

    /**
     * 返回 EntityHitResult（包含命中点）
     */
    @Nullable
    public static EntityHitResult raycastEntity(ServerPlayerEntity player, double maxDistance) {
        ServerWorld world = player.getWorld();
        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVec(1.0f);
        Vec3d end = start.add(direction.multiply(maxDistance));

        Entity closestEntity = null;
        Vec3d closestHitPoint = null;
        double closestDistance = maxDistance;

        for (Entity entity : world.iterateEntities()) {
            if (entity == player) continue;
            if (!entity.isAlive()) continue;

            double distanceToPlayerSq = entity.squaredDistanceTo(start);
            if (distanceToPlayerSq > maxDistance * maxDistance) continue;

            Box boundingBox = entity.getBoundingBox();
            var hit = boundingBox.raycast(start, end);
            if (hit.isPresent()) {
                double distance = start.distanceTo(hit.get());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestEntity = entity;
                    closestHitPoint = hit.get();
                }
            }
        }

        if (closestEntity != null && closestHitPoint != null) {
            return new EntityHitResult(closestEntity, closestHitPoint);
        }
        return null;
    }
}