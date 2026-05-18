package com.shushuwonie.clairvoyance.network.gazeguidance;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record MarkCountPacket(int count) implements CustomPayload {
    public static final Id<MarkCountPacket> ID = new Id<>(Identifier.of("clairvoyance", "mark_count"));
    public static final PacketCodec<RegistryByteBuf, MarkCountPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, MarkCountPacket::count,
            MarkCountPacket::new
    );
    private static boolean registered = false;

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        if (!registered) {
            net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(ID, CODEC);
            registered = true;
        }
    }
}