package com.shushuwonie.clairvoyance.network.clairvoyance;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public record ExplosionParticleS2CPacket(Vec3d pos) implements CustomPayload {
    public static final Id<ExplosionParticleS2CPacket> ID = new Id<>(Identifier.of("clairvoyance", "explosion_particle"));
    public static final PacketCodec<RegistryByteBuf, ExplosionParticleS2CPacket> CODEC = PacketCodec.of(
            (packet, buf) -> {
                buf.writeDouble(packet.pos.x);
                buf.writeDouble(packet.pos.y);
                buf.writeDouble(packet.pos.z);
            },
            buf -> new ExplosionParticleS2CPacket(new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble()))
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