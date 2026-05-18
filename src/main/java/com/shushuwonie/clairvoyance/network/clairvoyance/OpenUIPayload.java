package com.shushuwonie.clairvoyance.network.clairvoyance;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenUIPayload() implements CustomPayload {
    public static final CustomPayload.Id<OpenUIPayload> ID = new CustomPayload.Id<>(Identifier.of("clairvoyance", "open_ui"));
    public static final PacketCodec<RegistryByteBuf, OpenUIPayload> CODEC = PacketCodec.unit(new OpenUIPayload());



    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}