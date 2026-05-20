package com.shushuwonie.clairvoyance.client.model.torso;

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.util.math.MatrixStack;

public class TorsoModel extends SkullBlockEntityModel {
    private final ModelPart head;

    public TorsoModel(ModelPart root) {
        super(root);
        this.head = root.getChild(EntityModelPartNames.HEAD);
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        root.addChild(EntityModelPartNames.HEAD,
                ModelPartBuilder.create().uv(16,16).cuboid(-4.0F, -12.0F, -2.0F, 8.0F, 12.0F, 4.0F),
                ModelTransform.origin(0.0F, -6.0F, 0.0F));
        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setHeadRotation(float animationProgress, float yaw, float pitch) {
        this.head.yaw = yaw * (float)(Math.PI / 180.0);
        this.head.pitch = pitch * (float)(Math.PI / 180.0);
    }

//    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        this.head.render(matrices, vertices, light, overlay);
    }
}
