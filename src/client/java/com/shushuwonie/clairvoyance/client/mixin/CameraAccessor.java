package com.shushuwonie.clairvoyance.client.mixin;

import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraAccessor {
	@Invoker("setPos")
	void invokeSetPos(double x, double y, double z);

	@Invoker("setRotation")
	void invokeSetRotation(float yaw, float pitch);
}
