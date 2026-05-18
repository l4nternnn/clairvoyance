package com.shushuwonie.clairvoyance.network.camerawatch;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CameraWatchUnbindS2CPacket() implements CustomPayload {
    public static final Id<CameraWatchUnbindS2CPacket> ID =
            new Id<>(Identifier.of("clairvoyance", "camera_watch_unbind"));
    public static final PacketCodec<PacketByteBuf, CameraWatchUnbindS2CPacket> CODEC =
            PacketCodec.unit(new CameraWatchUnbindS2CPacket());

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