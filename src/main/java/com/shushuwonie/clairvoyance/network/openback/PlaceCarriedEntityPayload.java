package com.shushuwonie.clairvoyance.network.openback;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PlaceCarriedEntityPayload() implements CustomPayload {
    public static final CustomPayload.Id<PlaceCarriedEntityPayload> ID =
            new CustomPayload.Id<>(Identifier.of("clairvoyance", "place_carried_entity"));
    public static final PacketCodec<RegistryByteBuf, PlaceCarriedEntityPayload> CODEC =
            PacketCodec.unit(new PlaceCarriedEntityPayload());


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