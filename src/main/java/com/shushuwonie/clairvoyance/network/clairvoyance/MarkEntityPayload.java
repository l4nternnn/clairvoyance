package com.shushuwonie.clairvoyance.network.clairvoyance;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record MarkEntityPayload(int entityId) implements CustomPayload {
    public static final Id<MarkEntityPayload> ID = new Id<>(Identifier.of("clairvoyance", "mark_entity"));
    public static final PacketCodec<RegistryByteBuf, MarkEntityPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, MarkEntityPayload::entityId,
            MarkEntityPayload::new
    );


    private static boolean registered = false;
    public static void register() {
        if (!registered) {
            net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(ID, CODEC);
            registered = true;
        }
    }
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}