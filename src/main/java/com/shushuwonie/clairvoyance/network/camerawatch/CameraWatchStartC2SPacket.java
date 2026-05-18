package com.shushuwonie.clairvoyance.network.camerawatch;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record CameraWatchStartC2SPacket(UUID targetUuid) implements CustomPayload {
    public static final Id<CameraWatchStartC2SPacket> ID =
            new Id<>(Identifier.of("clairvoyance", "camera_watch_start"));
    public static final PacketCodec<PacketByteBuf, CameraWatchStartC2SPacket> CODEC =
            PacketCodec.tuple(Uuids.PACKET_CODEC, CameraWatchStartC2SPacket::targetUuid, CameraWatchStartC2SPacket::new);


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