package com.shushuwonie.clairvoyance.network.clairvoyance;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RequestGlobalConfigC2SPacket() implements CustomPayload {
    public static final Id<RequestGlobalConfigC2SPacket> ID = new Id<>(Identifier.of("clairvoyance", "request_global_config"));
    public static final PacketCodec<RegistryByteBuf, RequestGlobalConfigC2SPacket> CODEC = PacketCodec.unit(new RequestGlobalConfigC2SPacket());



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