package com.shushuwonie.clairvoyance.network;

import net.minecraft.util.Identifier;

public class ModPackets {
    // 观看系统（新系统）
    public static final Identifier CAMERA_UPDATE = Identifier.of("clairvoyance", "camera_update");
    public static final Identifier CAMERA_UNBIND = Identifier.of("clairvoyance", "camera_unbind");

    // 千里眼（Evil Eyes）系统
    public static final Identifier GLOBAL_CONFIG = Identifier.of("clairvoyance", "global_config");
    public static final Identifier OPEN_UI = Identifier.of("clairvoyance", "open_ui");
    public static final Identifier ENTITY_MARKED = Identifier.of("clairvoyance", "entity_marked");
    public static final Identifier TOGGLE_IMAGES = Identifier.of("clairvoyance", "toggle_images");
    public static final Identifier SELECT_VIEW = Identifier.of("clairvoyance", "select_view");
    public static final Identifier FORCE_EXIT_VIEW = Identifier.of("clairvoyance", "force_exit_view");
    public static final Identifier MARK_PARTICLE = Identifier.of("clairvoyance", "mark_particle");
    public static final Identifier PLAYER_STAGE = Identifier.of("clairvoyance", "player_stage");
    public static final Identifier ANCHOR_PARTICLE = Identifier.of("clairvoyance", "anchor_particle");
    public static final Identifier EXPLOSION_PARTICLE = Identifier.of("clairvoyance", "explosion_particle");

    // 视线诱导系统
    public static final Identifier SYNC_CONFIG = Identifier.of("clairvoyance", "sync_config");
    public static final Identifier ENERGY_SYNC = Identifier.of("clairvoyance", "energy_sync");
    public static final Identifier MARK_COUNT = Identifier.of("clairvoyance", "mark_count");
    public static final Identifier FOCUS_STATUS = Identifier.of("clairvoyance", "focus_status");
    public static final Identifier PARTICLE = Identifier.of("clairvoyance", "particle");
    public static final Identifier STRENGTH = Identifier.of("clairvoyance", "strength");

    // 原相机绑定（旧系统，已废弃但保留）
    public static final Identifier CAMERA_WATCH_BIND = Identifier.of("clairvoyance", "camera_watch_bind");
    public static final Identifier CAMERA_WATCH_UNBIND = Identifier.of("clairvoyance", "camera_watch_unbind");
}