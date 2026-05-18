package com.shushuwonie.clairvoyance.network.gazeguidance;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SyncConfigS2CPacket(String json) implements CustomPayload {
    public static final Id<SyncConfigS2CPacket> ID = new Id<>(Identifier.of("clairvoyance", "sync_config"));
    public static final PacketCodec<RegistryByteBuf, SyncConfigS2CPacket> CODEC = PacketCodec.of(SyncConfigS2CPacket::write, SyncConfigS2CPacket::new);

    private SyncConfigS2CPacket(RegistryByteBuf buf) {
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
            net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(ID, CODEC);
            registered = true;
        }
    }
}