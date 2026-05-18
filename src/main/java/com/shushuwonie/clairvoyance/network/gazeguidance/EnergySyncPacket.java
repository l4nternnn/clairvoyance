package com.shushuwonie.clairvoyance.network.gazeguidance;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record EnergySyncPacket(double currentEnergy, double maxEnergy) implements CustomPayload {
    public static final Id<EnergySyncPacket> ID = new Id<>(Identifier.of("clairvoyance", "energy_sync"));
    public static final PacketCodec<RegistryByteBuf, EnergySyncPacket> CODEC = PacketCodec.of(EnergySyncPacket::write, EnergySyncPacket::new);

    private EnergySyncPacket(RegistryByteBuf buf) {
        this(buf.readDouble(), buf.readDouble());
    }

    private void write(RegistryByteBuf buf) {
        buf.writeDouble(currentEnergy);
        buf.writeDouble(maxEnergy);
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