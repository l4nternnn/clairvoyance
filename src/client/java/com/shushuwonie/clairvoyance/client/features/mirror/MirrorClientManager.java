package com.shushuwonie.clairvoyance.client.features.mirror;

import com.shushuwonie.clairvoyance.network.mirror.MirrorStateS2CPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public class MirrorClientManager {
	private static boolean viewportActive = false;
	private static final CameraData[] slots = new CameraData[2];
	private static Vec3d origin = null; // player position when viewport was first enabled

	static {
		slots[0] = new CameraData(false, Vec3d.ZERO);
		slots[1] = new CameraData(false, Vec3d.ZERO);
	}

	public record CameraData(boolean active, Vec3d pos) {}

	public static void onStatePacket(MirrorStateS2CPacket packet) {
		boolean wasActive = viewportActive;
		viewportActive = packet.viewportActive();

		// Capture origin on first enable
		if (viewportActive && !wasActive && origin == null) {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.player != null) {
				origin = client.player.getPos();
			}
		}

		slots[0] = new CameraData(packet.slot1Active(), packet.getPos1() != null ? packet.getPos1() : Vec3d.ZERO);
		slots[1] = new CameraData(packet.slot2Active(), packet.getPos2() != null ? packet.getPos2() : Vec3d.ZERO);
	}

	/**
	 * The mirror viewport camera sits at: saved set point + player's displacement from origin.
	 * origin = the player's position when the viewport was first enabled.
	 * As you move, the viewport tracks with you, staying in the same relative position.
	 */
	public static Vec3d getOriginOffset() {
		if (origin == null) return Vec3d.ZERO;
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) return Vec3d.ZERO;
		return client.player.getPos().subtract(origin);
	}

	public static boolean isActive() {
		return viewportActive;
	}

	public static CameraData getSlot(int index) {
		if (index < 0 || index > 1) return new CameraData(false, Vec3d.ZERO);
		return slots[index];
	}

	public static void reset() {
		viewportActive = false;
		origin = null;
		slots[0] = new CameraData(false, Vec3d.ZERO);
		slots[1] = new CameraData(false, Vec3d.ZERO);
	}
}
