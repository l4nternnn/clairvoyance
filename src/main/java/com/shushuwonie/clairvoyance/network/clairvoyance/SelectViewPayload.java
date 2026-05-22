package com.shushuwonie.clairvoyance.network.clairvoyance;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record SelectViewPayload(UUID entityUuid) implements CustomPayload {
    public static final Id<SelectViewPayload> ID = new Id<>(Identifier.of("clairvoyance", "select_view"));
    public static final PacketCodec<PacketByteBuf, SelectViewPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING.xmap(UUID::fromString, UUID::toString), SelectViewPayload::entityUuid,
            SelectViewPayload::new
    );

    private static boolean registered = false;
    public static void register() {
        if (!registered) {
            registered = true;
            PayloadTypeRegistry.playC2S().register(ID, CODEC);
            PayloadTypeRegistry.playS2C().register(ID, CODEC);
        }
    }
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}