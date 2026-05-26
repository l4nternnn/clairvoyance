package com.shushuwonie.clairvoyance.features.carryentity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CarryManager {
	// Carrier -> carried entity data
	public static final Map<ServerPlayerEntity, CarriedEntityData> CARRIED_ENTITIES = new ConcurrentHashMap<>();
	// Carried entity -> carrier (for quick struggle lookup)
	public static final Map<Entity, ServerPlayerEntity> CARRIED_BY = new ConcurrentHashMap<>();
	// Carry cooldown: entity -> cooldown end timestamp
	public static final Map<Entity, Long> CARRIED_COOLDOWN = new ConcurrentHashMap<>();
	// Struggle counter: carried entity -> current sneak count
	public static final Map<Entity, Integer> STRUGGLE_COUNTER = new ConcurrentHashMap<>();
	// XP drain: carrier -> accumulated ticks
	public static final Map<ServerPlayerEntity, Integer> CARRY_XP_TICK_COUNTER = new ConcurrentHashMap<>();
	public static int CARRY_XP_DRAIN_RATE = 1;

	private static final Set<UUID> FALL_DAMAGE_PROCESSING = ConcurrentHashMap.newKeySet();

	public static class CarriedEntityData {
		public final Entity entity;
		public final boolean originalFlying;
		public final boolean originalAllowFlying;
		public final boolean originalInvulnerable;

		public CarriedEntityData(Entity entity) {
			this(entity, false, false, false);
		}

		public CarriedEntityData(Entity entity, boolean originalFlying, boolean originalAllowFlying, boolean originalInvulnerable) {
			this.entity = entity;
			this.originalFlying = originalFlying;
			this.originalAllowFlying = originalAllowFlying;
			this.originalInvulnerable = originalInvulnerable;
		}
	}

	public static int getStruggleThreshold(ServerPlayerEntity carrier, Entity carried) {
		if (!carrier.getCommandTags().contains("qiangzhiai")) return 1;
		if (carried instanceof ServerPlayerEntity carriedPlayer) {
			if (carriedPlayer.getCommandTags().contains("qiangzhiai")) return 1;
			if (carriedPlayer.getCommandTags().contains("strong_power")) return 100;
		}
		return 300;
	}

	public static void releaseCarried(ServerPlayerEntity carrier, Entity carried) {
		CarriedEntityData data = CARRIED_ENTITIES.get(carrier);
		CARRIED_ENTITIES.remove(carrier);
		CARRIED_BY.remove(carried);
		carried.setNoGravity(false);
		carried.setInvulnerable(false);
		carried.setSilent(false);
		if (carried instanceof MobEntity mob) {
			mob.setAiDisabled(false);
		}
		if (carried instanceof ServerPlayerEntity carriedPlayer) {
			carriedPlayer.getAbilities().flying = data != null ? data.originalFlying : false;
			carriedPlayer.getAbilities().allowFlying = data != null ? data.originalAllowFlying : false;
			carriedPlayer.getAbilities().invulnerable = data != null ? data.originalInvulnerable : false;
			carriedPlayer.sendAbilitiesUpdate();
		}
		STRUGGLE_COUNTER.remove(carried);
		CARRY_XP_TICK_COUNTER.remove(carrier);
		CARRIED_COOLDOWN.put(carried, System.currentTimeMillis() + 5000);
		Vec3d lookVec = carrier.getRotationVec(1.0f);
		Vec3d pos = carrier.getEyePos().add(lookVec.multiply(0.5));
		carried.refreshPositionAndAngles(pos.x, pos.y, pos.z, carrier.getYaw(), carrier.getPitch());
		if (carried instanceof ServerPlayerEntity carriedPlayer) {
			carriedPlayer.networkHandler.requestTeleport(pos.x, pos.y, pos.z, carrier.getYaw(), carrier.getPitch());
		}
	}

	public static boolean handleFallDamage(ServerPlayerEntity carrier, Entity carried, float damageTaken) {
		if (FALL_DAMAGE_PROCESSING.contains(carrier.getUuid())) return false;
		FALL_DAMAGE_PROCESSING.add(carrier.getUuid());
		try {
			float carrierExtra = damageTaken * 0.2F;
			float carriedDamage = damageTaken * 0.6F;
			float newCarrierHealth = carrier.getHealth() - carrierExtra;
			if (newCarrierHealth < 0) newCarrierHealth = 0;
			carrier.setHealth(newCarrierHealth);
			if (carried.isAlive()) {
				float newCarriedHealth = carried instanceof LivingEntity ? ((LivingEntity) carried).getHealth() - carriedDamage : 0;
				if (newCarriedHealth < 0) newCarriedHealth = 0;
				if (carried instanceof LivingEntity) ((LivingEntity) carried).setHealth(newCarriedHealth);
			}
			return true;
		} finally {
			FALL_DAMAGE_PROCESSING.remove(carrier.getUuid());
		}
	}

	public static void tickCarried(ServerPlayerEntity carrier, CarriedEntityData data) {
		Entity carried = data.entity;
		if (!carried.isAlive()) {
			CARRIED_ENTITIES.remove(carrier);
			CARRIED_BY.remove(carried);
			if (carried instanceof ServerPlayerEntity carriedPlayer) {
				carriedPlayer.getAbilities().flying = data.originalFlying;
				carriedPlayer.getAbilities().allowFlying = data.originalAllowFlying;
				carriedPlayer.getAbilities().invulnerable = data.originalInvulnerable;
				carriedPlayer.sendAbilitiesUpdate();
			}
			return;
		}

		float yaw = carrier.getYaw();
		double rad = Math.toRadians(yaw);
		double forwardX = -Math.sin(rad);
		double forwardZ = Math.cos(rad);
		Vec3d horizontalForward = new Vec3d(forwardX, 0, forwardZ).normalize();
		Vec3d targetPos = carrier.getPos().add(horizontalForward.multiply(1.2)).add(0, 0.5, 0);

		carried.setPosition(targetPos.x, targetPos.y, targetPos.z);
		carried.setVelocity(Vec3d.ZERO);
		carried.setNoGravity(true);
		if (carried instanceof MobEntity mob) {
			mob.setAiDisabled(true);
			mob.getNavigation().stop();
		}

		if (carried instanceof ServerPlayerEntity carriedPlayer) {
			if (carriedPlayer.isSneaking()) {
				int threshold = getStruggleThreshold(carrier, carried);
				if (threshold <= 1) {
					releaseCarried(carrier, carriedPlayer);
					carrier.sendMessage(Text.literal("§c" + carriedPlayer.getName().getString() + " 挣脱了"), false);
					carriedPlayer.sendMessage(Text.literal("§c你按潜行挣脱了怀抱"), false);
					STRUGGLE_COUNTER.remove(carried);
				} else {
					int count = STRUGGLE_COUNTER.merge(carried, 1, Integer::sum);
					carriedPlayer.sendMessage(Text.literal("§e挣扎进度: " + count + "/" + threshold), true);
					if (count >= threshold) {
						releaseCarried(carrier, carriedPlayer);
						carrier.sendMessage(Text.literal("§c" + carriedPlayer.getName().getString() + " 挣脱了"), false);
						carriedPlayer.sendMessage(Text.literal("§c你终于挣脱了怀抱"), false);
						STRUGGLE_COUNTER.remove(carried);
					}
				}
			} else {
				if (STRUGGLE_COUNTER.containsKey(carried)) {
					STRUGGLE_COUNTER.remove(carried);
					carriedPlayer.sendMessage(Text.literal("§7挣扎中断"), false);
				}
				carriedPlayer.networkHandler.requestTeleport(
						targetPos.x, targetPos.y, targetPos.z,
						carriedPlayer.getYaw(), carriedPlayer.getPitch()
				);
			}
		}

		// XP drain every 20 ticks
		if (carried instanceof LivingEntity && !carrier.isCreative() && !carrier.getCommandTags().contains("kebao")) {
			int tickCount = CARRY_XP_TICK_COUNTER.merge(carrier, 1, (oldVal, v) -> oldVal + 1);
			if (tickCount >= 20) {
				CARRY_XP_TICK_COUNTER.put(carrier, 0);
				if (carrier.experienceLevel > 0) {
					carrier.addExperienceLevels(-CARRY_XP_DRAIN_RATE);
					if (carrier.experienceLevel < 0) carrier.experienceLevel = 0;
				} else {
					releaseCarried(carrier, carried);
					carrier.sendMessage(Text.literal("§c体力耗尽，无法继续抱起"), false);
					if (carried instanceof ServerPlayerEntity cp) {
						cp.sendMessage(Text.literal("§c" + carrier.getName().getString() + "体力耗尽放下了你"), false);
					}
				}
			}
		}
	}

	public static void cleanupForDisconnect(ServerPlayerEntity player) {
		CarriedEntityData carriedData = CARRIED_ENTITIES.remove(player);
		if (carriedData != null) {
			Entity carried = carriedData.entity;
			CARRIED_BY.remove(carried);
			carried.setNoGravity(false);
			carried.setInvulnerable(false);
			carried.setSilent(false);
			if (carried instanceof ServerPlayerEntity carriedPlayer) {
				carriedPlayer.getAbilities().flying = carriedData.originalFlying;
				carriedPlayer.getAbilities().allowFlying = carriedData.originalAllowFlying;
				carriedPlayer.getAbilities().invulnerable = carriedData.originalInvulnerable;
				carriedPlayer.sendAbilitiesUpdate();
			}
			if (carried instanceof MobEntity mob) {
				mob.setAiDisabled(false);
			}
		}
		ServerPlayerEntity theirCarrier = CARRIED_BY.remove(player);
		if (theirCarrier != null) {
			CarriedEntityData removedData = CARRIED_ENTITIES.remove(theirCarrier);
			if (removedData != null) {
				player.getAbilities().flying = removedData.originalFlying;
				player.getAbilities().allowFlying = removedData.originalAllowFlying;
				player.getAbilities().invulnerable = removedData.originalInvulnerable;
				player.sendAbilitiesUpdate();
			}
		}
		CARRIED_COOLDOWN.remove(player);
		STRUGGLE_COUNTER.remove(player);
		CARRY_XP_TICK_COUNTER.remove(player);
	}

	public static void cleanupCooldowns() {
		long now = System.currentTimeMillis();
		CARRIED_COOLDOWN.values().removeIf(expiry -> expiry <= now);
	}
}
