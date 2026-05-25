package com.shushuwonie.clairvoyance.network.mirror;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record MirrorToggleC2SPacket() implements CustomPayload {
	public static final Id<MirrorToggleC2SPacket> ID = new Id<>(Identifier.of("clairvoyance", "mirror_toggle"));
	public static final PacketCodec<PacketByteBuf, MirrorToggleC2SPacket> CODEC = PacketCodec.unit(new MirrorToggleC2SPacket());

	private static boolean registered = false;

	public static void register() {
		if (!registered) {
			PayloadTypeRegistry.playC2S().register(ID, CODEC);
			registered = true;
		}
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
