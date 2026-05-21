package com.shushuwonie.clairvoyance.screen;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {
    public static final ScreenHandlerType<BodyPartScreenHandler> BODY_PART_SCREEN_HANDLER =
            new ScreenHandlerType<>(BodyPartScreenHandler::new, FeatureFlags.VANILLA_FEATURES);

    public static void initialize() {
        Registry.register(Registries.SCREEN_HANDLER,
                Identifier.of("clairvoyance", "body_part"),
                BODY_PART_SCREEN_HANDLER);
    }
}