package com.shushuwonie.clairvoyance.network.gazeguidance;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record MagicPacket(int entityId) implements CustomPayload {
    public static final Id<MagicPacket> ID = new Id<>(Identifier.of("clairvoyance", "magic"));
    public static final PacketCodec<RegistryByteBuf, MagicPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, MagicPacket::entityId,
            MagicPacket::new
    );

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