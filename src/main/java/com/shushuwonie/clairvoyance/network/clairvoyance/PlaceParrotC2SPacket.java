package com.shushuwonie.clairvoyance.network.clairvoyance;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public record PlaceParrotC2SPacket(Vec3d pos) implements CustomPayload {
    public static final Id<PlaceParrotC2SPacket> ID = new Id<>(Identifier.of("clairvoyance", "place_parrot"));
    public static final PacketCodec<RegistryByteBuf, PlaceParrotC2SPacket> CODEC = PacketCodec.of(
            (packet, buf) -> {
                buf.writeDouble(packet.pos.x);
                buf.writeDouble(packet.pos.y);
                buf.writeDouble(packet.pos.z);
            },
            buf -> new PlaceParrotC2SPacket(new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble()))
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