package com.shushuwonie.clairvoyance.network.gazeguidance;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public record ParticlePacket(double x, double y, double z) implements CustomPayload {
    public static final Id<ParticlePacket> ID = new Id<>(Identifier.of("clairvoyance", "particle"));
    public static final PacketCodec<RegistryByteBuf, ParticlePacket> CODEC = PacketCodec.tuple(
            PacketCodecs.DOUBLE, ParticlePacket::x,
            PacketCodecs.DOUBLE, ParticlePacket::y,
            PacketCodecs.DOUBLE, ParticlePacket::z,
            ParticlePacket::new
    );
    private static boolean registered = false;

    public ParticlePacket(Vec3d pos) {
        this(pos.x, pos.y, pos.z);
    }

    public Vec3d getPos() {
        return new Vec3d(x, y, z);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        if (!registered) {
            net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(ID, CODEC);
            registered = true;
        }
    }
}