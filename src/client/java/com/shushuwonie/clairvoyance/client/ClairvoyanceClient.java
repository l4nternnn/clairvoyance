package com.shushuwonie.clairvoyance.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.shushuwonie.clairvoyance.Clairvoyance;
import com.shushuwonie.clairvoyance.client.evil_eyes.watch.CameraWatchClientHandler;
import com.shushuwonie.clairvoyance.client.evil_eyes.watch.ClientCameraWatchReceiver;
import com.shushuwonie.clairvoyance.client.gui.openback.OtherPlayerInventoryScreen;
import com.shushuwonie.clairvoyance.client.model.ModModelLayers;
import com.shushuwonie.clairvoyance.client.model.arm.LeftArmModel;
import com.shushuwonie.clairvoyance.client.model.arm.RightArmModel;
import com.shushuwonie.clairvoyance.client.model.leg.LeftLegModel;
import com.shushuwonie.clairvoyance.client.model.leg.RightLegModel;
import com.shushuwonie.clairvoyance.client.model.torso.TorsoModel;
import com.shushuwonie.clairvoyance.client.renderer.arm.LeftArmBlockEntityRenderer;
import com.shushuwonie.clairvoyance.client.renderer.arm.RightArmBlockEntityRenderer;
import com.shushuwonie.clairvoyance.client.renderer.leg.LeftLegBlockEntityRenderer;
import com.shushuwonie.clairvoyance.client.renderer.leg.RightLegBlockEntityRenderer;
import com.shushuwonie.clairvoyance.client.renderer.torso.TorsoBlockEntityRenderer;
import com.shushuwonie.clairvoyance.entity.ModBlockEntities;
import com.shushuwonie.clairvoyance.network.ModNetworking;
import com.shushuwonie.clairvoyance.network.clairvoyance.*;
import com.shushuwonie.clairvoyance.network.gazeguidance.*;
//import com.shushuwonie.client.evil_eyes.Evil_EyesClient;
//import com.shushuwonie.client.evil_eyes.client.CameraWatchClientHandler;
//import com.shushuwonie.client.gazeguidance.GazeguidanceClient;
import com.shushuwonie.clairvoyance.client.gui.CombinedConfigScreen;
//import com.shushuwonie.client.gui.evil_eyes.Evil_eyesScreen;
import com.shushuwonie.clairvoyance.features.evil_eyes.Evil_Eyes;
import com.shushuwonie.clairvoyance.item.config.GazeConfig;
import com.shushuwonie.clairvoyance.item.gazeguidance.ModItems;
import com.shushuwonie.clairvoyance.network.openback.CarryEntityPayload;
import com.shushuwonie.clairvoyance.network.openback.OpenOtherInventoryPayload;
import com.shushuwonie.clairvoyance.network.openback.PlaceCarriedEntityPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.shushuwonie.client.gazeguidance.GazeguidanceClient.getTargetEntity;

public class ClairvoyanceClient implements ClientModInitializer {
	private static KeyBinding configKey;
	private static KeyBinding markKey;
	private static boolean hasRequestedConfig = false;
	private static int currentPlayerStage = 1;
	private static volatile int lastNotifiedStage = 1;

	private static final Logger LOGGER = LoggerFactory.getLogger(ClairvoyanceClient.class);

	// 背部图片相关
	private record BackImage(Identifier texture, float xOffset, float yOffset, float zOffset, float width, float height, float selfRotateZ) {}
	private static final List<BackImage> BACK_IMAGES = new ArrayList<>();
	private static final Set<String> SPECIAL_NAMES = Set.of("shushuwonie", "Remio");

	// 锚点信息（客户端本地维护）
	private static class AnchorInfo {
		Vec3d pos;
		long lastSeenTime;
	}
	private static final Map<UUID, AnchorInfo> anchors = new ConcurrentHashMap<>();
	private static boolean imagesEnabled = false;
	private static boolean lastMainHandEmpty = true;

	@Override
	public void onInitializeClient() {




		ModNetworking.registerS2CPackets();

		// 初始化其他模块
		com.shushuwonie.client.evil_eyes.Evil_EyesClient.initialize();
		com.shushuwonie.client.gazeguidance.GazeguidanceClient.initialize(); // 此方法内部不应再注册接收器，只保留业务初始化
		// 按键绑定
		configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.clairvoyance.config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_U, "category.clairvoyance"));
		markKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.clairvoyance.mark", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.clairvoyance"));

		// 按键处理
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) return;
			if (configKey.wasPressed() && client.player.isCreative()) {
				client.setScreen(new CombinedConfigScreen());
			}
			if (markKey.wasPressed()) {
				ItemStack mainHand = client.player.getMainHandStack();
				if (client.player.isSneaking() && mainHand.isEmpty() && client.player.getOffHandStack().isEmpty()) {
					Entity target = getTargetEntity(client, 5.0);
					if (target instanceof LivingEntity ) {
						ClientPlayNetworking.send(new CarryEntityPayload(target.getId()));
						client.player.sendMessage(Text.literal("§e尝试抱起实体"), true);
					} else {
						client.player.sendMessage(Text.literal("§c请对准一个可抱起的生物"), true);
					}
					return;
				}
				if (mainHand.getItem() == Evil_Eyes.CLAIRVOYANCE_ITEM) {
					// 同时检测实体和方块射线，取近的
					Vec3d eye = client.player.getEyePos();
					Vec3d look = client.player.getRotationVec(1.0f);
					Vec3d end = eye.add(look.multiply(50.0));
					double blockDist = 50.0;
					double entityDist = 50.0;
					Entity entityTarget = null;
					HitResult hit = client.player.raycast(50.0, 0.0f, false);
					if (hit.getType() == HitResult.Type.BLOCK) {
						blockDist = hit.getPos().distanceTo(eye);
					}
					for (Entity e : client.world.getEntities()) {
						if (!(e instanceof LivingEntity) || e == client.player) continue;
						var boxHit = e.getBoundingBox().raycast(eye, end);
						if (boxHit.isPresent()) {
							double d = eye.distanceTo(boxHit.get());
							if (d < entityDist) { entityDist = d; entityTarget = e; }
						}
					}
					if (entityTarget != null && entityDist <= blockDist) {
						ClientPlayNetworking.send(new MarkEntityPayload(entityTarget.getId()));
					} else if (hit.getType() == HitResult.Type.BLOCK) {
						BlockHitResult blockHit = (BlockHitResult) hit;
						Vec3d pos = blockHit.getPos().add(0, 2.2, 0);
						ClientPlayNetworking.send(new PlaceParrotC2SPacket(pos));
						for (int i = 0; i < 30; i++)
							client.particleManager.addParticle(ParticleTypes.ENCHANT, pos.x, pos.y, pos.z, 0, 0.5, 0);
					} else {
						client.player.sendMessage(Text.literal("§c请对准实体或方块表面"), true);
					}
				} else if (mainHand.getItem() == ModItems.MAGIC_STICK) {
					Entity target = getTargetEntity(client, 50.0);
					if (target instanceof LivingEntity) {
						ClientPlayNetworking.send(new MagicPacket(target.getId()));
					}
				} else {
					if (client.player.isCreative()) {
						Entity target = getTargetEntity(client, 50.0);
						if (target instanceof PlayerEntity) {
							ClientPlayNetworking.send(new OpenOtherInventoryPayload(target.getId()));
							client.player.sendMessage(Text.literal("§a尝试打开目标玩家背包"), true);
						} else {
							client.player.sendMessage(Text.literal("§c请对准一名玩家"), true);
						}
					} else {
						client.player.sendMessage(Text.literal("§c功能未完善喵，仅创造模式才能使用此功能"), true);
					}
				}
			}
		});

		// ==================== 所有 S2C 包接收器（统一在此注册）====================

		// 1. 千里眼全局配置接收
		ClientPlayNetworking.registerGlobalReceiver(GlobalConfigS2CPacket.ID, (packet, context) -> {
			context.client().execute(() -> {
				try {
					JsonObject root = JsonParser.parseString(packet.json()).getAsJsonObject();
					GlobalConfigS2CPacket.StageConfig[] configs = new GlobalConfigS2CPacket.StageConfig[7];
					for (int i = 1; i <= 7; i++) {
						JsonObject stageObj = root.getAsJsonObject("stage" + i);
						configs[i - 1] = new GlobalConfigS2CPacket.StageConfig(
								stageObj.get("dailyLimit").getAsInt(),
								stageObj.get("maxMarks").getAsInt(),
								stageObj.get("minScore").getAsInt(),
								stageObj.get("maxScore").getAsInt(),
								stageObj.get("watchRequiredTicks").getAsInt(),
								stageObj.get("parrotDailyLimit").getAsInt(),
								stageObj.get("maxActiveParrots").getAsInt()
						);
					}
					if (context.client().currentScreen instanceof CombinedConfigScreen screen) {
						screen.receiveEvilConfigs(configs);
					}
				} catch (Exception e) { e.printStackTrace(); }
			});
		});

		// 2. 打开/关闭 UI（旧系统）
		ClientPlayNetworking.registerGlobalReceiver(OpenUIPacket.ID, (packet, context) -> {
			context.client().execute(() -> {
				if (context.client().currentScreen instanceof com.shushuwonie.client.gui.evil_eyes.Evil_eyesScreen) context.client().setScreen(null);
				else context.client().setScreen(new com.shushuwonie.client.gui.evil_eyes.Evil_eyesScreen());
			});
		});

		// 3. 实体标记更新
		ClientPlayNetworking.registerGlobalReceiver(EntityMarkedPayload.ID, (packet, context) -> {
			context.client().execute(() -> {
				UUID uuid = packet.entityUuid();
				// 清空信号：同步清空本地标记和GUI列表
				if (uuid.getMostSignificantBits() == 0 && uuid.getLeastSignificantBits() == 0) {
					com.shushuwonie.client.evil_eyes.Evil_EyesClient.localMarkedEntities.clear();
					com.shushuwonie.client.gui.evil_eyes.Evil_eyesScreen.updateMarkedList(com.shushuwonie.client.evil_eyes.Evil_EyesClient.localMarkedEntities);
					return;
				}
				long expire = context.client().world != null ? context.client().world.getTime() + 60 : System.currentTimeMillis() / 50 + 60;
				com.shushuwonie.client.evil_eyes.Evil_EyesClient.localMarkedEntities.put(uuid, expire);
				com.shushuwonie.client.gui.evil_eyes.Evil_eyesScreen.updateMarkedList(com.shushuwonie.client.evil_eyes.Evil_EyesClient.localMarkedEntities);
			});
		});

		// 4. 切换图片显示
		ClientPlayNetworking.registerGlobalReceiver(ToggleImagesS2CPacket.ID, (packet, context) -> {
			context.client().execute(() -> imagesEnabled = packet.enabled());
		});

		// 5. 选择观看（旧系统）
//		ClientPlayNetworking.registerGlobalReceiver(SelectViewPayload.ID, (packet, context) -> {
//			context.client().execute(() -> Evil_EyesClient.onSelectView(packet.entityUuid()));
//		});

		// 6. 强制退出观看（旧系统）
		ClientPlayNetworking.registerGlobalReceiver(ForceExitViewPayload.ID, (packet, context) -> {
			context.client().execute(() -> {
				CameraWatchClientHandler.onUnbind();
				com.shushuwonie.client.evil_eyes.Evil_EyesClient.exitViewMode(context.client());
			});
		});

		// 7. 标记粒子效果
		ClientPlayNetworking.registerGlobalReceiver(MarkParticleS2CPacket.ID, (packet, context) -> {
			context.client().execute(() -> {
				Vec3d pos = packet.pos();
				if (context.client().world != null) {
					context.client().particleManager.addParticle(ParticleTypes.ENCHANT, pos.x, pos.y, pos.z, 0, 0.5, 0);
				}
			});
		});

		// 8. 视线诱导配置同步
		ClientPlayNetworking.registerGlobalReceiver(SyncConfigS2CPacket.ID, (packet, context) -> {
			context.client().execute(() -> {
				GazeConfig config = GazeConfig.fromJson(packet.json());
				if (context.client().currentScreen instanceof CombinedConfigScreen screen) {
					screen.receiveGazeConfig(config);
				}
			});
		});

		// 9. 能量同步
		ClientPlayNetworking.registerGlobalReceiver(EnergySyncPacket.ID, (packet, context) -> {
			context.client().execute(() -> com.shushuwonie.client.gazeguidance.GazeguidanceClient.setEnergy(packet.currentEnergy(), packet.maxEnergy()));
		});

		// 10. 标记数量同步
		ClientPlayNetworking.registerGlobalReceiver(MarkCountPacket.ID, (packet, context) -> {
			context.client().execute(() -> com.shushuwonie.client.gazeguidance.GazeguidanceClient.setMarkCount(packet.count()));
		});

		// 11. 粒子效果（静态点）
		ClientPlayNetworking.registerGlobalReceiver(ParticlePacket.ID, (packet, context) -> {
			context.client().execute(() -> {
				if (context.client().world != null) {
					Vec3d pos = new Vec3d(packet.x(), packet.y(), packet.z());
					for (int i = 0; i < 20; i++) {
						context.client().particleManager.addParticle(ParticleTypes.END_ROD, pos.x, pos.y + 0.5, pos.z, 0.5, 0.5, 0.5);
					}
				}
			});
		});

		// 12. 强度阶段同步
		ClientPlayNetworking.registerGlobalReceiver(StrengthPacket.ID, (packet, context) -> {
			context.client().execute(() -> com.shushuwonie.client.gazeguidance.GazeguidanceClient.setStrength(packet.stage(), packet.maxMarks()));
		});

		// 13. 锚点粒子接收（同时缓存位置）
		ClientPlayNetworking.registerGlobalReceiver(AnchorParticleS2CPacket.ID, (packet, context) -> {
			context.client().execute(() -> {
				MinecraftClient client = context.client();
				if (client.world == null) return;
				Vec3d pos = packet.pos();
				if (packet.type() == 0) {
					client.particleManager.addParticle(ParticleTypes.PORTAL, pos.x, pos.y, pos.z, 0, 0.1, 0);
				} else {
					client.particleManager.addParticle(ParticleTypes.WHITE_SMOKE, pos.x, pos.y, pos.z, 0, 0.05, 0);
				}
				UUID standId = packet.standId();
				AnchorInfo info = anchors.get(standId);
				if (info == null) {
					info = new AnchorInfo();
					anchors.put(standId, info);
				}
				info.pos = pos;
				info.lastSeenTime = System.currentTimeMillis();
			});
		});

		// 14. 玩家阶段同步
		ClientPlayNetworking.registerGlobalReceiver(PlayerStageS2CPacket.ID, (packet, context) -> {
			context.client().execute(() -> {
				int newStage = packet.stage();
				currentPlayerStage = newStage;
				MinecraftClient client = context.client();
				if (client.player != null) {
					ItemStack mainHand = client.player.getMainHandStack();
					if (mainHand.getItem() == Evil_Eyes.CLAIRVOYANCE_ITEM) {
						if (newStage != lastNotifiedStage) {
							lastNotifiedStage = newStage;
							showStageUpgradeToast(newStage);
						}
					}
				}
			});
		});

		// 15. 爆炸粒子接收
		ClientPlayNetworking.registerGlobalReceiver(ExplosionParticleS2CPacket.ID, (packet, context) -> {
			context.client().execute(() -> {
				MinecraftClient client = context.client();
				if (client.world == null) return;
				Vec3d pos = packet.pos();
				for (int i = 0; i < 20; i++) {
					double ox = (client.world.random.nextDouble() - 0.5) * 1.5;
					double oy = (client.world.random.nextDouble() - 0.5) * 1.5;
					double oz = (client.world.random.nextDouble() - 0.5) * 1.5;
					client.particleManager.addParticle(ParticleTypes.EXPLOSION, pos.x + ox, pos.y + oy, pos.z + oz, 0, 0, 0);
					client.particleManager.addParticle(ParticleTypes.POOF, pos.x + ox, pos.y + oy, pos.z + oz, 0, 0, 0);
				}
			});
		});

		ClientCameraWatchReceiver.register();   // 注册网络接收
		CameraWatchClientHandler.initialize();  // 注册 tick 事件

		// 主动请求配置（C2S 包，无需注册接收）
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player != null && !hasRequestedConfig) {
				hasRequestedConfig = true;
				ClientPlayNetworking.send(new RequestGlobalConfigC2SPacket());
			}
		});

		// 世界渲染事件
		WorldRenderEvents.AFTER_ENTITIES.register(context -> {
			PlayerEntity player = MinecraftClient.getInstance().player;
			if (player != null) {
				ItemStack mainHand = player.getMainHandStack();
				if (mainHand.getItem() == Evil_Eyes.CLAIRVOYANCE_ITEM || mainHand.getItem() == ModItems.MAGIC_STICK) {
					renderBackTexture(context.matrixStack(), context.consumers(), player);
					renderOrbitTextures(context.matrixStack(), context.consumers(), player);
				}
			}
			renderAnchorButtons(context.matrixStack(), context.consumers());
		});

		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (entity instanceof ArmorStandEntity armorStand) {
				Text name = armorStand.getCustomName();
				if (name != null && name.getString().equals("clairvoyance_evil_eyes")) {
					ClientPlayNetworking.send(new AnchorDestroyC2SPacket(armorStand.getUuid()));
					player.swingHand(hand);
					player.sendMessage(Text.literal("§a锚点已破坏"), true);
					return ActionResult.FAIL;
				}
			}
			return ActionResult.PASS;
		});

		// 注册 Screen
		HandledScreens.register(Clairvoyance.OTHER_INVENTORY_HANDLER, OtherPlayerInventoryScreen::new);

		// 右键交互（搬运实体）
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (player instanceof ClientPlayerEntity clientPlayer && clientPlayer.isSneaking() &&
					clientPlayer.getMainHandStack().isEmpty() && clientPlayer.getOffHandStack().isEmpty()) {
				ClientPlayNetworking.send(new PlaceCarriedEntityPayload());
				return ActionResult.SUCCESS;
			}
			return ActionResult.PASS;
		});

		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (player instanceof ClientPlayerEntity clientPlayer && hand == Hand.MAIN_HAND) {
				if (clientPlayer.getMainHandStack().isEmpty() && clientPlayer.getOffHandStack().isEmpty()) {
					ClientPlayNetworking.send(new PlaceCarriedEntityPayload());
				}
			}
			return ActionResult.PASS;
		});

		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (player instanceof ClientPlayerEntity clientPlayer &&
					clientPlayer.getMainHandStack().isEmpty() && clientPlayer.getOffHandStack().isEmpty()) {
				if (entity instanceof LivingEntity) {
					ClientPlayNetworking.send(new CarryEntityPayload(entity.getId()));
					return ActionResult.SUCCESS;
				}
			}
			return ActionResult.PASS;
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) return;
			boolean currentEmpty = client.player.getMainHandStack().isEmpty();
			if (lastMainHandEmpty && !currentEmpty) {
				ClientPlayNetworking.send(new PlaceCarriedEntityPayload());
			}
			lastMainHandEmpty = currentEmpty;
		});

		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen) {
				ClientPlayNetworking.send(new PlaceCarriedEntityPayload());
			}
		});

		// 锚点过期清理
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.world == null) {
				if (!anchors.isEmpty()) anchors.clear();
				return;
			}
			long now = System.currentTimeMillis();
			anchors.entrySet().removeIf(entry -> now - entry.getValue().lastSeenTime > 3000);
		});

		// 注册模型层
		//躯干

		EntityModelLayerRegistry.registerModelLayer(ModModelLayers.TORSO, TorsoModel::getTexturedModelData);
		BlockEntityRendererFactories.register(
				ModBlockEntities.TORSO_BLOCK_ENTITY,
				TorsoBlockEntityRenderer::new
		);

		//左手
		EntityModelLayerRegistry.registerModelLayer(ModModelLayers.LEFT_ARM, LeftArmModel::getTexturedModelData);
		BlockEntityRendererFactories.register(
				ModBlockEntities.LEFT_ARM_BLOCK_ENTITY,
				LeftArmBlockEntityRenderer::new
		);

		//右手臂
		EntityModelLayerRegistry.registerModelLayer(ModModelLayers.RIGHT_ARM, RightArmModel::getTexturedModelData);
		BlockEntityRendererFactories.register(
				ModBlockEntities.RIGHT_ARM_BLOCK_ENTITY,
				RightArmBlockEntityRenderer::new
		);

		//左腿
		EntityModelLayerRegistry.registerModelLayer(ModModelLayers.LEFT_LEG, LeftLegModel::getTexturedModelData);
		BlockEntityRendererFactories.register(
				ModBlockEntities.LEFT_LEG_BLOCK_ENTITY,
				LeftLegBlockEntityRenderer::new
		);

		//右腿
		EntityModelLayerRegistry.registerModelLayer(ModModelLayers.RIGHT_LEG, RightLegModel::getTexturedModelData);
		BlockEntityRendererFactories.register(
				ModBlockEntities.RIGHT_LEG_BLOCK_ENTITY,
				RightLegBlockEntityRenderer::new
		);
	}

	// ==================== 渲染方法（保持不变）====================
	private static final double SPHERE_RADIUS = 0.2;
	static {
		BACK_IMAGES.add(new BackImage(Identifier.of("clairvoyance", "textures/gui/6.png"), 0.0f, 1.0f, 3.0f, 3.5f, 3.5f, 1.0f));
		BACK_IMAGES.add(new BackImage(Identifier.of("clairvoyance", "textures/gui/3.png"), -2f, 0.8f, 3.0f, 2.0f, 2.0f, -1.5f));
		BACK_IMAGES.add(new BackImage(Identifier.of("clairvoyance", "textures/gui/3.png"), 2f, 0.8f, 3.0f, 2.0f, 2.0f, -1.5f));
		BACK_IMAGES.add(new BackImage(Identifier.of("clairvoyance", "textures/gui/11.png"), 0.0f, 3f, 4.0f, 2.0f, 2.0f, 3.0f));
	}

	private static void renderAnchorButtons(MatrixStack matrices, VertexConsumerProvider consumers) {
		if (anchors.isEmpty()) return;
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null || client.player == null) return;
		Camera camera = client.gameRenderer.getCamera();
		Vec3d targetPos = camera.getPos();
		Identifier buttonTexture = Identifier.of("clairvoyance", "textures/gui/yj5.png");
		float size = 1.0f;
		float half = size / 2;
		int overlay = OverlayTexture.DEFAULT_UV;

		Vec3d playerChest = client.player.getEyePos();
		double offsetX = 0.0, offsetY = -0.4, offsetZ = 0.0;
		Iterator<Map.Entry<UUID, AnchorInfo>> it = anchors.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, AnchorInfo> entry = it.next();
			if (client.world.getEntity(entry.getKey()) == null) {
				it.remove();
				continue;
			}
			AnchorInfo info = entry.getValue();
			Vec3d armorStandPos = info.pos;
			Vec3d center = armorStandPos.add(offsetX, offsetY, offsetZ);
			Vec3d dir = playerChest.subtract(center).normalize();
			Vec3d worldPos = center.add(dir.multiply(SPHERE_RADIUS));
			int light = 0xF000F0;

			matrices.push();
			matrices.translate(worldPos.x - targetPos.x, worldPos.y - targetPos.y, worldPos.z - targetPos.z);
			float yaw = (float) Math.toDegrees(Math.atan2(targetPos.z - worldPos.z, targetPos.x - worldPos.x)) + 90;
			float pitch = (float) Math.toDegrees(Math.atan2(targetPos.y - worldPos.y,
					Math.hypot(targetPos.x - worldPos.x, targetPos.z - worldPos.z)));
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));

			RenderLayer layer = RenderLayer.getEntityTranslucent(buttonTexture);
			VertexConsumer vertex = consumers.getBuffer(layer);
			Matrix4f posMat = matrices.peek().getPositionMatrix();

			vertex.vertex(posMat, half, half, 0).color(255,255,255,255).texture(0,0).overlay(overlay).light(light).normal(0,0,1);
			vertex.vertex(posMat, half, -half, 0).color(255,255,255,255).texture(0,1).overlay(overlay).light(light).normal(0,0,1);
			vertex.vertex(posMat, -half, -half, 0).color(255,255,255,255).texture(1,1).overlay(overlay).light(light).normal(0,0,1);
			vertex.vertex(posMat, -half, half, 0).color(255,255,255,255).texture(1,0).overlay(overlay).light(light).normal(0,0,1);
			matrices.pop();
		}
	}

	private static void renderBackTexture(MatrixStack matrices, VertexConsumerProvider consumers, PlayerEntity player) {
		if (!imagesEnabled) return;
		ItemStack mainHand = player.getMainHandStack();
		boolean hasValidItem = mainHand.getItem() == Evil_Eyes.CLAIRVOYANCE_ITEM || mainHand.getItem() == ModItems.MAGIC_STICK;
		if (!hasValidItem) return;

		String playerName = player.getName().getString();
		boolean isSpecial = SPECIAL_NAMES.contains(playerName);
		List<BackImage> imagesToRender = new ArrayList<>();
		if (isSpecial && currentPlayerStage == 7) {
			imagesToRender.addAll(BACK_IMAGES);
		} else if (currentPlayerStage == 7 && !BACK_IMAGES.isEmpty()) {
			imagesToRender.add(BACK_IMAGES.get(0));
		}
		if (imagesToRender.isEmpty()) return;

		Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
		Vec3d targetPos = camera.getPos();
		Vec3d forward = player.getRotationVec(1.0f);
		Vec3d backDir = forward.multiply(-1);
		Vec3d rightDir = new Vec3d(forward.z, 0, -forward.x).normalize();

		matrices.push();
		for (BackImage img : imagesToRender) {
			Vec3d worldPos = player.getPos()
					.add(backDir.multiply(img.zOffset))
					.add(rightDir.multiply(img.xOffset))
					.add(0, img.yOffset, 0);
			matrices.push();
			matrices.translate(worldPos.x - targetPos.x, worldPos.y - targetPos.y, worldPos.z - targetPos.z);
			float backYaw = player.getYaw() + 180;
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-backYaw));
			if (img.selfRotateZ != 0) {
				float angle = (player.getWorld().getTime() * img.selfRotateZ) % 360;
				matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
			}
			float halfW = img.width / 2;
			float halfH = img.height / 2;
			RenderLayer layer = RenderLayer.getEntityTranslucent(img.texture);
			VertexConsumer vertex = consumers.getBuffer(layer);
			Matrix4f posMat = matrices.peek().getPositionMatrix();
			int light = WorldRenderer.getLightmapCoordinates(player.getWorld(), player.getBlockPos());
			int overlay = OverlayTexture.DEFAULT_UV;
			float nx = (float) backDir.x, ny = 0, nz = (float) backDir.z;
			vertex.vertex(posMat, -halfW, -halfH, 0).color(255,255,255,255).texture(0,0).overlay(overlay).light(light).normal(nx,ny,nz);
			vertex.vertex(posMat, -halfW, halfH, 0).color(255,255,255,255).texture(0,1).overlay(overlay).light(light).normal(nx,ny,nz);
			vertex.vertex(posMat, halfW, halfH, 0).color(255,255,255,255).texture(1,1).overlay(overlay).light(light).normal(nx,ny,nz);
			vertex.vertex(posMat, halfW, -halfH, 0).color(255,255,255,255).texture(1,0).overlay(overlay).light(light).normal(nx,ny,nz);
			matrices.pop();
		}
		matrices.pop();
	}

	private static void showStageUpgradeToast(int newStage) {
		MinecraftClient client = MinecraftClient.getInstance();
		SystemToast.show(client.getToastManager(), SystemToast.Type.NARRATOR_TOGGLE, Text.literal("§6千里眼"), Text.literal("阶段提升至 §a" + newStage));
	}

	private record OrbitImage(Identifier texture, double radius, double speed, double yOffset, float selfRotateSpeed, double startAngle) {}
	private static final List<OrbitImage> ORBIT_IMAGES = new ArrayList<>();
	static {
		ORBIT_IMAGES.add(new OrbitImage(Identifier.of("clairvoyance", "textures/gui/7.png"), 0.5, 5.0, -0.7, 10.0f,0.0));
		ORBIT_IMAGES.add(new OrbitImage(Identifier.of("clairvoyance", "textures/gui/8.png"), 0.5, 5.0, -0.7 ,10.0f,45.0));
		ORBIT_IMAGES.add(new OrbitImage(Identifier.of("clairvoyance", "textures/gui/7.png"), 0.5, 5.0, -0.7, 10.0f,90.0));
		ORBIT_IMAGES.add(new OrbitImage(Identifier.of("clairvoyance", "textures/gui/8.png"), 0.5, 5.0, -0.7, 10.0f,135.0));
		ORBIT_IMAGES.add(new OrbitImage(Identifier.of("clairvoyance", "textures/gui/7.png"), 0.5, 5.0, -0.7, 10.0f,180.0));
		ORBIT_IMAGES.add(new OrbitImage(Identifier.of("clairvoyance", "textures/gui/8.png"), 0.5, 5.0, -0.7 ,10.0f,225.0));
		ORBIT_IMAGES.add(new OrbitImage(Identifier.of("clairvoyance", "textures/gui/7.png"), 0.5, 5.0, -0.7, 10.0f,270.0));
		ORBIT_IMAGES.add(new OrbitImage(Identifier.of("clairvoyance", "textures/gui/8.png"), 0.5, 5.0, -0.7, 10.0f,315.0));
	}

	private static void renderOrbitTextures(MatrixStack matrices, VertexConsumerProvider consumers, PlayerEntity player) {
		if (!imagesEnabled) return;
		String playerName = player.getName().getString();
		boolean isSpecial = SPECIAL_NAMES.contains(playerName);
		List<OrbitImage> imagesToRender = new ArrayList<>();
		if (isSpecial && currentPlayerStage == 7) {
			imagesToRender.addAll(ORBIT_IMAGES);
		} else if (currentPlayerStage == 7 && !ORBIT_IMAGES.isEmpty()) {
			imagesToRender.add(ORBIT_IMAGES.get(0));
			imagesToRender.add(ORBIT_IMAGES.get(2));
			imagesToRender.add(ORBIT_IMAGES.get(4));
			imagesToRender.add(ORBIT_IMAGES.get(6));
		}
		if (imagesToRender.isEmpty()) return;

		if (player == null) return;
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) return;
		Camera camera = client.gameRenderer.getCamera();
		Vec3d targetPos = player.getPos().add(0, player.getHeight() / 2, 0);
		Vec3d center = player.getPos().add(0, player.getHeight() * 0.5, 0);
		long gameTime = player.getWorld().getTime();
		double angleBase = (gameTime / 20.0) * 360;

		for (OrbitImage img : imagesToRender) {
			double angle = angleBase * (img.speed / 360.0) + img.startAngle;
			double rad = Math.toRadians(angle);
			double x = img.radius * Math.cos(rad);
			double z = img.radius * Math.sin(rad);
			Vec3d worldPos = center.add(x, img.yOffset, z);

			int light = WorldRenderer.getLightmapCoordinates(client.world, BlockPos.ofFloored(worldPos));
			int overlay = OverlayTexture.DEFAULT_UV;
			float size = 0.2f;
			float half = size / 2;

			matrices.push();
			matrices.translate(worldPos.x - targetPos.x, worldPos.y - targetPos.y, worldPos.z - targetPos.z);
			float yaw = (float) Math.toDegrees(Math.atan2(targetPos.z - worldPos.z, targetPos.x - worldPos.x)) + 90;
			float pitch = (float) Math.toDegrees(Math.atan2(targetPos.y - worldPos.y,
					Math.hypot(targetPos.x - worldPos.x, targetPos.z - worldPos.z)));
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
			float selfAngle = (float) ((gameTime * img.selfRotateSpeed / 20.0) % 360);
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(selfAngle));

			RenderLayer layer = RenderLayer.getEntityTranslucent(img.texture);
			VertexConsumer vertex = consumers.getBuffer(layer);
			Matrix4f posMat = matrices.peek().getPositionMatrix();

			vertex.vertex(posMat, -half, -half, 0).color(255,255,255,128).texture(0,0).overlay(overlay).light(light).normal(0,0,1);
			vertex.vertex(posMat, -half, half, 0).color(255,255,255,128).texture(0,1).overlay(overlay).light(light).normal(0,0,1);
			vertex.vertex(posMat, half, half, 0).color(255,255,255,128).texture(1,1).overlay(overlay).light(light).normal(0,0,1);
			vertex.vertex(posMat, half, -half, 0).color(255,255,255,128).texture(1,0).overlay(overlay).light(light).normal(0,0,1);
			matrices.pop();
		}
	}
}
