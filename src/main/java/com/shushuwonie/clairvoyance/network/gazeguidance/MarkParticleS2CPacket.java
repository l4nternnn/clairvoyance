package com.shushuwonie.clairvoyance.network.gazeguidance;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public record MarkParticleS2CPacket(Vec3d pos) implements CustomPayload {
    public static final Id<MarkParticleS2CPacket> ID = new Id<>(Identifier.of("clairvoyance", "mark_particle"));
    public static final PacketCodec<RegistryByteBuf, MarkParticleS2CPacket> CODEC = PacketCodec.of(MarkParticleS2CPacket::write, MarkParticleS2CPacket::new);

    private MarkParticleS2CPacket(RegistryByteBuf buf) {
        this(new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble()));
    }

    private void write(RegistryByteBuf buf) {
        buf.writeDouble(pos.x);
        buf.writeDouble(pos.y);
        buf.writeDouble(pos.z);
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