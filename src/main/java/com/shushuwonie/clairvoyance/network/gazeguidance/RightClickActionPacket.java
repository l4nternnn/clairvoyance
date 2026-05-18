package com.shushuwonie.clairvoyance.network.gazeguidance;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RightClickActionPacket(boolean start) implements CustomPayload {
    public static final Id<RightClickActionPacket> ID = new Id<>(Identifier.of("clairvoyance", "rightclick"));
    public static final PacketCodec<RegistryByteBuf, RightClickActionPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOLEAN, RightClickActionPacket::start,
            RightClickActionPacket::new
    );
    private static boolean registered = false;

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        if (!registered) {
            net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(ID, CODEC);
            registered = true;
        }
    }
}