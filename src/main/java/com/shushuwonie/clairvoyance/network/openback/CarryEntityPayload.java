package com.shushuwonie.clairvoyance.network.openback;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CarryEntityPayload(int entityId) implements CustomPayload {
    public static final CustomPayload.Id<CarryEntityPayload> ID =
            new CustomPayload.Id<>(Identifier.of("clairvoyance", "carry_entity"));
    public static final PacketCodec<RegistryByteBuf, CarryEntityPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.INTEGER, CarryEntityPayload::entityId, CarryEntityPayload::new);


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