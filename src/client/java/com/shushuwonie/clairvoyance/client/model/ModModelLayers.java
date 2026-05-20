package com.shushuwonie.clairvoyance.client.model;

import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class ModModelLayers {
    public static final EntityModelLayer TORSO = new EntityModelLayer(Identifier.
            of("clairvoyance", "torso"), "main");


    public static final EntityModelLayer LEFT_ARM = new EntityModelLayer(Identifier.
            of("clairvoyance", "left_arm"), "main");

    public static final EntityModelLayer RIGHT_ARM = new EntityModelLayer(Identifier.
            of("clairvoyance", "right_arm"), "main");

    public static final EntityModelLayer LEFT_LEG = new EntityModelLayer(Identifier.
            of("clairvoyance", "left_leg"), "main");

    public static final EntityModelLayer RIGHT_LEG = new EntityModelLayer(Identifier.
            of("clairvoyance", "right_leg"), "main");

}