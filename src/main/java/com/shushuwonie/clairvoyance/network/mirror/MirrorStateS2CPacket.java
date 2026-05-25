package com.shushuwonie.clairvoyance.network.mirror;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public record MirrorStateS2CPacket(
	boolean slot1Active, double x1, double y1, double z1,
	boolean slot2Active, double x2, double y2, double z2,
	boolean viewportActive
) implements CustomPayload {
	public static final Id<MirrorStateS2CPacket> ID = new Id<>(Identifier.of("clairvoyance", "mirror_state"));
	public static final PacketCodec<PacketByteBuf, MirrorStateS2CPacket> CODEC = PacketCodec.tuple(
		PacketCodecs.BOOLEAN, MirrorStateS2CPacket::slot1Active,
		PacketCodecs.DOUBLE, MirrorStateS2CPacket::x1,
		PacketCodecs.DOUBLE, MirrorStateS2CPacket::y1,
		PacketCodecs.DOUBLE, MirrorStateS2CPacket::z1,
		PacketCodecs.BOOLEAN, MirrorStateS2CPacket::slot2Active,
		PacketCodecs.DOUBLE, MirrorStateS2CPacket::x2,
		PacketCodecs.DOUBLE, MirrorStateS2CPacket::y2,
		PacketCodecs.DOUBLE, MirrorStateS2CPacket::z2,
		PacketCodecs.BOOLEAN, MirrorStateS2CPacket::viewportActive,
		MirrorStateS2CPacket::new
	);

	private static boolean registered = false;

	public static void register() {
		if (!registered) {
			PayloadTypeRegistry.playS2C().register(ID, CODEC);
			registered = true;
		}
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}

	public Vec3d getPos1() {
		return slot1Active ? new Vec3d(x1, y1, z1) : null;
	}

	public Vec3d getPos2() {
		return slot2Active ? new Vec3d(x2, y2, z2) : null;
	}
}
