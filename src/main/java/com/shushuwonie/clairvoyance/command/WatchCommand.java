package com.shushuwonie.clairvoyance.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.shushuwonie.clairvoyance.features.evil_eyes.server.CameraWatchManager;
import com.shushuwonie.clairvoyance.util.RaycastHelper;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

import java.util.Collection;
import java.util.List;

public class WatchCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("watch")
                .then(CommandManager.argument("target", StringArgumentType.word())
                        .suggests((context, builder) -> CommandSource.suggestMatching(getAllEntityNames(context.getSource()), builder))
                        .executes(WatchCommand::executeWatchByName))
                .executes(WatchCommand::executeWatchLooking)
                .then(CommandManager.literal("angle")
                        .then(CommandManager.argument("yaw", FloatArgumentType.floatArg())
                                .then(CommandManager.argument("pitch", FloatArgumentType.floatArg())
                                        .then(CommandManager.argument("distance", DoubleArgumentType.doubleArg(0.5, 10.0))
                                                .executes(ctx -> {
                                                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                                                    float yaw = FloatArgumentType.getFloat(ctx, "yaw");
                                                    float pitch = FloatArgumentType.getFloat(ctx, "pitch");
                                                    double distance = DoubleArgumentType.getDouble(ctx, "distance");
                                                    CameraWatchManager.setOffset(player, yaw, pitch, distance);
                                                    player.sendMessage(Text.literal(String.format("§a相机偏移已设置: yaw=%.1f° pitch=%.1f° distance=%.1f", yaw, pitch, distance)), false);
                                                    // 如果当前正在观看，立即重新计算一次相机位置（可选）
                                                    if (CameraWatchManager.isWatching(player)) {
                                                        // 触发一次立即更新（下一 tick 自动更新）
                                                    }
                                                    return 1;
                                                }))
                                        .then(CommandManager.literal("reset")
                                                .executes(ctx -> {
                                                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                                                    CameraWatchManager.resetOffset(player);
                                                    player.sendMessage(Text.literal("§a相机偏移已重置为默认值"), false);
                                                    return 1;
                                                }))))
                ));



    }

    // 无参数：观看准星指向的实体
    private static int executeWatchLooking(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity viewer = ctx.getSource().getPlayer();
        if (viewer == null) return 0;

        // 如果正在观看，则停止
        if (CameraWatchManager.isWatching(viewer)) {
            CameraWatchManager.stopWatching(viewer, viewer.getServer());
            viewer.sendMessage(Text.literal("§a已停止观看"), false);
            return 1;
        }

        // 否则尝试观看准星指向的实体
        Entity target = RaycastHelper.getTargetEntity(viewer, 50.0);
        if (target == null) {
            viewer.sendMessage(Text.literal("§c未对准任何实体"), false);
            return 0;
        }
        CameraWatchManager.startWatching(viewer, target.getUuid(), viewer.getServer());
        return 1;
    }

    // 按名称观看（玩家名或实体自定义名）
    private static int executeWatchByName(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity viewer = ctx.getSource().getPlayer();
        if (viewer == null) return 0;

        String name = StringArgumentType.getString(ctx, "target");
        Entity target = findEntityByName(viewer, name);
        if (target == null) {
            viewer.sendMessage(Text.literal("§c未找到名为 " + name + " 的实体"), false);
            return 0;
        }

        // 如果已经在观看，先停止
        if (CameraWatchManager.isWatching(viewer)) {
            CameraWatchManager.stopWatching(viewer, viewer.getServer());
        }

        CameraWatchManager.startWatching(viewer, target.getUuid(), viewer.getServer());
        return 1;
    }

    // 服务端射线检测获取准星实体
    private static Entity getTargetEntity(ServerPlayerEntity player, double maxDistance) {
        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVec(1.0f);
        Vec3d end = start.add(direction.multiply(maxDistance));

        // 先检测实体
        EntityHitResult entityHit = raycastEntity(player, start, end, maxDistance);
        if (entityHit != null) return entityHit.getEntity();

        return null;
    }

    private static EntityHitResult raycastEntity(ServerPlayerEntity player, Vec3d start, Vec3d end, double maxDistance) {
        // 简单遍历附近实体（性能可接受，因为调用频率低）
        double closest = maxDistance;
        EntityHitResult closestHit = null;
        for (Entity entity : player.getWorld().iterateEntities()) {
            if (entity == player) continue;
            if (!entity.isAlive()) continue;
            // 粗略距离过滤
            double distToStart = entity.squaredDistanceTo(start);
            if (distToStart > maxDistance * maxDistance) continue;
            // 更精确的射线相交检测（使用实体的边界框）
            var hit = entity.getBoundingBox().raycast(start, end);
            if (hit.isPresent()) {
                double dist = start.distanceTo(hit.get());
                if (dist < closest) {
                    closest = dist;
                    closestHit = new EntityHitResult(entity, hit.get());
                }
            }
        }
        return closestHit;
    }

    private static Entity findEntityByName(ServerPlayerEntity player, String name) {
        for (Entity e : player.getWorld().iterateEntities()) {
            if (e.hasCustomName() && e.getCustomName().getString().equalsIgnoreCase(name)) {
                return e;
            }
            if (e instanceof PlayerEntity && e.getName().getString().equalsIgnoreCase(name)) {
                return e;
            }
        }
        return null;
    }

    private static Collection<String> getAllEntityNames(ServerCommandSource source) {
        // 简化实现，返回空集合，不影响使用（建议功能可后续完善）
        return List.of();
    }
}