package com.shushuwonie.client.gazeguidance;

import com.shushuwonie.clairvoyance.network.gazeguidance.RightClickActionPacket;
import com.shushuwonie.clairvoyance.item.gazeguidance.ModItems;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class GazeguidanceClient {
	private static KeyBinding stageConfigKey;
	private static boolean lastRightClickState = false;
	private static int currentStrength = 0;
	private static double currentEnergy = 0;
	private static double maxEnergy = 100;
	private static int currentMarkCount = 0;
	private static int currentMaxMarks = 0;

	// 供外部调用的设置方法（由网络接收器调用）
	public static void setEnergy(double current, double max) {
		currentEnergy = current;
		maxEnergy = max;
	}

	public static void setMarkCount(int count) {
		currentMarkCount = count;
	}

	public static void setStrength(int stage, int maxMarks) {
		currentMaxMarks = maxMarks;
		if (stage != currentStrength) {
			currentStrength = stage;
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.player != null && client.player.getMainHandStack().getItem() == ModItems.MAGIC_STICK) {
				showStageToast(stage, currentMarkCount, maxMarks);
			}
		}
	}

	private static void showStageToast(int stage, int markCount, int maxMarks) {
		MinecraftClient client = MinecraftClient.getInstance();
		SystemToast.show(client.getToastManager(), SystemToast.Type.NARRATOR_TOGGLE,
				Text.literal("§6诱导"),
				Text.literal("阶段 §a" + stage)
		);
	}

	public static void initialize() {
		// 所有接收器已移至 ClairvoyanceClient，此处不再注册
		// 只保留业务逻辑所需的初始化（如按键绑定）
		stageConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.gazeguidance.stage_config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_U, "category.gazeguidance"));

		// Tick 事件处理（右键长按检测）
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) return;
			// 右键长按检测
			boolean rightPressed = client.options.useKey.isPressed();
			if (rightPressed != lastRightClickState) {
				lastRightClickState = rightPressed;
				ItemStack stack = client.player.getMainHandStack();
				if (stack.getItem() == ModItems.MAGIC_STICK) {
					ClientPlayNetworking.send(new RightClickActionPacket(rightPressed));
				}
			}
			// K 键打开配置界面（仅创造模式）
			if (stageConfigKey.wasPressed() && client.player.isCreative()) {
				// 注意：StageConfigScreen 可能需要从旧包导入，但此处暂时注释
				// client.setScreen(new StageConfigScreen(null));
				client.player.sendMessage(Text.literal("§a阶段配置界面待实现"), true);
			} else if (stageConfigKey.wasPressed()) {
				client.player.sendMessage(Text.literal("§c仅创造模式可配置"), true);
			}
		});

		// HUD 渲染（能量条）
		net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.player == null) return;
			ItemStack mainHand = client.player.getMainHandStack();
			if (mainHand.getItem() != ModItems.MAGIC_STICK) return;

			int screenWidth = drawContext.getScaledWindowWidth();
			int screenHeight = drawContext.getScaledWindowHeight();
			int barWidth = 80;
			int barHeight = 5;
			int x = (screenWidth - barWidth) / 2;
			int y = screenHeight - 49;

			drawContext.fill(x, y, x + barWidth, y + barHeight, 0xFF444444);
			int fillWidth = (int)(barWidth * (currentEnergy / maxEnergy));
			drawContext.fill(x, y, x + fillWidth, y + barHeight, 0xFF55AAFF);

			String text = String.format("%.1f/%.0f", currentEnergy, maxEnergy);
			int textX = x + barWidth + 5;
			int textY = y - 2;
			drawContext.drawText(client.textRenderer, text, textX, textY, 0xFF5555, true);
		});

		System.out.println("GazeguidanceClient: Initialized (receivers moved to ClairvoyanceClient)");
	}

	// 辅助方法：获取目标实体（保留供其他类使用）
	public static Entity getTargetEntity(MinecraftClient client, double maxRange) {
		if (client.player == null || client.world == null) return null;
		Vec3d start = client.player.getEyePos();
		Vec3d direction = client.player.getRotationVec(1.0f);
		Vec3d end = start.add(direction.multiply(maxRange));
		Box searchBox = client.player.getBoundingBox().stretch(direction.multiply(maxRange)).expand(1.0);
		Entity closest = null;
		double closestDistSq = maxRange * maxRange;
		for (Entity entity : client.world.getOtherEntities(client.player, searchBox,
				e -> e instanceof LivingEntity && e.isAlive())) {
			Vec3d hitPos = entity.getBoundingBox().raycast(start, end).orElse(null);
			if (hitPos != null) {
				double distSq = start.squaredDistanceTo(hitPos);
				if (distSq < closestDistSq) {
					closestDistSq = distSq;
					closest = entity;
				}
			}
		}
		return closest;
	}
}