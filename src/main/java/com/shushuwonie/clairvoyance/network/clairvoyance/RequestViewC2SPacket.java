// 文件：RequestViewC2SPacket.java
package com.shushuwonie.clairvoyance.network.clairvoyance;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record RequestViewC2SPacket(UUID entityUuid) implements CustomPayload {
    public static final Id<RequestViewC2SPacket> ID = new Id<>(Identifier.of("clairvoyance", "request_view"));
    public static final PacketCodec<RegistryByteBuf, RequestViewC2SPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING.xmap(UUID::fromString, UUID::toString), RequestViewC2SPacket::entityUuid,
            RequestViewC2SPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}