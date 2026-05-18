// 文件：SetCameraEntityS2CPacket.java
package com.shushuwonie.clairvoyance.network.clairvoyance;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record SetCameraEntityS2CPacket(UUID cameraEntityUuid) implements CustomPayload {
    public static final Id<SetCameraEntityS2CPacket> ID = new Id<>(Identifier.of("clairvoyance", "set_camera"));
    public static final PacketCodec<RegistryByteBuf, SetCameraEntityS2CPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING.xmap(UUID::fromString, UUID::toString), SetCameraEntityS2CPacket::cameraEntityUuid,
            SetCameraEntityS2CPacket::new
    );

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