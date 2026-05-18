package com.shushuwonie.clairvoyance.features.guidance;

import com.shushuwonie.clairvoyance.item.gazeguidance.ModItems;
import com.shushuwonie.clairvoyance.item.config.GazeConfig;
import com.shushuwonie.clairvoyance.network.gazeguidance.*;
//import com.shushuwonie.client.network.gazeguidance.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Gazeguidance {
	public static final String MOD_ID = "gazeguidance";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final long COOLDOWN_TICKS = 100;
	private static final Map<UUID, Long> lastFocusEndTime = new ConcurrentHashMap<>();

	private static UUID currentFocusEntityId = null;
	private static UUID temporaryFocusPlayerId = null;
	private static UUID temporaryFocusEntityId = null;

	private static final Map<UUID, Double> playerEnergy = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> markedEntities = new ConcurrentHashMap<>();

	private static final Map<UUID, Integer> lastSentStage = new ConcurrentHashMap<>();
	private static int currentStage = 1;
	private static final Map<UUID, Long> focusStartTime = new ConcurrentHashMap<>();

	private static int globalEnergySyncTick = 0;
	private static int particleTickCounter = 0;

	// 焦点类型枚举
	private enum FocusType { SELF, ENTITY, STATIC }
	private static FocusType currentFocusType = null;

	private static final Set<String> SPECIAL_NAMES = Set.of("shushuwonie", "Remio","Ice_in_North");

	// 禁用状态（内存存储，服务器重启后重置）
	private static final Set<UUID> IMAGES_DISABLED = ConcurrentHashMap.newKeySet();
	private static final Set<UUID> PARTICLES_DISABLED = ConcurrentHashMap.newKeySet();


	// ========== 阶段计算 ==========
	private static int getStageFromScore(int score) {
		if (score <= 5) return 1;
		else if (score <= 20) return 2;
		else if (score <= 40) return 3;
		else if (score <= 60) return 4;
		else if (score <= 70) return 5;
		else if (score <= 80) return 6;
		else return 7;
	}

	private static int getPlayerStage(PlayerEntity player) {
		Scoreboard scoreboard = player.getWorld().getScoreboard();
		var objective = scoreboard.getNullableObjective("monvhua");
		if (objective == null) return 1;
		var score = scoreboard.getScore(player, objective);
		if (score == null) return 1;
		int rawScore = Math.max(0, Math.min(100, score.getScore()));
		return getStageFromScore(rawScore);
	}

	// ========== 阶段属性 ==========
	private static double getAttractRadiusFromStage(int stage) {
		return GazeConfig.getInstance().getRadius(stage);
	}

	private static int getMaxMarkedCount(int stage) {
		return GazeConfig.getInstance().getMaxMarks(stage);
	}

	private static void sendStageIfChanged(ServerPlayerEntity player, int newStage) {
		UUID uuid = player.getUuid();
		Integer last = lastSentStage.get(uuid);
		if (last == null || last != newStage) {
			lastSentStage.put(uuid, newStage);
			double drain = GazeConfig.getInstance().getEnergyDrain(newStage);
			double regen = GazeConfig.getInstance().getEnergyRegen(newStage);
			double range = GazeConfig.getInstance().getRadius(newStage);
			int maxMarks = GazeConfig.getInstance().getMaxMarks(newStage);
			ServerPlayNetworking.send(player, new StrengthPacket(newStage, drain, regen, range, maxMarks));
//			LOGGER.info("发送阶段 {} 消耗={} 回复={} 范围={} 最大标记={}", newStage, drain, regen, range, maxMarks);
		}
	}


	public static void initialize() {


		// 命令
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(CommandManager.literal("clairvoyance")
						.then(CommandManager.literal("resetcooldown_重置冷却")
								.executes(context -> {
									PlayerEntity player = context.getSource().getPlayer();
									if (player != null) {
										lastFocusEndTime.remove(player.getUuid());
										context.getSource().sendFeedback(() -> Text.literal("§a已重置你的诱导冷却"), false);
									}
									return 1;
								})
								.then(CommandManager.argument("target", EntityArgumentType.player())
										.requires(source -> source.hasPermissionLevel(2))
										.executes(context -> {
											PlayerEntity target = EntityArgumentType.getPlayer(context, "target");
											lastFocusEndTime.remove(target.getUuid());
											context.getSource().sendFeedback(() -> Text.literal("§a已重置 " + target.getName().getString() + " 的诱导冷却"), false);
											return 1;
										})
								)

						)
						.then(CommandManager.literal("clearmarks_清除诱导标记实体")
								.requires(source -> source.hasPermissionLevel(2))  // 需要 OP 权限（2级）或创造模式可自行调整
								.executes(context -> {
									markedEntities.clear();
									// 广播标记数量清零给所有在线玩家
									for (ServerPlayerEntity player : context.getSource().getServer().getPlayerManager().getPlayerList()) {
										ServerPlayNetworking.send(player, new MarkCountPacket(0));
									}
									context.getSource().sendFeedback(() -> Text.literal("§a已清除所有标记实体"), false);
									return 1;
								})
						)

						// 在 CommandRegistrationCallback 内部添加以下两个 then
						.then(CommandManager.literal("toggleimages__开|关图片")
								.requires(source -> source.hasPermissionLevel(2))
								.then(CommandManager.argument("target", EntityArgumentType.player())
										.executes(context -> {
											ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
											UUID uuid = target.getUuid();
											// 当前是否被禁用？如果禁用，则要开启；否则要关闭。
											boolean currentlyDisabled = IMAGES_DISABLED.contains(uuid);
											boolean newState = currentlyDisabled;  // 新状态：true=开启，false=关闭
											if (currentlyDisabled) {
												IMAGES_DISABLED.remove(uuid);
											} else {
												IMAGES_DISABLED.add(uuid);
											}
											ServerPlayNetworking.send(target, new ToggleImagesS2CPacket(newState));
											context.getSource().sendFeedback(() ->
													Text.literal("§a已" + (newState ? "开启" : "关闭") + "玩家 " + target.getName().getString() + " 的图片显示"), false);
											return 1;
										})
								)
						)
						.then(CommandManager.literal("toggleparticles__开|关粒子")
								.requires(source -> source.hasPermissionLevel(2))
								.then(CommandManager.argument("target", EntityArgumentType.player())
										.executes(context -> {
											ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
											UUID uuid = target.getUuid();
											boolean currentlyDisabled = PARTICLES_DISABLED.contains(uuid);
											boolean newState = currentlyDisabled;  // 新状态：true=开启，false=关闭
											if (currentlyDisabled) {
												PARTICLES_DISABLED.remove(uuid);
											} else {
												PARTICLES_DISABLED.add(uuid);
											}
											// 粒子环不需要客户端包，服务端直接判断集合即可
											context.getSource().sendFeedback(() ->
													Text.literal("§a已" + (newState ? "开启" : "关闭") + "玩家 " + target.getName().getString() + " 的粒子环显示"), false);
											return 1;
										})
								)
						)
				)
		);

		// 注册网络包


		// 禁止左键破坏方块
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			ItemStack stack = player.getStackInHand(hand);
			return stack.getItem() == ModItems.MAGIC_STICK ? ActionResult.FAIL : ActionResult.PASS;
		});

		// 右键长按开始/结束诱导
		ServerPlayNetworking.registerGlobalReceiver(RightClickActionPacket.ID, (payload, context) -> {
			context.server().execute(() -> {
				PlayerEntity player = context.player();
				if (player.getMainHandStack().getItem() != ModItems.MAGIC_STICK) return;
				if (payload.start()) {
					setTemporaryFocus(player, player.getWorld(), player.isSneaking());
				} else {
					endFocus(player);
				}
			});
		});
		// 客户端请求配置
		ServerPlayNetworking.registerGlobalReceiver(RequestConfigC2SPacket.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			String json = GazeConfig.getInstance().toJson(); // 需要 GazeConfig 有 toJson()
			ServerPlayNetworking.send(player, new SyncConfigS2CPacket(json));
		});

// 客户端更新配置
		ServerPlayNetworking.registerGlobalReceiver(UpdateConfigC2SPacket.ID, (payload, context) -> {
			context.server().execute(() -> {
				GazeConfig newConfig = GazeConfig.fromJson(payload.json());
				GazeConfig.setInstance(newConfig);
				// 广播新配置给所有在线玩家（无论模式）
				for (ServerPlayerEntity player : context.server().getPlayerManager().getPlayerList()) {
					ServerPlayNetworking.send(player, new SyncConfigS2CPacket(newConfig.toJson()));
				}
				// 仅当发起更新的玩家是创造模式时，显示成功提示（通常已经是创造模式，但二次确认）
				if (context.player() != null && context.player().isCreative()) {
					context.player().sendMessage(Text.literal("§a配置已更新并同步至所有玩家"), false);
				}
			});
		});
		// G 键标记实体（支持取消）
		ServerPlayNetworking.registerGlobalReceiver(MagicPacket.ID, (payload, context) -> {
			context.server().execute(() -> {
				var player = context.player();
				var world = player.getWorld();
				var entity = world.getEntityById(payload.entityId());
				if (entity instanceof LivingEntity target && player.getMainHandStack().getItem() == ModItems.MAGIC_STICK) {
					double maxRange = 50.0;
					if (player.squaredDistanceTo(target) > maxRange * maxRange) {
						player.sendMessage(Text.literal("§c目标过远，无法标记"), true);
						return;
					}

					UUID targetUuid = target.getUuid();
					int stage = getPlayerStage(player);
					int maxMarks = getMaxMarkedCount(stage);

					if (markedEntities.containsKey(targetUuid)) {
						// 取消标记
						markedEntities.remove(targetUuid);
						player.sendMessage(Text.literal("§e已取消标记 " + target.getName().getString()), true);
						if (player instanceof ServerPlayerEntity sp) {
							sendStageIfChanged(sp, stage);
							ServerPlayNetworking.send(sp, new MarkCountPacket(markedEntities.size()));
						}
					} else {
						if (markedEntities.size() >= maxMarks) {
							player.sendMessage(Text.literal("§c标记已达上限，无法继续标记"), true);
							return;
						}
						long expiryTime = world.getTime() + 1200;
						markedEntities.put(targetUuid, expiryTime);
						player.sendMessage(Text.literal("§a已标记 " + target.getName().getString() + " (持续60秒)"), true);
						if (player instanceof ServerPlayerEntity sp) {
							sendStageIfChanged(sp, stage);
							ServerPlayNetworking.send(sp, new MarkCountPacket(markedEntities.size()));
						}
//						LOGGER.info("标记 {} (阶段 {})", target.getName().getString(), stage);
					}
				}
			});
		});

		// 每 tick 处理
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			// 更新当前阶段
			if (temporaryFocusPlayerId != null) {
				PlayerEntity p = server.getPlayerManager().getPlayer(temporaryFocusPlayerId);
				if (p != null) {
					int newStage = getPlayerStage(p);
					if (newStage != currentStage) {
						currentStage = newStage;
						if (p instanceof ServerPlayerEntity sp) sendStageIfChanged(sp, newStage);
					}
				}
			}

			// 焦点实体有效性检查
			if (temporaryFocusEntityId != null) {
				World w = server.getWorld(World.OVERWORLD);
				if (w instanceof ServerWorld sw) {
					Entity e = sw.getEntity(temporaryFocusEntityId);
					if (e == null || !e.isAlive()) {
						temporaryFocusEntityId = null;
						currentFocusEntityId = null;
						temporaryFocusPlayerId = null;
						currentFocusType = null;
					} else {
						currentFocusEntityId = temporaryFocusEntityId;
					}
				}
			}

			// 自我诱导持续条件检查：必须手持魔杖且潜行，否则立即结束
			if (temporaryFocusPlayerId != null && currentFocusType == FocusType.SELF) {
				PlayerEntity fp = server.getPlayerManager().getPlayer(temporaryFocusPlayerId);
				if (fp != null) {
					boolean stillValid = fp.getMainHandStack().getItem() == ModItems.MAGIC_STICK && fp.isSneaking();
					if (!stillValid) {
						endFocus(fp);
					}
				}
			}

			// 检查诱导玩家是否仍满足条件：
			// 1. 必须手持魔法棒；
			// 2. 如果是自我诱导，还必须保持潜行。
			if (temporaryFocusPlayerId != null && currentFocusEntityId != null) {
				PlayerEntity fp = server.getPlayerManager().getPlayer(temporaryFocusPlayerId);
				if (fp != null) {
					boolean valid = fp.getMainHandStack().getItem() == ModItems.MAGIC_STICK;
					if (valid && currentFocusType == FocusType.SELF && !fp.isSneaking()) {
						valid = false;
					}
					if (!valid) {
						endFocus(fp);
						fp.sendMessage(Text.literal("§c诱导条件不满足，已结束"), true);
					}
				}
			}


			// 清理过期标记和已死亡的实体
			World overworld = server.getWorld(World.OVERWORLD);
			if (overworld != null) {
				long now = overworld.getTime();
				boolean removed = false; // 标记是否有移除
				Iterator<Map.Entry<UUID, Long>> iterator = markedEntities.entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry<UUID, Long> entry = iterator.next();
					Entity e = overworld.getEntity(entry.getKey());
					if (entry.getValue() <= now || e == null || !e.isAlive()) {
						iterator.remove();
						removed = true;
					}
				}
				// 如果移除了标记，需要通知诱导玩家更新计数
				if (removed && temporaryFocusPlayerId != null) {
					PlayerEntity fp = server.getPlayerManager().getPlayer(temporaryFocusPlayerId);
					if (fp instanceof ServerPlayerEntity sp) {
						ServerPlayNetworking.send(sp, new MarkCountPacket(markedEntities.size()));
					}
				}
			}

			// 能量管理
			if (temporaryFocusPlayerId != null && currentFocusEntityId != null) {
				PlayerEntity fp = server.getPlayerManager().getPlayer(temporaryFocusPlayerId);
				if (fp != null) {
					int markedCount = markedEntities.size();
					double drainRate = GazeConfig.getInstance().getEnergyDrain(currentStage);
					double energyCost = markedCount * drainRate / 20.0;
					double currentEnergy = playerEnergy.getOrDefault(fp.getUuid(), GazeConfig.getInstance().maxEnergy);
					currentEnergy -= energyCost;
					if (currentEnergy <= 0) {
						endFocus(fp);
						fp.sendMessage(Text.literal("§c能量耗尽，诱导结束"), true);
					} else {
						playerEnergy.put(fp.getUuid(), currentEnergy);
					}
				}
			} else if (temporaryFocusPlayerId != null) {
				PlayerEntity fp = server.getPlayerManager().getPlayer(temporaryFocusPlayerId);
				if (fp != null) endFocus(fp);
			}

			// 能量回复
			for (Map.Entry<UUID, Double> entry : playerEnergy.entrySet()) {
				PlayerEntity p = server.getPlayerManager().getPlayer(entry.getKey());
				if (p == null) continue;
				if (temporaryFocusPlayerId == null || !temporaryFocusPlayerId.equals(p.getUuid())) {
					double regenRate = GazeConfig.getInstance().getEnergyRegen(getPlayerStage(p));
					double newEnergy = entry.getValue() + regenRate / 20.0;
					if (newEnergy > GazeConfig.getInstance().maxEnergy) newEnergy = GazeConfig.getInstance().maxEnergy;
					playerEnergy.put(p.getUuid(), newEnergy);
				}
			}

			// 每 5 tick 同步能量
			globalEnergySyncTick++;
			if (globalEnergySyncTick >= 5) {
				globalEnergySyncTick = 0;
				for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
					if (player.getMainHandStack().getItem() == ModItems.MAGIC_STICK) {
						double current = playerEnergy.getOrDefault(player.getUuid(), GazeConfig.getInstance().maxEnergy);
						double max = GazeConfig.getInstance().maxEnergy;
						ServerPlayNetworking.send(player, new EnergySyncPacket(current, max));
					}
				}
			}

			// 每 tick 递增粒子计数器
			// 每 tick 递增粒子计数器
			particleTickCounter++;
			if (particleTickCounter >= 15 && temporaryFocusPlayerId != null) {
				particleTickCounter = 0;
				PlayerEntity fp = server.getPlayerManager().getPlayer(temporaryFocusPlayerId);
				if (fp instanceof ServerPlayerEntity sp) {
					// 直接使用上面已经定义的 overworld 变量（需要在外部定义且不为 null）
					if (overworld instanceof ServerWorld serverWorld) {
						for (UUID uuid : markedEntities.keySet()) {
							Entity e = serverWorld.getEntity(uuid);
							if (e instanceof LivingEntity living && living.isAlive()) {
								ServerPlayNetworking.send(sp, new MarkParticleS2CPacket(living.getPos()));
							}
						}
					}
				}
			}





			// 吸引逻辑
			if (currentFocusEntityId != null && overworld instanceof ServerWorld serverWorld) {
				Entity focus = serverWorld.getEntity(currentFocusEntityId);
				if (focus != null && focus.isAlive()) {
					Vec3d focusPos = focus.getPos();
					double attractRadius = getAttractRadiusFromStage(currentStage);
					for (UUID uuid : markedEntities.keySet()) {
						Entity e = serverWorld.getEntity(uuid);
						if (e instanceof LivingEntity living && living.isAlive() && !living.getUuid().equals(currentFocusEntityId)) {
							if (living.getPos().squaredDistanceTo(focusPos) <= attractRadius * attractRadius) {
								living.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, focusPos);
							}
						}
					}
				}
			}


		});

//		LOGGER.info("GazeGuidance initialized with energy system. Press I (creative mode) to config stages.");

			//==============生成环================//
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTicks() % 10 != 0) return; // 每 10 tick 生成一次
			World world = server.getWorld(World.OVERWORLD);
			if (!(world instanceof ServerWorld serverWorld)) return;
			long tick = server.getTicks();
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				// 1. 检查粒子显示开关是否被禁用
				if (PARTICLES_DISABLED.contains(player.getUuid())) continue;
				// 2. 获取玩家阶段，只有 stage 7 才显示粒子环（与图片逻辑对齐）
				int stage = getPlayerStage(player);
				if (stage != 7) continue;

				boolean isSpecial = SPECIAL_NAMES.contains(player.getName().getString());
				for (ParticleRingConfig config : PARTICLE_RINGS) {
					boolean match = (config.onlyForPlayer != null && config.onlyForPlayer.equals(player.getName().getString()))
							|| (config.onlyForSpecial && isSpecial);
					if (!match) continue;
					spawnCustomRingParticle(serverWorld, player, config, tick);
				}
			}
		});


		// 每秒检查所有玩家阶段变化，并推送 StrengthPacket 给手持魔法棒的玩家
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTicks() % 20 != 0) return; // 每秒一次
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				ItemStack mainHand = player.getMainHandStack();
				if (mainHand.getItem() != ModItems.MAGIC_STICK) continue; // 只处理手持魔法棒的玩家

				int newStage = getPlayerStage(player);
				UUID uuid = player.getUuid();
				Integer lastStage = lastSentStage.get(uuid);
				if (lastStage == null || lastStage != newStage) {
					// 阶段变化，发送 StrengthPacket
					sendStageIfChanged(player, newStage);
					lastSentStage.put(uuid, newStage);
				}
			}
		});


	}

	// 结束诱导
	private static void endFocus(PlayerEntity player) {
		if (temporaryFocusPlayerId != null && temporaryFocusPlayerId.equals(player.getUuid())) {
			Long startTime = focusStartTime.get(player.getUuid());
			long duration = 0;
			if (startTime != null) {
				duration = player.getWorld().getTime() - startTime;
				focusStartTime.remove(player.getUuid());
			}
			if (duration >= 40) {
				lastFocusEndTime.put(player.getUuid(), player.getWorld().getTime());
				player.sendMessage(Text.literal("§e诱导结束，进入5秒冷却"), true);
			} else {
				player.sendMessage(Text.literal("§7诱导结束（持续时间过短，无冷却）"), true);
			}

			if (temporaryFocusEntityId != null) {
				World w = player.getWorld();
				if (w instanceof ServerWorld sw) {
					Entity old = sw.getEntity(temporaryFocusEntityId);
					if (old instanceof ArmorStandEntity) old.discard();
				}
			}

			temporaryFocusPlayerId = null;
			temporaryFocusEntityId = null;
			currentFocusEntityId = null;
			currentFocusType = null;
			markedEntities.clear();

			if (player instanceof ServerPlayerEntity sp) {
				ServerPlayNetworking.send(sp, new FocusStatusPacket(false));
			}
		}
	}

	private static void setTemporaryFocus(PlayerEntity player, World world, boolean self) {
		long now = world.getTime();
		Long lastEnd = lastFocusEndTime.get(player.getUuid());
		if (lastEnd != null && now - lastEnd < COOLDOWN_TICKS) {
			long remaining = (COOLDOWN_TICKS - (now - lastEnd)) / 20;
			player.sendMessage(Text.literal("§c诱导冷却中，还需 " + remaining + " 秒"), true);
			return;
		}

		if (temporaryFocusPlayerId != null && !temporaryFocusPlayerId.equals(player.getUuid())) {
			player.sendMessage(Text.literal("§c已有其他玩家正在诱导，请等待"), true);
			return;
		}
		if (temporaryFocusPlayerId != null && temporaryFocusPlayerId.equals(player.getUuid())) {
			player.sendMessage(Text.literal("§c你已经有一个诱导焦点，请先结束"), true);
			return;
		}

		ServerWorld serverWorld = (ServerWorld) world;
		temporaryFocusPlayerId = player.getUuid();

		// 初始化能量
		playerEnergy.put(player.getUuid(), GazeConfig.getInstance().maxEnergy);

		int stage = getPlayerStage(player);
		if (player instanceof ServerPlayerEntity sp) {
			sendStageIfChanged(sp, stage);
		}

		if (self) {
			if (temporaryFocusEntityId != null && temporaryFocusEntityId.equals(player.getUuid())) return;
			if (temporaryFocusEntityId != null) {
				Entity old = serverWorld.getEntity(temporaryFocusEntityId);
				if (old instanceof ArmorStandEntity) old.discard();
			}
			temporaryFocusEntityId = player.getUuid();
			currentFocusType = FocusType.SELF;
			player.sendMessage(Text.literal("诱导开启 (自己)"), true);
		} else {
			double placementRange = 20.0;
			Entity targetEntity = getTargetEntity(player, placementRange);
			if (targetEntity != null) {
				if (temporaryFocusEntityId != null && temporaryFocusEntityId.equals(targetEntity.getUuid())) return;
				if (temporaryFocusEntityId != null) {
					Entity old = serverWorld.getEntity(temporaryFocusEntityId);
					if (old instanceof ArmorStandEntity) old.discard();
				}
				temporaryFocusEntityId = targetEntity.getUuid();
				currentFocusType = FocusType.ENTITY;
				player.sendMessage(Text.literal("诱导开启 (实体)"), true);
			} else {
				Vec3d hitPos = getPlayerLookBlockPosition(player, placementRange);
				if (hitPos != null) {
					if (temporaryFocusEntityId != null) {
						Entity old = serverWorld.getEntity(temporaryFocusEntityId);
						if (old instanceof ArmorStandEntity) {
							if (old.getPos().squaredDistanceTo(hitPos) < 0.01) return;
							old.discard();
						}
					}
					ArmorStandEntity focus = new ArmorStandEntity(EntityType.ARMOR_STAND, serverWorld);
					focus.setInvisible(true);
					focus.setInvulnerable(true);
					focus.setNoGravity(true);
					focus.setPosition(hitPos.x, hitPos.y, hitPos.z);
					serverWorld.spawnEntity(focus);
					temporaryFocusEntityId = focus.getUuid();
					currentFocusType = FocusType.STATIC;
					if (player instanceof ServerPlayerEntity sp) {
						ServerPlayNetworking.send(sp, new ParticlePacket(hitPos));
					}
					player.sendMessage(Text.literal("诱导开启 (静态点)"), true);
				} else {
					player.sendMessage(Text.literal("无法诱导，请对准实体或方块"), true);
					temporaryFocusPlayerId = null;
					return;
				}
			}
		}
		currentFocusEntityId = temporaryFocusEntityId;
		if (player instanceof ServerPlayerEntity sp) {
			ServerPlayNetworking.send(sp, new FocusStatusPacket(true));
		}
		focusStartTime.put(player.getUuid(), world.getTime());
	}

	private static Entity getTargetEntity(PlayerEntity player, double maxRange) {
		World world = player.getWorld();
		Vec3d start = player.getEyePos();
		Vec3d direction = player.getRotationVec(1.0f);
		Vec3d end = start.add(direction.multiply(maxRange));

		BlockHitResult blockHit = world.raycast(new RaycastContext(start, end,
				RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
		double blockDistSq = blockHit.getPos().squaredDistanceTo(start);

		Box searchBox = player.getBoundingBox().stretch(direction.multiply(maxRange)).expand(1.0);
		Entity closest = null;
		double closestDistSq = maxRange * maxRange;

		for (Entity entity : world.getOtherEntities(player, searchBox,
				e -> e instanceof LivingEntity && e.isAlive())) {
			Vec3d hitPos = entity.getBoundingBox().raycast(start, end).orElse(null);
			if (hitPos != null) {
				double distSq = start.squaredDistanceTo(hitPos);
				if (distSq < closestDistSq && distSq < blockDistSq) {
					closestDistSq = distSq;
					closest = entity;
				}
			}
		}
		return closest;
	}

	private static Vec3d getPlayerLookBlockPosition(PlayerEntity player, double maxDistance) {
		Vec3d start = player.getEyePos();
		Vec3d direction = player.getRotationVec(1.0f);
		Vec3d end = start.add(direction.multiply(maxDistance));
		BlockHitResult result = player.getWorld().raycast(new RaycastContext(start, end,
				RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
		return result.getType() == HitResult.Type.BLOCK ? result.getPos() : null;
	}


						//===================粒子效果=======================//
	public static class ParticleRingConfig {
		public final String name;
		public final double radius;            // 三叶草半径（形状大小）
		public final double sphereRadius;      // 球面半径（环中心到球心的距离）
		public final double rotationSpeed;     // 自转速度（度/秒）
		public final ParticleEffect particleEffect;
		public final int particleCount;
		public final double speedFactor;       // 速度因子（可忽略）
		public final double sphereCenterOffsetX;   // 球心 X 轴偏移（相对于玩家腰部）
		public final double sphereCenterOffsetY;   // 球心 Y 轴偏移（正向上）
		public final double sphereCenterOffsetZ;   // 球心 Z 轴偏移（正向前）
		public final float yaw;                 // 环中心方向偏航角（度，绕 Y 轴）
		public final float pitch;               // 环中心方向俯仰角（度，绕 X 轴）
		public final String onlyForPlayer;      // 指定玩家名，区分大小写
		public final boolean onlyForSpecial;    // 是否为特殊名字玩家

		public ParticleRingConfig(String name, double radius, double sphereRadius, double rotationSpeed,
								  ParticleEffect particleEffect, int particleCount, double speedFactor,
								  double sphereCenterOffsetX, double sphereCenterOffsetY, double sphereCenterOffsetZ,
								  float yaw, float pitch, String onlyForPlayer,boolean onlyForSpecial) {
			this.name = name;
			this.radius = radius;
			this.sphereRadius = sphereRadius;
			this.rotationSpeed = rotationSpeed;
			this.particleEffect = particleEffect;
			this.particleCount = particleCount;
			this.speedFactor = speedFactor;
			this.sphereCenterOffsetX = sphereCenterOffsetX;
			this.sphereCenterOffsetY = sphereCenterOffsetY;
			this.sphereCenterOffsetZ = sphereCenterOffsetZ;
			this.yaw = yaw;
			this.pitch = pitch;
			this.onlyForPlayer = onlyForPlayer;
			this.onlyForSpecial = onlyForSpecial;
		}
	}

	private static final List<ParticleRingConfig> PARTICLE_RINGS = new ArrayList<>();

	static {
		// 主环（白色，半径0.6，腰部，不旋转）
//		PARTICLE_RINGS.add(new ParticleRingConfig("main", 0.6, 0.0, 0.0, ParticleTypes.GLOW, 32, 0.0,0.0f,0,0,0));
		// 第二环（红色，半径0.9，腰部偏上，逆时针旋转30度/秒）
		PARTICLE_RINGS.add(new ParticleRingConfig("ice", 0.8, 5, 0, ParticleTypes.GLOW, 30, 0.0, 0.0f, 0.0f, 0.0, 130,45,"Ice_in_North",false));
		PARTICLE_RINGS.add(new ParticleRingConfig("ice", 0.8, 5, 0, ParticleTypes.GLOW, 30, 0.0, 0.0f, 0.0f, 0.0, -130,45,"Ice_in_North",false));
		// 第三环（绿色，半径1.2，头部高度，顺时针45度/秒）
		PARTICLE_RINGS.add(new ParticleRingConfig("ice", 2, 2, 0.5, ParticleTypes.END_ROD, 60, 0.0, 0.0f, 0, 0.0f, 180,-10,"Ice_in_North",false));
	}
	private static void spawnCustomRingParticle(ServerWorld world, ServerPlayerEntity player, ParticleRingConfig config, long tick) {




		// 1. 球心 = 玩家腰部 + 自定义偏移（绝对坐标）
		Vec3d playerCenter = player.getPos().add(0, player.getHeight() * 0.5, 0);
		Vec3d sphereCenter = playerCenter.add(config.sphereCenterOffsetX, config.sphereCenterOffsetY, config.sphereCenterOffsetZ);

		// 2. 计算玩家水平朝向（忽略俯仰）
		Vec3d lookVec = player.getRotationVec(1.0f);
		Vec3d forward = new Vec3d(lookVec.x, 0, lookVec.z).normalize();

		// 3. 根据玩家水平朝向 + 自定义偏航/俯仰，计算环中心的方向
		// 使用四元数旋转：先绕Y轴旋转 yaw，再绕新的右轴旋转 pitch
		org.joml.Quaternionf quat = new org.joml.Quaternionf();
		// 绕Y轴旋转 yaw
		quat.rotateY((float) Math.toRadians(config.yaw));
		// 应用旋转到 forward 向量
		org.joml.Vector3f dirVec = new org.joml.Vector3f((float) forward.x, 0, (float) forward.z).normalize();
		quat.transform(dirVec);
		// 计算右轴（用于俯仰旋转）
		org.joml.Vector3f rightVec = new org.joml.Vector3f(-dirVec.z, 0, dirVec.x).normalize();
		// 绕右轴旋转 pitch
		org.joml.Quaternionf quatPitch = new org.joml.Quaternionf().rotateAxis((float) Math.toRadians(config.pitch), rightVec.x, rightVec.y, rightVec.z);
		quatPitch.transform(dirVec);
		Vec3d direction = new Vec3d(dirVec.x, dirVec.y, dirVec.z).normalize();

		// 4. 环中心 = 球心 + 方向 * 球面半径
		Vec3d ringCenter = sphereCenter.add(direction.multiply(config.sphereRadius));

		// 5. 法线方向：从环中心指向球心
		Vec3d normal = sphereCenter.subtract(ringCenter).normalize();

		// 6. 构建局部基向量（使圆环平面垂直于法线）
		Vec3d worldUp = new Vec3d(0, 1, 0);
		Vec3d localRight = worldUp.crossProduct(normal).normalize();
		if (localRight.lengthSquared() < 1e-6) { // 法线平行于世界Y轴时
			localRight = new Vec3d(1, 0, 0);
		}
		Vec3d localUp = normal.crossProduct(localRight).normalize();

		// 7. 自转角度
		double angleRad = Math.toRadians((tick / 20.0) * config.rotationSpeed);

		// 8. 生成粒子
		for (int i = 0; i < config.particleCount; i++) {
			double offsetAngle = 2 * Math.PI * i / config.particleCount;
			double theta = offsetAngle + angleRad;
			double r = config.radius * Math.abs(Math.cos(3 * theta)); // 三叶草半径
			double xLocal = r * Math.cos(theta);
			double yLocal = r * Math.sin(theta);
			// 世界坐标 = 环中心 + xLocal*localRight + yLocal*localUp
			double x = ringCenter.x + xLocal * localRight.x + yLocal * localUp.x;
			double y = ringCenter.y + xLocal * localRight.y + yLocal * localUp.y;
			double z = ringCenter.z + xLocal * localRight.z + yLocal * localUp.z;

			world.spawnParticles(config.particleEffect, x, y, z, 1, 0, 0, 0, 0.0);
		}
	}
}