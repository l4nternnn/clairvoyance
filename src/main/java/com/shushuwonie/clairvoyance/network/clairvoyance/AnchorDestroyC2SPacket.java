package com.shushuwonie.clairvoyance.network.clairvoyance;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record AnchorDestroyC2SPacket(UUID standId) implements CustomPayload {
    public static final Id<AnchorDestroyC2SPacket> ID = new Id<>(Identifier.of("clairvoyance", "anchor_destroy"));
    public static final PacketCodec<RegistryByteBuf, AnchorDestroyC2SPacket> CODEC = PacketCodec.of(
            (packet, buf) -> buf.writeUuid(packet.standId),
            buf -> new AnchorDestroyC2SPacket(buf.readUuid())
    );


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