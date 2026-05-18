package com.shushuwonie.clairvoyance.network.clairvoyance;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record GlobalConfigS2CPacket(String json) implements CustomPayload {
    public static final Id<GlobalConfigS2CPacket> ID = new Id<>(Identifier.of("clairvoyance", "global_config"));
    public static final PacketCodec<PacketByteBuf, GlobalConfigS2CPacket> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, GlobalConfigS2CPacket::json, GlobalConfigS2CPacket::new);

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }

    private static boolean registered = false;
    public static void register() {
        if (!registered) {
            net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(ID, CODEC);
            registered = true;
        }
    }


    public record StageConfig(int dailyLimit, int maxMarks, int minScore, int maxScore, int watchRequiredTicks, int parrotDailyLimit, int maxActiveParrots) {}
}
