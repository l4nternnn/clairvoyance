package com.shushuwonie.clairvoyance.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.shushuwonie.clairvoyance.entity.ModEntities;
import com.shushuwonie.clairvoyance.entity.TestMannequinEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public class ModCommands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("placemannequin")
                .executes(ModCommands::placeMannequin));
    }

    private static int placeMannequin(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        TestMannequinEntity mannequin = new TestMannequinEntity(ModEntities.TEST_MANNEQUIN, player.getWorld());
        mannequin.setPosition(player.getX(), player.getY(), player.getZ());
        player.getWorld().spawnEntity(mannequin);
        player.sendMessage(Text.literal("§a已放置测试模型实体"), false);
        return 1;
    }
}