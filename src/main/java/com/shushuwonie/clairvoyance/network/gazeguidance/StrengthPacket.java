package com.shushuwonie.clairvoyance.network.gazeguidance;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StrengthPacket(int stage, double drain, double regen, double range, int maxMarks) implements CustomPayload {
    public static final Id<StrengthPacket> ID = new Id<>(Identifier.of("clairvoyance", "strength"));
    public static final PacketCodec<RegistryByteBuf, StrengthPacket> CODEC = PacketCodec.of(StrengthPacket::write, StrengthPacket::new);

    private StrengthPacket(RegistryByteBuf buf) {
        this(buf.readInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readInt());
    }

    private void write(RegistryByteBuf buf) {
        buf.writeInt(stage);
        buf.writeDouble(drain);
        buf.writeDouble(regen);
        buf.writeDouble(range);
        buf.writeInt(maxMarks);
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