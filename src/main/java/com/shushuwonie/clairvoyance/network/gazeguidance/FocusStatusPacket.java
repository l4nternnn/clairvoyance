package com.shushuwonie.clairvoyance.network.gazeguidance;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record FocusStatusPacket(boolean active) implements CustomPayload {
    public static final Id<FocusStatusPacket> ID = new Id<>(Identifier.of("clairvoyance", "focus_status"));
    public static final PacketCodec<RegistryByteBuf, FocusStatusPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOLEAN, FocusStatusPacket::active,
            FocusStatusPacket::new
    );


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