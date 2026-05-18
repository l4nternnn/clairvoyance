// 文件：com.shushuwonie.clairvoyance.network.clairvoyance.PlayerStageS2CPacket
package com.shushuwonie.clairvoyance.network.clairvoyance;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PlayerStageS2CPacket(int stage) implements CustomPayload {
    public static final Id<PlayerStageS2CPacket> ID = new Id<>(Identifier.of("clairvoyance", "player_stage"));
    public static final PacketCodec<PacketByteBuf, PlayerStageS2CPacket> CODEC =
            PacketCodec.tuple(PacketCodecs.INTEGER, PlayerStageS2CPacket::stage, PlayerStageS2CPacket::new);

    private static boolean registered = false;
    public static void register() {
        if (!registered) {
            net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(ID, CODEC);
            registered = true;
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}