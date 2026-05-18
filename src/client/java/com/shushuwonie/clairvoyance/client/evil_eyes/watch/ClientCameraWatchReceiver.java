package com.shushuwonie.clairvoyance.client.evil_eyes.watch;

import com.shushuwonie.clairvoyance.network.camerawatch.CameraUpdateS2CPacket;
import com.shushuwonie.clairvoyance.network.camerawatch.CameraWatchUnbindS2CPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ClientCameraWatchReceiver {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(CameraUpdateS2CPacket.ID, (packet, context) -> {
            context.client().execute(() ->
                    CameraWatchClientHandler.onCameraUpdate(packet.pos(), packet.yaw(), packet.pitch())
            );
        });

        ClientPlayNetworking.registerGlobalReceiver(CameraWatchUnbindS2CPacket.ID, (packet, context) -> {
            context.client().execute(CameraWatchClientHandler::onUnbind);
        });
    }
}