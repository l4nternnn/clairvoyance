package com.shushuwonie.clairvoyance.client.features.mirror;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.shushuwonie.clairvoyance.client.mixin.CameraAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.concurrent.atomic.AtomicBoolean;

public class MirrorViewportRenderer {
	private static final int VIEWPORT_WIDTH = 1920;
	private static final int VIEWPORT_HEIGHT = 1080;
	private static final int PADDING = 8;

	private static final AtomicBoolean renderingMirror = new AtomicBoolean(false);
	private static final SimpleFramebuffer[] fbos = new SimpleFramebuffer[2];
	private static boolean hasRenderedMirrors = false;

	public static void renderViewports(RenderTickCounter tickCounter, GpuBufferSlice fogBuffer, Vector4f fogColor, Camera mainCamera) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null || client.player == null) return;
		if (!MirrorClientManager.isActive()) return;
		if (renderingMirror.getAndSet(true)) return;

		try {
			float camYaw = mainCamera.getYaw();
			float camPitch = mainCamera.getPitch();
			Vec3d originOffset = MirrorClientManager.getOriginOffset();

			for (int slot = 0; slot < 2; slot++) {
				MirrorClientManager.CameraData data = MirrorClientManager.getSlot(slot);
				if (!data.active()) continue;



				Vec3d slotPos = data.pos().add(originOffset).add(-3.6,16,-0.4);

				SimpleFramebuffer fbo = getOrCreateFbo(slot);
				FramebufferOverride.setOverride(fbo);

				try {
					Camera mirrorCamera = new Camera();
					CameraAccessor acc = (CameraAccessor) mirrorCamera;
					acc.invokeSetPos(slotPos.x, slotPos.y, slotPos.z);
					acc.invokeSetRotation(camYaw, camPitch);

					float fovDeg = client.options.getFov().getValue().floatValue();
					float fovRad = fovDeg * (float) (Math.PI / 180.0);
					Matrix4f projMatrix = new Matrix4f().perspective(
						fovRad,
						(float) VIEWPORT_WIDTH / VIEWPORT_HEIGHT,
						0.05F,
						client.gameRenderer.getFarPlaneDistance()
					);

					float yawRad = camYaw * (float)(Math.PI / 180.0);
					float pitchRad = camPitch * (float)(Math.PI / 180.0);
					Matrix4f viewMatrix = new Matrix4f().lookAt(
						(float)slotPos.x, (float)slotPos.y, (float)slotPos.z,
						(float)(slotPos.x - Math.sin(yawRad) * Math.cos(pitchRad)),
						(float)(slotPos.y - Math.sin(pitchRad)),
						(float)(slotPos.z + Math.cos(yawRad) * Math.cos(pitchRad)),
						0, 1, 0
					);

					client.worldRenderer.render(
						ObjectAllocator.TRIVIAL,
						tickCounter,
						false,
						mirrorCamera,
						viewMatrix,
						projMatrix,
						fogBuffer,
						fogColor,
						true
					);
				} finally {
					FramebufferOverride.clearOverride();
				}
			}

			hasRenderedMirrors = true;
		} finally {
			renderingMirror.set(false);
		}
	}

	public static void blitToMainFramebuffer() {
		if (!hasRenderedMirrors) return;
		MinecraftClient client = MinecraftClient.getInstance();
		Framebuffer mainFbo = client.getFramebuffer();
		int scaleFactor = (int) client.getWindow().getScaleFactor();
		int sw = client.getWindow().getFramebufferWidth();
		int sh = client.getWindow().getFramebufferHeight();

		hasRenderedMirrors = false;

		for (int slot = 0; slot < 2; slot++) {
			MirrorClientManager.CameraData data = MirrorClientManager.getSlot(slot);
			if (!data.active()) continue;
			SimpleFramebuffer fbo = fbos[slot];
			if (fbo == null) continue;

			int x = sw - VIEWPORT_WIDTH - PADDING * scaleFactor;
			int y = PADDING * scaleFactor + slot * (VIEWPORT_HEIGHT + PADDING * scaleFactor);
			int invertedY = sh - y - VIEWPORT_HEIGHT;

			RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
				fbo.getColorAttachment(),
				mainFbo.getColorAttachment(),
				0,
				x, invertedY,
				0, 0,
				VIEWPORT_WIDTH, VIEWPORT_HEIGHT
			);
		}
	}

	private static SimpleFramebuffer getOrCreateFbo(int slot) {
		if (fbos[slot] == null) {
			fbos[slot] = new SimpleFramebuffer("mirror_" + slot, VIEWPORT_WIDTH, VIEWPORT_HEIGHT, true);
		}
		return fbos[slot];
	}

	public static void cleanup() {
		for (int i = 0; i < 2; i++) {
			if (fbos[i] != null) {
				fbos[i].delete();
				fbos[i] = null;
			}
		}
		hasRenderedMirrors = false;
	}
}
