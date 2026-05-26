package com.shushuwonie.clairvoyance;

import com.google.gson.JsonObject;
import com.shushuwonie.clairvoyance.command.ClairvoyanceCommand;
import com.shushuwonie.clairvoyance.command.GiveBodyPartCommand;
import com.shushuwonie.clairvoyance.command.ReplaceBodyPartCommand;
import com.shushuwonie.clairvoyance.command.WatchCommand;
import com.shushuwonie.clairvoyance.command.MirrorCommand;
import com.shushuwonie.clairvoyance.features.block.body.BodyPartManager;
import com.shushuwonie.clairvoyance.features.carryentity.CarryEvents;
import com.shushuwonie.clairvoyance.features.evil_eyes.Evil_Eyes;
import com.shushuwonie.clairvoyance.features.evil_eyes.ViewingModeBlocker;
import com.shushuwonie.clairvoyance.features.evil_eyes.server.CameraWatchManager;
import com.shushuwonie.clairvoyance.features.guidance.Gazeguidance;
import com.shushuwonie.clairvoyance.item.mirror.mirror_of_then_and_now;
import com.shushuwonie.clairvoyance.item.config.GazeConfig;
import com.shushuwonie.clairvoyance.item.gazeguidance.ModItems;
import com.shushuwonie.clairvoyance.item.modblock.ModBlocks;
import com.shushuwonie.clairvoyance.item.modblock.moditems.Assembly_ModItems;
import com.shushuwonie.clairvoyance.network.ModNetworking;
import com.shushuwonie.clairvoyance.network.camerawatch.*;
import com.shushuwonie.clairvoyance.network.clairvoyance.*;
import com.shushuwonie.clairvoyance.network.gazeguidance.*;
import com.shushuwonie.clairvoyance.network.mirror.MirrorStateS2CPacket;
import com.shushuwonie.clairvoyance.network.mirror.MirrorToggleC2SPacket;
import com.shushuwonie.clairvoyance.network.openback.CarryEntityPayload;
import com.shushuwonie.clairvoyance.network.openback.OpenOtherInventoryPayload;
import com.shushuwonie.clairvoyance.network.openback.PlaceCarriedEntityPayload;
import com.shushuwonie.clairvoyance.screen.ModScreenHandlers;
import com.shushuwonie.clairvoyance.screen.OtherPlayerInventoryScreenHandler;
import com.shushuwonie.clairvoyance.screen.OtherPlayerInventoryScreenHandlerFactory;

import com.shushuwonie.clairvoyance.config.GlobalConfigManager;
import com.shushuwonie.clairvoyance.entity.ModBlockEntities;
import net.fabricmc.api.ModInitializer;
import net.minecraft.server.command.CommandManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Clairvoyance implements ModInitializer {
	public static final String MOD_ID = "clairvoyance";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static ScreenHandlerType<OtherPlayerInventoryScreenHandler> OTHER_INVENTORY_HANDLER =
			new ScreenHandlerType<>(OtherPlayerInventoryScreenHandler::new, FeatureSet.empty());

	// Who is viewing whose inventory
	public static final Map<ServerPlayerEntity, ServerPlayerEntity> VIEWING_MAP = new ConcurrentHashMap<>();
	// Viewing mode preference: "legacy" = client armor stand camera, "modern" = server CameraWatch (default)
	public static final Map<UUID, String> VIEW_MODE_PREFERENCE = new ConcurrentHashMap<>();

	@Override
	public void onInitialize() {
		// ===== S2C Packets =====
		ModNetworking.registerS2CPackets();
		GlobalConfigS2CPacket.register();
		OpenUIPacket.register();
		EntityMarkedPayload.register();
		ToggleImagesS2CPacket.register();
		SelectViewPayload.register();
		ForceExitViewPayload.register();
		MarkParticleS2CPacket.register();
		SyncConfigS2CPacket.register();
		EnergySyncPacket.register();
		MarkCountPacket.register();
		FocusStatusPacket.register();
		ParticlePacket.register();
		StrengthPacket.register();
		AnchorParticleS2CPacket.register();
		PlayerStageS2CPacket.register();
		ExplosionParticleS2CPacket.register();
		CameraWatchBindS2CPacket.register();
		CameraWatchUnbindS2CPacket.register();
		CameraUpdateS2CPacket.register();
		MirrorStateS2CPacket.register();

		// ===== C2S Packets =====
		ModNetworking.registerC2SPackets();
		MarkEntityPayload.register();
		ExitViewPayload.register();
		MagicPacket.register();
		RightClickActionPacket.register();
		RequestConfigC2SPacket.register();
		UpdateConfigC2SPacket.register();
		RequestGlobalConfigC2SPacket.register();
		UpdateGlobalConfigC2SPacket.register();
		PlaceParrotC2SPacket.register();
		AnchorDestroyC2SPacket.register();
		OpenOtherInventoryPayload.register();
		CarryEntityPayload.register();
		PlaceCarriedEntityPayload.register();
		CameraWatchStartC2SPacket.register();
		CameraWatchStopC2SPacket.register();
		MirrorToggleC2SPacket.register();

		// Mirror toggle handler
		ServerPlayNetworking.registerGlobalReceiver(MirrorToggleC2SPacket.ID, (packet, context) -> {
			MirrorCommand.toggleViewport(context.player());
		});

		// ===== Commands =====
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			GiveBodyPartCommand.register(dispatcher, registryAccess, environment);
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			ReplaceBodyPartCommand.register(dispatcher, registryAccess, environment);
		});
		CommandRegistrationCallback.EVENT.register(WatchCommand::register);
		CommandRegistrationCallback.EVENT.register(MirrorCommand::register);
		CommandRegistrationCallback.EVENT.register(ClairvoyanceCommand::register);

		// Combined body merge command
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("clairvoyance-肢体|合并")
				.requires(source -> source.hasPermissionLevel(2))
				.executes(context -> {
					ServerPlayerEntity player = context.getSource().getPlayer();
					if (player == null) return 0;
					return BodyPartManager.mergeBodyParts(player);
				})
			);
		});

		// ===== Config =====
		GlobalConfigManager configManager = new GlobalConfigManager();

		ServerPlayNetworking.registerGlobalReceiver(RequestGlobalConfigC2SPacket.ID, (packet, context) -> {
			sendGlobalConfigToPlayer(context.player(), configManager);
		});

		ServerPlayNetworking.registerGlobalReceiver(RequestConfigC2SPacket.ID, (packet, context) -> {
			ServerPlayerEntity player = context.player();
			ServerPlayNetworking.send(player, new SyncConfigS2CPacket(GazeConfig.getInstance().toJson()));
		});

		// Update config (OP) -> broadcast to all online players
		ServerPlayNetworking.registerGlobalReceiver(UpdateGlobalConfigC2SPacket.ID, (packet, context) -> {
			if (context.player().hasPermissionLevel(2)) {
				configManager.updateStageConfig(packet.stage(), packet.dailyLimit(), packet.maxMarks(),
					packet.minScore(), packet.maxScore(), packet.watchRequiredTicks(), packet.parrotDailyLimit(), packet.maxActiveParrots()
				);
				for (ServerPlayerEntity player : context.player().getServer().getPlayerManager().getPlayerList()) {
					sendGlobalConfigToPlayer(player, configManager);
					player.sendMessage(Text.literal("§e[千里眼] 阶段 " + packet.stage() + " 配置已更新"), false);
				}
				context.player().sendMessage(Text.literal("§a阶段 " + packet.stage() + " 配置已更新并同步"), true);
			} else {
				context.player().sendMessage(Text.literal("§c我做不到"), true);
			}
		});

		// Push config on player join
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			sendGlobalConfigToPlayer(player, configManager);
			int stage = Evil_Eyes.getPlayerStage(player, configManager);
			ServerPlayNetworking.send(player, new PlayerStageS2CPacket(stage));
		});

		// ===== Camera Watch =====
		ServerTickEvents.END_SERVER_TICK.register(CameraWatchManager::tick);

		ServerPlayNetworking.registerGlobalReceiver(CameraWatchStartC2SPacket.ID, (packet, context) -> {
			ServerPlayerEntity player = context.player();
			CameraWatchManager.startWatching(player, packet.targetUuid(), player.getServer());
		});

		ServerPlayNetworking.registerGlobalReceiver(CameraWatchStopC2SPacket.ID, (packet, context) -> {
			ServerPlayerEntity player = context.player();
			MinecraftServer server = player.getServer();
			CameraWatchManager.stopWatching(player, server);
			Evil_Eyes.forceStopWatching(player, server);
		});

		// ===== Module Init =====
		Evil_Eyes.initialize(configManager);
		Gazeguidance.initialize();
		ModItems.initialize();
		ModBlocks.initialize();
		Assembly_ModItems.initialize();
		mirror_of_then_and_now.initialize();
		ModBlockEntities.initialize();
		ModScreenHandlers.initialize();

		// ===== Feature Event Registrations =====
		CarryEvents.register();
		ViewingModeBlocker.register();
		BodyPartManager.registerEvents();

		// ===== Open Other Inventory =====
		ServerPlayNetworking.registerGlobalReceiver(OpenOtherInventoryPayload.ID, (payload, context) -> {
			ServerPlayerEntity requester = context.player();
			if (!requester.isCreative()) {
				if (!requester.getCommandTags().contains("kaibao")) {
					requester.sendMessage(Text.literal("§c你没有§k12§r打开§k1234§r"), false);
					return;
				}
			}
			ServerWorld world = (ServerWorld) requester.getWorld();
			Entity target = world.getEntityById(payload.targetEntityId());
			if (!(target instanceof ServerPlayerEntity targetPlayer)) {
				requester.sendMessage(Text.literal("§c目标玩家不存在"), false);
				return;
			}
			if (requester.distanceTo(targetPlayer) > 3.0) {
				requester.sendMessage(Text.literal("§c太远了，也许我该离近点？"), false);
				return;
			}
			if (targetPlayer.isDead() || !targetPlayer.isAlive()) {
				requester.sendMessage(Text.literal("§c目标玩家已死亡"), false);
				return;
			}
			VIEWING_MAP.put(requester, targetPlayer);
			requester.openHandledScreen(new OtherPlayerInventoryScreenHandlerFactory(targetPlayer));
		});

		Registry.register(Registries.SCREEN_HANDLER, Identifier.of(MOD_ID, "other_inventory"), OTHER_INVENTORY_HANDLER);

		// ===== Anchor Destroy =====
		ServerPlayNetworking.registerGlobalReceiver(AnchorDestroyC2SPacket.ID, (packet, context) -> {
			ServerPlayerEntity player = context.player();
			UUID standId = packet.standId();
			World world = player.getWorld();
			Entity stand = world.getEntity(standId);
			if (stand instanceof ArmorStandEntity armorStand) {
				UUID ownerUuid = Evil_Eyes.armorStandOwner.get(standId);
				Evil_Eyes.sendExplosionToNearbyPlayers(stand, player.getServer());
				stand.remove(Entity.RemovalReason.DISCARDED);
				Evil_Eyes.armorStandOwner.remove(standId);
				Evil_Eyes.armorStandSpawnTick.remove(standId);
				if (ownerUuid != null) {
					Evil_Eyes.configManager.removeActiveParrot(ownerUuid);
				}
				player.sendMessage(Text.literal("§a锚点已破坏"), true);
			} else {
				player.sendMessage(Text.literal("§c锚点不存在"), true);
			}
		});

		// ===== Disconnect Cleanup =====
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			CameraWatchManager.stopWatching(player, server);
			Evil_Eyes.forceStopWatching(player, server);
			Evil_Eyes.clearPlayerMarks(player.getUuid());
			VIEWING_MAP.remove(player);
			VIEWING_MAP.values().removeIf(v -> v == player);
			VIEW_MODE_PREFERENCE.remove(player.getUuid());
		});
	}

	private static void sendGlobalConfigToPlayer(ServerPlayerEntity player, GlobalConfigManager configManager) {
		JsonObject root = new JsonObject();
		for (int i = 1; i <= 7; i++) {
			var cfg = configManager.getStageConfig(i);
			JsonObject stageObj = new JsonObject();
			stageObj.addProperty("dailyLimit", cfg.dailyLimit());
			stageObj.addProperty("maxMarks", cfg.maxMarks());
			stageObj.addProperty("minScore", cfg.minScore());
			stageObj.addProperty("maxScore", cfg.maxScore());
			stageObj.addProperty("watchRequiredTicks", cfg.watchRequiredTicks());
			stageObj.addProperty("parrotDailyLimit", cfg.parrotDailyLimit());
			stageObj.addProperty("maxActiveParrots", cfg.maxActiveParrots());
			root.add("stage" + i, stageObj);
		}
		String json = root.toString();
		ServerPlayNetworking.send(player, new GlobalConfigS2CPacket(json));
	}
}
