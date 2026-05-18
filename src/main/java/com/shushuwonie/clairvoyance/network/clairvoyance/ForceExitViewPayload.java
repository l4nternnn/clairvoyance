package com.shushuwonie.clairvoyance.network.clairvoyance;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ForceExitViewPayload() implements CustomPayload {
    public static final Id<ForceExitViewPayload> ID = new Id<>(Identifier.of("clairvoyance", "force_exit_view"));
    public static final PacketCodec<RegistryByteBuf, ForceExitViewPayload> CODEC = PacketCodec.unit(new ForceExitViewPayload());


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