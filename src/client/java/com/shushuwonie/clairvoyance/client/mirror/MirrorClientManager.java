package com.shushuwonie.clairvoyance.client.mirror;

import com.shushuwonie.clairvoyance.network.mirror.MirrorStateS2CPacket;
import net.minecraft.util.math.Vec3d;

public class MirrorClientManager {
	private static boolean viewportActive = false;
	private static final CameraData[] slots = new CameraData[2];

	static {
		slots[0] = new CameraData(false, Vec3d.ZERO);
		slots[1] = new CameraData(false, Vec3d.ZERO);
	}

	public record CameraData(boolean active, Vec3d pos) {}

	public static void onStatePacket(MirrorStateS2CPacket packet) {
		viewportActive = packet.viewportActive();
		slots[0] = new CameraData(packet.slot1Active(), packet.getPos1() != null ? packet.getPos1() : Vec3d.ZERO);
		slots[1] = new CameraData(packet.slot2Active(), packet.getPos2() != null ? packet.getPos2() : Vec3d.ZERO);
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
		slots[0] = new CameraData(false, Vec3d.ZERO);
		slots[1] = new CameraData(false, Vec3d.ZERO);
	}
}
