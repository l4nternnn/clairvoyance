package com.shushuwonie.clairvoyance.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.shushuwonie.clairvoyance.Clairvoyance;
import com.shushuwonie.clairvoyance.features.evil_eyes.Evil_Eyes;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.Map;
import java.util.UUID;

public class ClairvoyanceCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("clairvoyance")
                .then(CommandManager.literal("clearanchors_清除千里眼锚点")
                        // 清除自己的锚点
                        .executes(ctx -> clearOwnAnchors(ctx))
                        // 清除指定玩家的锚点（需要 OP）
                        .then(CommandManager.argument("player", StringArgumentType.word())
                                .requires(source -> source.hasPermissionLevel(2))
                                .executes(ctx -> clearOtherAnchors(ctx, StringArgumentType.getString(ctx, "player")))
                        )
                        // 清除所有锚点（需要 OP）
                        .then(CommandManager.literal("all")
                                .requires(source -> source.hasPermissionLevel(2))
                                .executes(ctx -> clearAllAnchors(ctx.getSource()))
                        )
                )
                .then(CommandManager.literal("viewmode_切换相机系统")
                        .then(CommandManager.literal("legacy——旧版客户端")
                                .executes(ctx -> setViewMode(ctx, "legacy"))
                        )
                        .then(CommandManager.literal("modern——新版服务端")
                                .executes(ctx -> setViewMode(ctx, "modern"))
                        )
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            String current = Clairvoyance.VIEW_MODE_PREFERENCE.getOrDefault(player.getUuid(), "modern");
                            String modeName = "legacy".equals(current) ? "§e旧版(客户端盔甲架)" : "§a新版(服务端CameraWatch)";
                            player.sendMessage(Text.literal("§6当前观看模式: " + modeName), false);
                            player.sendMessage(Text.literal("§7使用 /clairvoyance viewmode <legacy|modern> 切换"), false);
                            return 1;
                        })
                )
        );
    }

    private static int setViewMode(CommandContext<ServerCommandSource> ctx, String mode) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        Clairvoyance.VIEW_MODE_PREFERENCE.put(player.getUuid(), mode);

        // 如果在观看中，停止当前观看
        if (com.shushuwonie.clairvoyance.features.evil_eyes.server.CameraWatchManager.isWatching(player)) {
            com.shushuwonie.clairvoyance.features.evil_eyes.server.CameraWatchManager.stopWatching(player, player.getServer());
        }
        if (Evil_Eyes.isWatching(player)) {
            Evil_Eyes.forceStopWatching(player, player.getServer());
        }

        String displayName = "legacy".equals(mode) ? "§e旧版(客户端盔甲架)" : "§a新版(服务端CameraWatch)";
        player.sendMessage(Text.literal("§a已切换到观看模式: " + displayName), false);
        return 1;
    }

    private static int clearOwnAnchors(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        MinecraftServer server = source.getServer();
        int count = Evil_Eyes.clearAnchorsForPlayer(player.getUuid(), server);
        source.sendFeedback(() -> Text.literal("已清除 " + count + " 个锚点").formatted(Formatting.GREEN), false);
        return count;
    }

    private static int clearOtherAnchors(CommandContext<ServerCommandSource> ctx, String playerName) {
        ServerCommandSource source = ctx.getSource();
        MinecraftServer server = source.getServer();
        ServerPlayerEntity target = server.getPlayerManager().getPlayer(playerName);
        if (target == null) {
            source.sendError(Text.literal("玩家 " + playerName + " 不在线"));
            return 0;
        }
        int count = Evil_Eyes.clearAnchorsForPlayer(target.getUuid(), server);
        source.sendFeedback(() -> Text.literal("已清除 " + playerName + " 的 " + count + " 个锚点").formatted(Formatting.GREEN), false);
        return count;
    }

    // 清除所有锚点（通过 armorStandOwner 映射遍历）
    private static int clearAllAnchors(ServerCommandSource source) {
        MinecraftServer server = source.getServer();
        World world = server.getWorld(World.OVERWORLD);
        if (world == null) return 0;
        int count = 0;
        // 遍历盔甲架映射的副本，避免并发修改
        for (Map.Entry<UUID, UUID> entry : Evil_Eyes.armorStandOwner.entrySet().stream().toList()) {
            UUID standId = entry.getKey();
            Entity stand = world.getEntity(standId);
            if (stand instanceof ArmorStandEntity armorStand) {
                // 移除映射
                Evil_Eyes.armorStandOwner.remove(standId);
                Evil_Eyes.armorStandSpawnTick.remove(standId);
                UUID ownerId = entry.getValue();
                if (ownerId != null) {
                    Evil_Eyes.configManager.removeActiveParrot(ownerId);
                }
                // 产生爆炸效果
                Evil_Eyes.sendExplosionToNearbyPlayers(armorStand, server);
                armorStand.remove(Entity.RemovalReason.DISCARDED);
                count++;
            }
        }
        int finalCount = count;
        source.sendFeedback(() -> Text.literal("已清除所有 " + finalCount + " 个锚点").formatted(Formatting.GREEN), false);
        return count;
    }
}
