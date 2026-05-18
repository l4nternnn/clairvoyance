// AnchorParticleS2CPacket.java
package com.shushuwonie.clairvoyance.network.clairvoyance;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public record AnchorParticleS2CPacket(UUID standId, Vec3d pos, int type) implements CustomPayload {
    public static final Id<AnchorParticleS2CPacket> ID = new Id<>(Identifier.of("clairvoyance", "anchor_particle"));
    public static final PacketCodec<RegistryByteBuf, AnchorParticleS2CPacket> CODEC = PacketCodec.of(
            (packet, buf) -> {
                buf.writeUuid(packet.standId);
                buf.writeDouble(packet.pos.x);
                buf.writeDouble(packet.pos.y);
                buf.writeDouble(packet.pos.z);
                buf.writeInt(packet.type);
            },
            buf -> new AnchorParticleS2CPacket(buf.readUuid(), new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble()), buf.readInt())
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