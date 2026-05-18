package com.shushuwonie.clairvoyance.network.clairvoyance;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record EntityMarkedPayload(UUID entityUuid, String entityName) implements CustomPayload {
    public static final Id<EntityMarkedPayload> ID = new Id<>(Identifier.of("clairvoyance", "entity_marked"));
    public static final PacketCodec<RegistryByteBuf, EntityMarkedPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING.xmap(UUID::fromString, UUID::toString), EntityMarkedPayload::entityUuid,
            PacketCodecs.STRING, EntityMarkedPayload::entityName,
            EntityMarkedPayload::new
    );


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