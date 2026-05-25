package com.shushuwonie.clairvoyance.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.shushuwonie.clairvoyance.network.mirror.MirrorStateS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MirrorCommand {
	public static final Map<UUID, Vec3d[]> PLAYER_MIRRORS = new ConcurrentHashMap<>();
	public static final Map<UUID, Boolean> VIEWPORT_ACTIVE = new ConcurrentHashMap<>();

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
			CommandRegistryAccess registryAccess,
			CommandManager.RegistrationEnvironment environment) {
		dispatcher.register(CommandManager.literal("mirror")
			.requires(source -> source.hasPermissionLevel(2))
			.then(CommandManager.literal("set")
				.then(CommandManager.argument("slot", IntegerArgumentType.integer(1, 2))
					.executes(ctx -> executeSet(ctx.getSource(),
						IntegerArgumentType.getInteger(ctx, "slot"),
						null))
					.then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
						.executes(ctx -> executeSet(ctx.getSource(),
							IntegerArgumentType.getInteger(ctx, "slot"),
							Vec3ArgumentType.getVec3(ctx, "pos"))))))
			.then(CommandManager.literal("remove")
				.then(CommandManager.argument("slot", IntegerArgumentType.integer(1, 2))
					.executes(ctx -> executeRemove(ctx.getSource(),
						IntegerArgumentType.getInteger(ctx, "slot")))))
			.then(CommandManager.literal("list")
				.executes(ctx -> executeList(ctx.getSource())))
			.then(CommandManager.literal("clear")
				.executes(ctx -> executeClear(ctx.getSource())))
		);
	}

	private static int executeSet(ServerCommandSource source, int slot, Vec3d pos) {
		ServerPlayerEntity player = source.getPlayer();
		if (player == null) return 0;

		Vec3d targetPos = (pos != null) ? pos : player.getPos();

		PLAYER_MIRRORS.compute(player.getUuid(), (uuid, existing) -> {
			if (existing == null) {
				Vec3d[] arr = new Vec3d[2];
				arr[slot - 1] = targetPos;
				return arr;
			}
			existing[slot - 1] = targetPos;
			return existing;
		});

		source.sendMessage(Text.literal("§a镜位 " + slot + " 已设置为: " +
			String.format("%.1f, %.1f, %.1f", targetPos.x, targetPos.y, targetPos.z)));

		syncToClient(player);
		return 1;
	}

	private static int executeRemove(ServerCommandSource source, int slot) {
		ServerPlayerEntity player = source.getPlayer();
		if (player == null) return 0;

		Vec3d[] mirrors = PLAYER_MIRRORS.get(player.getUuid());
		if (mirrors != null) {
			mirrors[slot - 1] = null;
		}

		source.sendMessage(Text.literal("§a镜位 " + slot + " 已清除"));
		syncToClient(player);
		return 1;
	}

	private static int executeList(ServerCommandSource source) {
		ServerPlayerEntity player = source.getPlayer();
		if (player == null) return 0;

		Vec3d[] mirrors = PLAYER_MIRRORS.get(player.getUuid());
		source.sendMessage(Text.literal("§b=== 镜位列表 ==="));

		boolean anyActive = false;
		for (int i = 0; i < 2; i++) {
			Vec3d p = (mirrors != null) ? mirrors[i] : null;
			if (p != null) {
				anyActive = true;
				source.sendMessage(Text.literal("§a镜位 " + (i + 1) + ": §f" +
					String.format("%.1f, %.1f, %.1f", p.x, p.y, p.z)));
			} else {
				source.sendMessage(Text.literal("§7镜位 " + (i + 1) + ": §7未设置"));
			}
		}

		boolean active = VIEWPORT_ACTIVE.getOrDefault(player.getUuid(), false);
		source.sendMessage(Text.literal("§6镜像视图: " + (active ? "§a开启" : "§7关闭")));

		if (!anyActive) {
			source.sendMessage(Text.literal("§7使用 /mirror set <1|2> [x y z] 设置镜位"));
		}
		return 1;
	}

	private static int executeClear(ServerCommandSource source) {
		ServerPlayerEntity player = source.getPlayer();
		if (player == null) return 0;

		PLAYER_MIRRORS.remove(player.getUuid());
		source.sendMessage(Text.literal("§a所有镜位已清除"));
		syncToClient(player);
		return 1;
	}

	public static void toggleViewport(ServerPlayerEntity player) {
		UUID uuid = player.getUuid();
		Vec3d[] mirrors = PLAYER_MIRRORS.get(uuid);

		boolean hasAny = false;
		if (mirrors != null) {
			for (Vec3d p : mirrors) {
				if (p != null) { hasAny = true; break; }
			}
		}

		if (!hasAny) {
			player.sendMessage(Text.literal("§c请先使用 /mirror set <1|2> 设置至少一个镜位"), false);
			return;
		}

		boolean current = VIEWPORT_ACTIVE.getOrDefault(uuid, false);
		VIEWPORT_ACTIVE.put(uuid, !current);
		player.sendMessage(Text.literal(current ? "§7镜像视图已关闭" : "§a镜像视图已开启"), true);
		syncToClient(player);
	}

	private static void syncToClient(ServerPlayerEntity player) {
		UUID uuid = player.getUuid();
		Vec3d[] mirrors = PLAYER_MIRRORS.get(uuid);

		Vec3d p1 = (mirrors != null && mirrors[0] != null) ? mirrors[0] : null;
		Vec3d p2 = (mirrors != null && mirrors[1] != null) ? mirrors[1] : null;
		boolean active = VIEWPORT_ACTIVE.getOrDefault(uuid, false);

		ServerPlayNetworking.send(player, new MirrorStateS2CPacket(
			p1 != null, p1 != null ? p1.x : 0, p1 != null ? p1.y : 0, p1 != null ? p1.z : 0,
			p2 != null, p2 != null ? p2.x : 0, p2 != null ? p2.y : 0, p2 != null ? p2.z : 0,
			active
		));
	}
}
