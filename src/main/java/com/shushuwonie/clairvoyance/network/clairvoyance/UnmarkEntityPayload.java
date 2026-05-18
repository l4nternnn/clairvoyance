package com.shushuwonie.clairvoyance.network.clairvoyance;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record UnmarkEntityPayload(UUID entityUuid) implements CustomPayload {
    public static final Id<UnmarkEntityPayload> ID = new Id<>(Identifier.of("clairvoyance", "unmark_entity"));
    public static final PacketCodec<RegistryByteBuf, UnmarkEntityPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING.xmap(UUID::fromString, UUID::toString), UnmarkEntityPayload::entityUuid,
            UnmarkEntityPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}