package com.shushuwonie.clairvoyance.client.mirror;

import net.minecraft.client.gl.Framebuffer;

public class FramebufferOverride {
    private static final ThreadLocal<Framebuffer> OVERRIDE_FRAMEBUFFER = new ThreadLocal<>();

    public static void setOverride(Framebuffer framebuffer) {
        OVERRIDE_FRAMEBUFFER.set(framebuffer);
    }

    public static void clearOverride() {
        OVERRIDE_FRAMEBUFFER.remove();
    }

    public static Framebuffer getOverride() {
        return OVERRIDE_FRAMEBUFFER.get();
    }
}
