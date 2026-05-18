package com.shushuwonie.clairvoyance.network.camerawatch;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public record CameraUpdateS2CPacket(Vec3d pos, float yaw, float pitch) implements CustomPayload {
    public static final Id<CameraUpdateS2CPacket> ID = new Id<>(Identifier.of("clairvoyance", "camera_update"));
    public static final PacketCodec<PacketByteBuf, CameraUpdateS2CPacket> CODEC = PacketCodec.tuple(
            Vec3d.PACKET_CODEC, CameraUpdateS2CPacket::pos,
            PacketCodecs.FLOAT, CameraUpdateS2CPacket::yaw,
            PacketCodecs.FLOAT, CameraUpdateS2CPacket::pitch,
            CameraUpdateS2CPacket::new
    );


    private static boolean registered = false;
    public static void register() {
        if (!registered) {
            net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(ID, CODEC);
            registered = true;
        }
    }
    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}