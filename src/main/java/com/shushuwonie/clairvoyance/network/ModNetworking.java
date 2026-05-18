package com.shushuwonie.clairvoyance.network;

import com.shushuwonie.clairvoyance.network.camerawatch.*;
import com.shushuwonie.clairvoyance.network.clairvoyance.*;
import com.shushuwonie.clairvoyance.network.gazeguidance.*;
import com.shushuwonie.clairvoyance.network.openback.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class ModNetworking {
    // 注册所有 S2C 包（客户端和服务端都需要）
    public static void registerS2CPackets() {
        GlobalConfigS2CPacket.register();
        OpenUIPacket.register();
        EntityMarkedPayload.register();
        ToggleImagesS2CPacket.register();
        SelectViewPayload.register();
        ForceExitViewPayload.register();
        MarkParticleS2CPacket.register();
        SyncConfigS2CPacket.register();
        EnergySyncPacket.register();
        MarkCountPacket.register();
        FocusStatusPacket.register();
        ParticlePacket.register();
        StrengthPacket.register();
        AnchorParticleS2CPacket.register();
        PlayerStageS2CPacket.register();
        ExplosionParticleS2CPacket.register();
        CameraWatchBindS2CPacket.register();
        CameraWatchUnbindS2CPacket.register();
        CameraUpdateS2CPacket.register();
    }

    // 注册所有 C2S 包（只需要服务端）
    public static void registerC2SPackets() {
        MarkEntityPayload.register();
        SelectViewPayload.register();  // 注意：你的 SelectViewPayload 实际上是 S2C？请确认
        ExitViewPayload.register();
        MagicPacket.register();
        RightClickActionPacket.register();
        RequestConfigC2SPacket.register();
        UpdateConfigC2SPacket.register();
        RequestGlobalConfigC2SPacket.register();
        UpdateGlobalConfigC2SPacket.register();
        PlaceParrotC2SPacket.register();
        AnchorDestroyC2SPacket.register();
        OpenOtherInventoryPayload.register();
        CarryEntityPayload.register();
        PlaceCarriedEntityPayload.register();
        CameraWatchStartC2SPacket.register();
        CameraWatchStopC2SPacket.register();
    }
}