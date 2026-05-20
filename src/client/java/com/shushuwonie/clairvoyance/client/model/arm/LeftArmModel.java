package com.shushuwonie.clairvoyance.client.model.arm;

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.util.math.MatrixStack;

public class LeftArmModel extends SkullBlockEntityModel {
    private final ModelPart left_arm;

    public LeftArmModel(ModelPart root) {
        super(root);
        this.left_arm = root.getChild(EntityModelPartNames.HEAD);
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        // 左臂：宽6，高12，深6，原点偏移使其位于方块左侧
        root.addChild(EntityModelPartNames.HEAD,
                ModelPartBuilder.create().uv(32, 48).cuboid(-2.0F, -12.0F, -2.0f, 4.0F, 12.0F, 4.0F),
                ModelTransform.origin(0.0f,-6.0f,0.0f));
        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setHeadRotation(float animationProgress, float yaw, float pitch) {
        this.left_arm.yaw = yaw * (float)(Math.PI / 180.0);
        this.left_arm.pitch = pitch * (float)(Math.PI / 180.0);
    }
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha){
        this.left_arm.render(matrices, vertices, light, overlay);
    }
}