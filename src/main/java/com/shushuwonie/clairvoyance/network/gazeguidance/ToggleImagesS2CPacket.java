package com.shushuwonie.clairvoyance.network.gazeguidance;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ToggleImagesS2CPacket(boolean enabled) implements CustomPayload {
    public static final Id<ToggleImagesS2CPacket> ID = new Id<>(Identifier.of("clairvoyance", "toggle_images"));
    public static final PacketCodec<RegistryByteBuf, ToggleImagesS2CPacket> CODEC = PacketCodec.of(
            (packet, buf) -> buf.writeBoolean(packet.enabled),
            buf -> new ToggleImagesS2CPacket(buf.readBoolean())
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