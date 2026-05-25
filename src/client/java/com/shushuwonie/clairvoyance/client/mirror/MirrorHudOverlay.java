package com.shushuwonie.clairvoyance.client.mirror;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class MirrorHudOverlay {
	private static final int VIEWPORT_WIDTH = 320;
	private static final int VIEWPORT_HEIGHT = 180;
	private static final int PADDING = 8;

	public static void register() {
		HudRenderCallback.EVENT.register((context, tickCounter) -> {
			if (!MirrorClientManager.isActive()) return;

			MinecraftClient client = MinecraftClient.getInstance();
			if (client.player == null) return;

			// Blit mirror framebuffers to main framebuffer
			MirrorViewportRenderer.blitToMainFramebuffer();

			// Draw borders and labels
			int scaleFactor = (int) client.getWindow().getScaleFactor();
			int vpW = Math.ceilDiv(VIEWPORT_WIDTH, scaleFactor);
			int vpH = Math.ceilDiv(VIEWPORT_HEIGHT, scaleFactor);
			int sw = client.getWindow().getScaledWidth();

			int y = PADDING;
			for (int slot = 0; slot < 2; slot++) {
				MirrorClientManager.CameraData data = MirrorClientManager.getSlot(slot);
				if (!data.active()) continue;

				int x = sw - vpW - PADDING;

				// Border
				int borderColor = slot == 0 ? 0xFFFF55FF : 0xFF55FFFF;
				context.fill(x - 1, y - 1, x + vpW + 1, y + vpH + 1, borderColor);

				// Background bar for label
				context.fill(x, y, x + 40, y + 12, 0xAA000000);

				// Label
				String label = slot == 0 ? "镜1" : "镜2";
				int labelColor = slot == 0 ? 0xFFFF55FF : 0xFF55FFFF;
				context.drawText(client.textRenderer, Text.literal(label),
					x + 4, y + 2, labelColor, false);

				y += vpH + PADDING;
			}
		});
	}
}
