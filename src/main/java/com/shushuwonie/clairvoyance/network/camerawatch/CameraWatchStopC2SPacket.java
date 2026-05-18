package com.shushuwonie.clairvoyance.network.camerawatch;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CameraWatchStopC2SPacket() implements CustomPayload {
    public static final Id<CameraWatchStopC2SPacket> ID =
            new Id<>(Identifier.of("clairvoyance", "camera_watch_stop"));
    public static final PacketCodec<PacketByteBuf, CameraWatchStopC2SPacket> CODEC =
            PacketCodec.unit(new CameraWatchStopC2SPacket());


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