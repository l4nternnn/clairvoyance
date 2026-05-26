package com.shushuwonie.clairvoyance.features.carryentity;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.shushuwonie.clairvoyance.network.openback.CarryEntityPayload;
import com.shushuwonie.clairvoyance.network.openback.PlaceCarriedEntityPayload;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import com.shushuwonie.clairvoyance.features.block.body.BodyPartManager;

import java.util.Map;

public class CarryEvents {
	public static void register() {
		// Fall damage sharing between carrier and carried
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
			if (source.isOf(DamageTypes.FALL) && entity instanceof ServerPlayerEntity carrier) {
				CarryManager.CarriedEntityData data = CarryManager.CARRIED_ENTITIES.get(carrier);
				if (data != null) {
					CarryManager.handleFallDamage(carrier, data.entity, damageTaken);
				}
			}
		});

		// Server tick: carry position updates, struggle, XP drain
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (Map.Entry<ServerPlayerEntity, CarryManager.CarriedEntityData> entry : CarryManager.CARRIED_ENTITIES.entrySet()) {
				CarryManager.tickCarried(entry.getKey(), entry.getValue());
			}
			CarryManager.cleanupCooldowns();

			// Heaviness: kebao players holding body part items get Slowness
			for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
				if (p.getCommandTags().contains("kebao") && !p.isCreative()) {
					ItemStack held = p.getMainHandStack();
					if (!held.isEmpty() && (
						BodyPartManager.isCombinedBodyItem(held) ||
						BodyPartManager.BODY_PART_DISPLAY_ITEMS.contains(held.getItem())
					)) {
						if (!p.getCommandTags().contains("qiangzhiai") && !p.getCommandTags().contains("strong_power")) {
							p.addStatusEffect(new StatusEffectInstance(
								StatusEffects.SLOWNESS, 40, 0, false, false, true
							));
						}
					}
				}
			}
		});

		// Disconnect cleanup
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			CarryManager.cleanupForDisconnect(player);
		});

		// Carry entity request
		ServerPlayNetworking.registerGlobalReceiver(CarryEntityPayload.ID, (payload, context) -> {
			ServerPlayerEntity carrier = context.player();
			if (!carrier.isSneaking()) return;
			if (!carrier.getMainHandStack().isEmpty() || !carrier.getOffHandStack().isEmpty()) return;
			if (!carrier.isCreative() && !carrier.getCommandTags().contains("kebao")) {
				carrier.sendMessage(Text.literal("§c还无法抱起§k12§r"), false);
				return;
			}
			ServerWorld world = (ServerWorld) carrier.getWorld();
			Entity target = world.getEntityById(payload.entityId());
			if (target == null) return;

			// Check cooldown
			Long cooldownEnd = CarryManager.CARRIED_COOLDOWN.get(target);
			if (cooldownEnd != null && cooldownEnd > System.currentTimeMillis()) {
				carrier.sendMessage(Text.literal("§c哈..哈~...待会再抱吧，剩余" + ((cooldownEnd - System.currentTimeMillis()) / 1000 + 1) + "秒"), false);
				return;
			}

			CarryManager.CarriedEntityData currentData = CarryManager.CARRIED_ENTITIES.get(carrier);
			if (currentData != null && currentData.entity == target) {
				CarryManager.releaseCarried(carrier, target);
				carrier.sendMessage(Text.literal("§a放下了" + target.getName().getString()), false);
				return;
			}

			if (CarryManager.CARRIED_ENTITIES.containsKey(carrier)) {
				carrier.sendMessage(Text.literal("§c已经抱了一个，先放下她"), false);
				return;
			}
			if (CarryManager.CARRIED_BY.containsKey(target)) {
				carrier.sendMessage(Text.literal("§c§k1234§r已经被别人抱起了"), false);
				return;
			}

			boolean savedFlying = false, savedAllowFlying = false, savedInvulnerable = false;
			if (target instanceof ServerPlayerEntity carriedPlayer) {
				savedFlying = carriedPlayer.getAbilities().flying;
				savedAllowFlying = carriedPlayer.getAbilities().allowFlying;
				savedInvulnerable = carriedPlayer.getAbilities().invulnerable;
				carriedPlayer.setNoGravity(true);
				carriedPlayer.getAbilities().flying = true;
				carriedPlayer.getAbilities().allowFlying = true;
				carriedPlayer.getAbilities().invulnerable = true;
				carriedPlayer.sendAbilitiesUpdate();
				carriedPlayer.networkHandler.requestTeleport(carrier.getX(), carrier.getY(), carrier.getZ(), carrier.getYaw(), carrier.getPitch());

				if (CarryManager.CARRIED_ENTITIES.containsKey(carriedPlayer)) {
					carrier.sendMessage(Text.literal("§c§k1§r正在抱起其他存在，无法抱起"), false);
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
			target.setSilent(true);

			CarryManager.CARRIED_ENTITIES.put(carrier, new CarryManager.CarriedEntityData(target, savedFlying, savedAllowFlying, savedInvulnerable));
			CarryManager.CARRIED_BY.put(target, carrier);
			carrier.sendMessage(Text.literal("§a你抱起了 " + target.getName().getString()), false);
		});

		// Place carried entity request
		ServerPlayNetworking.registerGlobalReceiver(PlaceCarriedEntityPayload.ID, (payload, context) -> {
			ServerPlayerEntity carrier = context.player();
			CarryManager.CarriedEntityData data = CarryManager.CARRIED_ENTITIES.remove(carrier);
			if (data == null) return;
			Entity carried = data.entity;
			CarryManager.CARRIED_BY.remove(carried);
			if (carried.isAlive()) {
				carried.setNoGravity(false);
				carried.setSilent(false);
				if (carried instanceof ServerPlayerEntity carriedPlayer) {
					carriedPlayer.getAbilities().flying = data.originalFlying;
					carriedPlayer.getAbilities().allowFlying = data.originalAllowFlying;
					carriedPlayer.getAbilities().invulnerable = data.originalInvulnerable;
					carriedPlayer.sendAbilitiesUpdate();
				}
				if (carried instanceof MobEntity mob) {
					mob.setAiDisabled(false);
				}
				Vec3d lookVec = carrier.getRotationVec(1.0f);
				Vec3d pos = carrier.getEyePos().add(lookVec.multiply(1.5)).subtract(0, 0.5, 0);
				carried.refreshPositionAndAngles(pos.x, pos.y, pos.z, carrier.getYaw(), carrier.getPitch());
				carrier.sendMessage(Text.literal("§a放下了抱起的实体"), false);
			} else {
				carrier.sendMessage(Text.literal("§c实体已经死亡，无法放下"), false);
			}
		});

		// carry-xp-rate command
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("carry-xp-rate")
				.requires(source -> source.hasPermissionLevel(2))
				.then(CommandManager.argument("amount", IntegerArgumentType.integer(0, 100))
					.executes(ctx -> {
						int amount = IntegerArgumentType.getInteger(ctx, "amount");
						CarryManager.CARRY_XP_DRAIN_RATE = amount;
						ctx.getSource().sendMessage(Text.literal("§a搬运经验消耗率已设置为 " + amount));
						return 1;
					})
				)
				.executes(ctx -> {
					ctx.getSource().sendMessage(Text.literal("§e当前搬运经验消耗率: " + CarryManager.CARRY_XP_DRAIN_RATE));
					return 1;
				})
			);
		});
	}
}
