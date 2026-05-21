package com.shushuwonie.clairvoyance;

import com.google.gson.JsonObject;
import com.shushuwonie.clairvoyance.command.ClairvoyanceCommand;
import com.shushuwonie.clairvoyance.command.GiveBodyPartCommand;
import com.shushuwonie.clairvoyance.command.WatchCommand;
import com.shushuwonie.clairvoyance.config.GlobalConfigManager;
import com.shushuwonie.clairvoyance.entity.ModBlockEntities;
import com.shushuwonie.clairvoyance.features.evil_eyes.Evil_Eyes;
import com.shushuwonie.clairvoyance.features.evil_eyes.server.CameraWatchManager;
import com.shushuwonie.clairvoyance.features.guidance.Gazeguidance;
import com.shushuwonie.clairvoyance.item.config.GazeConfig;
import com.shushuwonie.clairvoyance.item.gazeguidance.ModItems;
import com.shushuwonie.clairvoyance.item.modblock.ModBlocks;
import com.shushuwonie.clairvoyance.item.modblock.moditems.Assembly_ModItems;
import com.shushuwonie.clairvoyance.network.ModNetworking;
import com.shushuwonie.clairvoyance.network.camerawatch.*;
import com.shushuwonie.clairvoyance.network.clairvoyance.*;
import com.shushuwonie.clairvoyance.network.gazeguidance.*;
//import com.shushuwonie.client.network.camerawatch.*;
//import com.shushuwonie.client.network.clairvoyance.*;
//import com.shushuwonie.client.network.gazeguidance.*;
import com.shushuwonie.clairvoyance.network.openback.CarryEntityPayload;
import com.shushuwonie.clairvoyance.network.openback.OpenOtherInventoryPayload;
import com.shushuwonie.clairvoyance.network.openback.PlaceCarriedEntityPayload;
import com.shushuwonie.clairvoyance.screen.ModScreenHandlers;
import com.shushuwonie.clairvoyance.screen.OtherPlayerInventoryScreenHandler;
import com.shushuwonie.clairvoyance.screen.OtherPlayerInventoryScreenHandlerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Clairvoyance implements ModInitializer {
	public static final String MOD_ID = "clairvoyance";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static  ScreenHandlerType<OtherPlayerInventoryScreenHandler> OTHER_INVENTORY_HANDLER =
			new ScreenHandlerType<>(OtherPlayerInventoryScreenHandler::new, FeatureSet.empty());
	// 在类中定义静态 Map，记录谁在查看谁
	public static final Map<ServerPlayerEntity, ServerPlayerEntity> VIEWING_MAP = new ConcurrentHashMap<>();

	// 抱起者 -> 被抱实体
	public static final Map<ServerPlayerEntity, CarriedEntityData> CARRIED_ENTITIES = new ConcurrentHashMap<>();
	// 被抱实体 -> 抱起者（用于玩家主动挣脱时快速找到抱起者）
	public static final Map<Entity, ServerPlayerEntity> CARRIED_BY = new ConcurrentHashMap<>();
	// 辅助记录实体数据
	public static class CarriedEntityData {
		public final Entity entity;
		public CarriedEntityData(Entity entity) {
			this.entity = entity;
		}
	}


	/**
	 * 判断玩家是否处于任何观看模式（旧系统或新系统）
	 */
	public static boolean isViewing(ServerPlayerEntity player) {
		// 旧系统：Evil_Eyes.watchingPlayers 包含该玩家
		if (Evil_Eyes.isWatching(player)) return true;
		// 新系统：CameraWatchManager 中有会话
		if (CameraWatchManager.isWatching(player)) return true;
		return false;
	}
	private static boolean hasTag(ServerPlayerEntity player, String tag) {
		return player.getCommandTags().contains(tag);
	}
	private static final Set<UUID> FALL_DAMAGE_PROCESSING = ConcurrentHashMap.newKeySet();


	@Override
	public void onInitialize() {


		// 注册所有 S2C 包
		ModNetworking.registerS2CPackets();   // 注册 S2C 包
		GlobalConfigS2CPacket.register();
		OpenUIPacket.register();
		EntityMarkedPayload.register();
		ToggleImagesS2CPacket.register();
		SelectViewPayload.register();      // S2C
		ForceExitViewPayload.register();   // S2C
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

		// 注册所有 C2S 包
		ModNetworking.registerC2SPackets();   // 注册 C2S 包
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


		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			GiveBodyPartCommand.register(dispatcher);
		});

		GlobalConfigManager configManager = new GlobalConfigManager();

		// 处理客户端请求配置
		ServerPlayNetworking.registerGlobalReceiver(RequestGlobalConfigC2SPacket.ID, (packet, context) -> {
			sendGlobalConfigToPlayer(context.player(), configManager);
		});

		ServerPlayNetworking.registerGlobalReceiver(RequestConfigC2SPacket.ID, (packet, context) -> {
			ServerPlayerEntity player = context.player();
			ServerPlayNetworking.send(player, new SyncConfigS2CPacket(GazeConfig.getInstance().toJson()));
		});


		// 注册新指令
		CommandRegistrationCallback.EVENT.register(WatchCommand::register);

		// 启动 tick 事件更新相机
		ServerTickEvents.END_SERVER_TICK.register(CameraWatchManager::tick);

// 处理开始观看请求
		ServerPlayNetworking.registerGlobalReceiver(CameraWatchStartC2SPacket.ID, (packet, context) -> {
			ServerPlayerEntity player = context.player();
			MinecraftServer server = player.getServer();
			CameraWatchManager.startWatching(player, packet.targetUuid(), server);
		});

// 处理停止观看请求（可选，如果客户端主动发送）
		ServerPlayNetworking.registerGlobalReceiver(CameraWatchStopC2SPacket.ID, (packet, context) -> {
			ServerPlayerEntity player = context.player();
			MinecraftServer server = player.getServer();
			CameraWatchManager.stopWatching(player, server);
			Evil_Eyes.forceStopWatching(player, server);
		});


		// 更新配置（OP 权限）-> 向所有在线玩家广播
		ServerPlayNetworking.registerGlobalReceiver(UpdateGlobalConfigC2SPacket.ID, (packet, context) -> {
			if (context.player().hasPermissionLevel(2)) {
				configManager.updateStageConfig(packet.stage(), packet.dailyLimit(), packet.maxMarks(),
						packet.minScore(), packet.maxScore(), packet.watchRequiredTicks(), packet.parrotDailyLimit(), packet.maxActiveParrots()
				);
				// 广播给所有玩家（不再检查物品）
				for (ServerPlayerEntity player : context.player().getServer().getPlayerManager().getPlayerList()) {
					sendGlobalConfigToPlayer(player, configManager);
					player.sendMessage(Text.literal("§e[千里眼] 阶段 " + packet.stage() + " 配置已更新"), false);
				}
				context.player().sendMessage(Text.literal("§a阶段 " + packet.stage() + " 配置已更新并同步"), true);
			} else {
				context.player().sendMessage(Text.literal("§c我做不到"), true);
			}
		});

		// 玩家登录时直接推送配置（不检查物品）
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			sendGlobalConfigToPlayer(player, configManager);
			// 登录时立即发送阶段
			int stage = Evil_Eyes.getPlayerStage(player, configManager);
			ServerPlayNetworking.send(player, new PlayerStageS2CPacket(stage));
		});

		// 初始化功能模块
		Evil_Eyes.initialize(configManager);
		Gazeguidance.initialize();
		ModItems.initialize();
		ModBlocks.initialize();   // 确保所有 static 字段被初始化
		Assembly_ModItems.initialize();
		ModBlockEntities.initialize();
		ModScreenHandlers.initialize();


//		LOGGER.info("Clairvoyance Mod 初始化完成");

		// 在 onInitialize 方法末尾添加
		CommandRegistrationCallback.EVENT.register(ClairvoyanceCommand::register);


		ServerPlayNetworking.registerGlobalReceiver(AnchorDestroyC2SPacket.ID, (packet, context) -> {
			ServerPlayerEntity player = context.player();
			UUID standId = packet.standId();
			World world = player.getWorld();
			Entity stand = world.getEntity(standId);
			if (stand instanceof ArmorStandEntity armorStand) {
				UUID ownerUuid = Evil_Eyes.armorStandOwner.get(standId);
				Evil_Eyes.sendExplosionToNearbyPlayers(stand, player.getServer());
				// 直接移除，不调用 kill
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

		ServerPlayNetworking.registerGlobalReceiver(OpenOtherInventoryPayload.ID, (payload, context) -> {
			ServerPlayerEntity requester = context.player();
			if (!requester.isCreative()) {
				// 检查是否拥有 "kaibao" 标签
				if (!requester.getCommandTags().contains("kaibao")) {
					requester.sendMessage(Text.literal("§c你没有§k12§r打开§k1234§r"), false);
					return;
				}
//				requester.sendMessage(Text.literal("§c只有§k1234§r才能打开别人的背包"), false);
//				return;
			}
			ServerWorld world = (ServerWorld) requester.getWorld();
			Entity target = world.getEntityById(payload.targetEntityId());
			if (!(target instanceof ServerPlayerEntity targetPlayer)) {
				requester.sendMessage(Text.literal("§c目标玩家不存在"), false);
				return;
			}
			// 距离检查（3格内）
			if (requester.distanceTo(targetPlayer) > 3.0) {
				requester.sendMessage(Text.literal("§c太远了，也许我该离近点？"), false);
				return;
			}
			// 死亡检查
			if (targetPlayer.isDead() || !targetPlayer.isAlive()) {
				requester.sendMessage(Text.literal("§c目标玩家已死亡"), false);
				return;
			}
			// 记录到 Map
			VIEWING_MAP.put(requester, targetPlayer);
			requester.openHandledScreen(new OtherPlayerInventoryScreenHandlerFactory(targetPlayer));
		});

		// 注册 ScreenHandlerType
		Registry.register(Registries.SCREEN_HANDLER, Identifier.of(MOD_ID, "other_inventory"), OTHER_INVENTORY_HANDLER);

		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
			if (source.isOf(DamageTypes.FALL) && entity instanceof ServerPlayerEntity carrier) {
				CarriedEntityData data = CARRIED_ENTITIES.get(carrier);
				if (data != null && !FALL_DAMAGE_PROCESSING.contains(carrier.getUuid())) {
					FALL_DAMAGE_PROCESSING.add(carrier.getUuid());
					try {
						Entity carried = data.entity;
						float carrierExtra = damageTaken * 0.2F;   // 额外 0.2 倍伤害（总共 1.2 倍）
						float carriedDamage = damageTaken * 0.6F;  // 0.6 倍伤害

						// 直接修改血量，避免再次触发事件
						float newCarrierHealth = carrier.getHealth() - carrierExtra;
						if (newCarrierHealth < 0) newCarrierHealth = 0;
						carrier.setHealth(newCarrierHealth);

						if (carried.isAlive()) {
							float newCarriedHealth = carried instanceof LivingEntity ? ((LivingEntity) carried).getHealth() - carriedDamage : 0;
							if (newCarriedHealth < 0) newCarriedHealth = 0;
							if (carried instanceof LivingEntity) ((LivingEntity) carried).setHealth(newCarriedHealth);
							else if (carried instanceof ServerPlayerEntity) ((ServerPlayerEntity) carried).setHealth(newCarriedHealth);
						}
					} finally {
						FALL_DAMAGE_PROCESSING.remove(carrier.getUuid());
					}
				}
			}
		});

		// 注册断开连接事件，清理 Map
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (Map.Entry<ServerPlayerEntity, CarriedEntityData> entry : CARRIED_ENTITIES.entrySet()) {
				ServerPlayerEntity carrier = entry.getKey();
				Entity carried = entry.getValue().entity;
				if (!carried.isAlive()) {
					CARRIED_ENTITIES.remove(carrier);
					CARRIED_BY.remove(carried);
					continue;
				}

				Vec3d lookVec = carrier.getRotationVec(1.0f);
				Vec3d targetPos = carrier.getEyePos().add(lookVec.multiply(1.5));
				// 使用公共方法 setPosition 代替 refreshPosition
				carried.setPosition(targetPos.x, targetPos.y, targetPos.z);
				carried.setVelocity(Vec3d.ZERO);
				carried.setNoGravity(true);
//				carried.setOnGround(true);
//				carried.fallDistance = 0.0F;
				if (carried instanceof MobEntity mob) {
					mob.setAiDisabled(true);
					mob.getNavigation().stop();
				}

				if (carried instanceof ServerPlayerEntity carriedPlayer) {
					if (carriedPlayer.isSneaking()) {
						releaseCarried(carrier, carriedPlayer);
						carrier.sendMessage(Text.literal("§c" + carriedPlayer.getName().getString() + " 挣脱了"), false);
						carriedPlayer.sendMessage(Text.literal("§c你挣脱了怀抱"), false);
					}
				}
			}
		});


		//		// 处理搬运实体请求
		ServerPlayNetworking.registerGlobalReceiver(CarryEntityPayload.ID, (payload, context) -> {
			ServerPlayerEntity carrier = context.player();
			if (!carrier.isSneaking()) return;
			if (!carrier.getMainHandStack().isEmpty() || !carrier.getOffHandStack().isEmpty()) return;
			if (!carrier.isCreative() && !carrier.getCommandTags().contains("kebao")) {
				carrier.sendMessage(Text.literal("§c你无法抱起§k12§r"), false);
				return;
			}
			ServerWorld world = (ServerWorld) carrier.getWorld();
			Entity target = world.getEntityById(payload.entityId());
			if (target == null) return;

			CarriedEntityData currentData = CARRIED_ENTITIES.get(carrier);
			if (currentData != null && currentData.entity == target) {
				releaseCarried(carrier, target);
				carrier.sendMessage(Text.literal("§a你放下了" + target.getName().getString()), false);
				return;
			}

			if (target instanceof ServerPlayerEntity carriedPlayer) {
				// 保存被抱玩家的原始能力（可选，恢复时使用）
				carriedPlayer.setNoGravity(true);
				carriedPlayer.getAbilities().flying = true;
				carriedPlayer.getAbilities().allowFlying = true;
				carriedPlayer.getAbilities().invulnerable = true; // 可选，防止被抱玩家受伤
				carriedPlayer.sendAbilitiesUpdate();
				// 同步位置
				carriedPlayer.networkHandler.requestTeleport(carrier.getX(), carrier.getY(), carrier.getZ(), carrier.getYaw(), carrier.getPitch());
			}


			if (CARRIED_ENTITIES.containsKey(carrier)) {
				carrier.sendMessage(Text.literal("§c你已经抱起了一个，先放下她"), false);
				return;
			}
			if (CARRIED_BY.containsKey(target)) {
				carrier.sendMessage(Text.literal("§c§k1234§r已经被别人抱起了"), false);
				return;
			}

			if (target instanceof ServerPlayerEntity carriedPlayer) {
				if (CARRIED_ENTITIES.containsKey(carriedPlayer)) {
					carrier.sendMessage(Text.literal("§c§k1§r正在抱起其他实体，无法抱起"), false);
					return;
				}
				carriedPlayer.sendMessage(Text.literal("§e你被 " + carrier.getName().getString() + " 抱起来了，按潜行键挣脱"), false);
			} else if (!(target instanceof LivingEntity)) {
				carrier.sendMessage(Text.literal("§c只能抱起活物"), false);
				return;
			}

			if (target instanceof MobEntity mob) {
				mob.setAiDisabled(true);
				mob.getNavigation().stop();
			}
			target.stopRiding();
			target.setNoGravity(true);
//			target.setInvulnerable(true);
			target.setSilent(true);

			CARRIED_ENTITIES.put(carrier, new CarriedEntityData(target));
			CARRIED_BY.put(target, carrier);
			carrier.sendMessage(Text.literal("§a你抱起了 " + target.getName().getString()), false);
		});

		//// 处理放置实体请求
		ServerPlayNetworking.registerGlobalReceiver(PlaceCarriedEntityPayload.ID, (payload, context) -> {
			ServerPlayerEntity carrier = context.player();
			CarriedEntityData data = CARRIED_ENTITIES.remove(carrier);
			if (data == null) return;
			Entity carried = data.entity;
			CARRIED_BY.remove(carried);
			if (carried.isAlive()) {
				carried.setNoGravity(false);
//				carried.setInvulnerable(false);
				carried.setSilent(false);
				if (carried instanceof MobEntity mob) {
					mob.setAiDisabled(false);
				}
				Vec3d lookVec = carrier.getRotationVec(1.0f);
				Vec3d pos = carrier.getEyePos().add(lookVec.multiply(1.5)).subtract(0, 0.5, 0);
				carried.refreshPositionAndAngles(pos.x, pos.y, pos.z, carrier.getYaw(), carrier.getPitch());
				carrier.sendMessage(Text.literal("§a你放下了抱起的实体"), false);
			} else {
				carrier.sendMessage(Text.literal("§c实体已经死亡，无法放下"), false);
			}
		});


		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (Map.Entry<ServerPlayerEntity, CarriedEntityData> entry : CARRIED_ENTITIES.entrySet()) {
				ServerPlayerEntity carrier = entry.getKey();
				Entity carried = entry.getValue().entity;
				if (!carried.isAlive()) {
					CARRIED_ENTITIES.remove(carrier);
					CARRIED_BY.remove(carried);
					continue;
				}

				// 获取玩家的身体朝向水平方向（忽略俯仰）
				float yaw = carrier.getYaw();
				double rad = Math.toRadians(yaw);
				double forwardX = -Math.sin(rad);
				double forwardZ = Math.cos(rad);
				Vec3d horizontalForward = new Vec3d(forwardX, 0, forwardZ).normalize();
				double distance = 1.2;      // 身体前方距离
				double yOffset = 0.5;       // 相对于玩家脚部的Y偏移（胸口高度）
				Vec3d targetPos = carrier.getPos().add(horizontalForward.multiply(distance)).add(0, yOffset, 0);

				// 只更新位置，不更新朝向（保持实体自己的朝向）
				carried.setPosition(targetPos.x, targetPos.y, targetPos.z);
				carried.setVelocity(Vec3d.ZERO);
				carried.setNoGravity(true);
				// 不要设置 setOnGround 和 fallDistance，让摔落逻辑正常工作
				if (carried instanceof MobEntity mob) {
					mob.setAiDisabled(true);
					mob.getNavigation().stop();
				}

				if (carried instanceof ServerPlayerEntity carriedPlayer) {
					if (carriedPlayer.isSneaking()) {
						releaseCarried(carrier, carriedPlayer);
						carrier.sendMessage(Text.literal("§c" + carriedPlayer.getName().getString() + " 挣脱了"), false);
						carriedPlayer.sendMessage(Text.literal("§c你按潜行挣脱了怀抱"), false);
					} else {
						// 强制同步被抱玩家的服务端位置到客户端，但不改变其朝向
						carriedPlayer.networkHandler.requestTeleport(
								targetPos.x, targetPos.y, targetPos.z,
								carriedPlayer.getYaw(), carriedPlayer.getPitch()
						);
					}
				}
			}
		});

		// 观看模式下禁止交互（服务端拦截）
		// 阻止攻击实体
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (player instanceof ServerPlayerEntity serverPlayer && isViewing(serverPlayer)) {
				serverPlayer.sendMessage(Text.literal("§c观看模式下无法攻击"), true);
				return ActionResult.FAIL;
			}
			return ActionResult.PASS;
		});

// 阻止破坏方块（左键）
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			if (player instanceof ServerPlayerEntity serverPlayer && isViewing(serverPlayer)) {
				serverPlayer.sendMessage(Text.literal("§c观看模式下无法破坏方块"), true);
				return ActionResult.FAIL;
			}
			return ActionResult.PASS;
		});

// 阻止使用方块（右键）
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (player instanceof ServerPlayerEntity serverPlayer && isViewing(serverPlayer)) {
				serverPlayer.sendMessage(Text.literal("§c观看模式下无法使用方块"), true);
				return ActionResult.FAIL;
			}
			return ActionResult.PASS;
		});

// 阻止与实体交互（右键实体）
		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (player instanceof ServerPlayerEntity serverPlayer && isViewing(serverPlayer)) {
				serverPlayer.sendMessage(Text.literal("§c观看模式下无法与实体交互"), true);
				return ActionResult.FAIL;
			}
			return ActionResult.PASS;
		});

// 阻止使用物品（右键空气）
		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (player instanceof ServerPlayerEntity serverPlayer && isViewing(serverPlayer)) {
				serverPlayer.sendMessage(Text.literal("§c观看模式下无法使用物品"), true);
				return ActionResult.FAIL;
			}
			return ActionResult.PASS;
		});

	}




	private static void releaseCarried(ServerPlayerEntity carrier, Entity carried) {
		CARRIED_ENTITIES.remove(carrier);
		CARRIED_BY.remove(carried);
		carried.setNoGravity(false);
		carried.setInvulnerable(false);
		carried.setSilent(false);
		if (carried instanceof MobEntity mob) {
			mob.setAiDisabled(false);
		}
		if (carried instanceof ServerPlayerEntity carriedPlayer) {
			carriedPlayer.getAbilities().flying = false;
			carriedPlayer.getAbilities().allowFlying = false;
			carriedPlayer.getAbilities().invulnerable = false;
			carriedPlayer.sendAbilitiesUpdate();
		}
		// 放下位置（使用原本的计算，但需要强制同步）
		Vec3d lookVec = carrier.getRotationVec(1.0f);
		Vec3d pos = carrier.getEyePos().add(lookVec.multiply(0.5));
		carried.refreshPositionAndAngles(pos.x, pos.y, pos.z, carrier.getYaw(), carrier.getPitch());

		if (carried instanceof ServerPlayerEntity carriedPlayer) {
			carriedPlayer.networkHandler.requestTeleport(pos.x, pos.y, pos.z, carriedPlayer.getYaw(), carriedPlayer.getPitch());
		}
		if (carried instanceof ServerPlayerEntity carriedPlayer) {
			carriedPlayer.networkHandler.requestTeleport(pos.x, pos.y, pos.z, carrier.getYaw(), carrier.getPitch());
		}
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
			// 注意：鹦鹉参数已经移除，所以不包含在 JSON 中
			root.add("stage" + i, stageObj);
		}
		String json = root.toString();
		ServerPlayNetworking.send(player, new GlobalConfigS2CPacket(json));
	}
}
