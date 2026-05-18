package com.shushuwonie.clairvoyance.network.gazeguidance;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record UpdateConfigC2SPacket(String json) implements CustomPayload {
    public static final Id<UpdateConfigC2SPacket> ID = new Id<>(Identifier.of("clairvoyance", "update_config"));
    public static final PacketCodec<RegistryByteBuf, UpdateConfigC2SPacket> CODEC = PacketCodec.of(UpdateConfigC2SPacket::write, UpdateConfigC2SPacket::new);

    private UpdateConfigC2SPacket(RegistryByteBuf buf) {
        this(buf.readString());
    }

    private void write(RegistryByteBuf buf) {
        buf.writeString(json);
    }

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