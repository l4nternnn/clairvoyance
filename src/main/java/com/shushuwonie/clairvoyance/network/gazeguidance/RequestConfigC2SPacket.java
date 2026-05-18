package com.shushuwonie.clairvoyance.network.gazeguidance;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RequestConfigC2SPacket() implements CustomPayload {
    public static final Id<RequestConfigC2SPacket> ID = new Id<>(Identifier.of("clairvoyance", "request_config"));
    public static final PacketCodec<RegistryByteBuf, RequestConfigC2SPacket> CODEC = PacketCodec.unit(new RequestConfigC2SPacket());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static boolean registered = false;

    public static void register() {
        if (!registered) {

            PayloadTypeRegistry.playC2S().register(ID, CODEC);
            registered = true;
        }
    }
}