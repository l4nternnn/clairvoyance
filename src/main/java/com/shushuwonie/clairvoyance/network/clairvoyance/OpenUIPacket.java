package com.shushuwonie.clairvoyance.network.clairvoyance;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenUIPacket() implements CustomPayload {
    public static final Id<OpenUIPacket> ID = new Id<>(Identifier.of("clairvoyance", "open_ui"));
    public static final PacketCodec<RegistryByteBuf, OpenUIPacket> CODEC = PacketCodec.unit(new OpenUIPacket());


    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static boolean registered = false;

    public static void register() {
        if (!registered) {
            net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(ID, CODEC);
            registered = true;
        }
    }
}