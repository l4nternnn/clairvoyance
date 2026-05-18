package com.shushuwonie.clairvoyance.network.camerawatch;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CameraWatchBindS2CPacket(int entityId) implements CustomPayload {
    public static final Id<CameraWatchBindS2CPacket> ID =
            new Id<>(Identifier.of("clairvoyance", "camera_watch_bind"));
    public static final PacketCodec<PacketByteBuf, CameraWatchBindS2CPacket> CODEC =
            PacketCodec.tuple(PacketCodecs.INTEGER, CameraWatchBindS2CPacket::entityId, CameraWatchBindS2CPacket::new);

    private static boolean registered = false;
    public static void register() {
        if (!registered) {
            net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(ID, CODEC);
            registered = true;
        }
    }
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}