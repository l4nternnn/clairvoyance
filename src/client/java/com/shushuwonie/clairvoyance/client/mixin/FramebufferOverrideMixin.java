package com.shushuwonie.clairvoyance.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import com.shushuwonie.clairvoyance.client.features.mirror.FramebufferOverride;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class FramebufferOverrideMixin {
	@Inject(method = "getFramebuffer", at = @At("HEAD"), cancellable = true)
	private void onGetFramebuffer(CallbackInfoReturnable<Framebuffer> cir) {
		Framebuffer override = FramebufferOverride.getOverride();
		if (override != null) {
			cir.setReturnValue(override);
		}
	}
}
