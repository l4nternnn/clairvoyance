package com.shushuwonie.clairvoyance.network.clairvoyance;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ExitViewPayload() implements CustomPayload {
    public static final Id<ExitViewPayload> ID = new Id<>(Identifier.of("clairvoyance", "exit_view"));
    public static final PacketCodec<RegistryByteBuf, ExitViewPayload> CODEC = PacketCodec.unit(new ExitViewPayload());



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