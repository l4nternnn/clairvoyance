package com.shushuwonie.clairvoyance.features.evil_eyes;

import com.shushuwonie.clairvoyance.features.evil_eyes.server.CameraWatchManager;
import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

public class ViewingModeBlocker {
	public static void register() {
		// Block entity attacks
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (player instanceof ServerPlayerEntity serverPlayer && isViewing(serverPlayer)) {
				serverPlayer.sendMessage(Text.literal("§c观看模式下无法攻击"), true);
				return ActionResult.FAIL;
			}
			return ActionResult.PASS;
		});

		// Block block breaking (left-click)
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			if (player instanceof ServerPlayerEntity serverPlayer && isViewing(serverPlayer)) {
				serverPlayer.sendMessage(Text.literal("§c观看模式下无法破坏方块"), true);
				return ActionResult.FAIL;
			}
			return ActionResult.PASS;
		});

		// Block block use (right-click)
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (player instanceof ServerPlayerEntity serverPlayer && isViewing(serverPlayer)) {
				serverPlayer.sendMessage(Text.literal("§c观看模式下无法使用方块"), true);
				return ActionResult.FAIL;
			}
			return ActionResult.PASS;
		});

		// Block entity interaction (right-click entity)
		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (player instanceof ServerPlayerEntity serverPlayer && isViewing(serverPlayer)) {
				serverPlayer.sendMessage(Text.literal("§c观看模式下无法与实体交互"), true);
				return ActionResult.FAIL;
			}
			return ActionResult.PASS;
		});

		// Block item use (right-click air)
		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (player instanceof ServerPlayerEntity serverPlayer && isViewing(serverPlayer)) {
				serverPlayer.sendMessage(Text.literal("§c观看模式下无法使用物品"), true);
				return ActionResult.FAIL;
			}
			return ActionResult.PASS;
		});
	}

	public static boolean isViewing(ServerPlayerEntity player) {
		if (Evil_Eyes.isWatching(player)) return true;
		if (CameraWatchManager.isWatching(player)) return true;
		return false;
	}
}
