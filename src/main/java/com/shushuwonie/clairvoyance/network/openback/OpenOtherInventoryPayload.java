package com.shushuwonie.clairvoyance.network.openback;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenOtherInventoryPayload(int targetEntityId) implements CustomPayload {
    public static final CustomPayload.Id<OpenOtherInventoryPayload> ID =
            new CustomPayload.Id<>(Identifier.of("clairvoyance", "open_other_inv"));
    public static final PacketCodec<RegistryByteBuf, OpenOtherInventoryPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.INTEGER, OpenOtherInventoryPayload::targetEntityId,
                    OpenOtherInventoryPayload::new
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