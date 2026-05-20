package com.shushuwonie.clairvoyance.client.model.torso;

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.util.math.MatrixStack;

public class TorsoModel extends SkullBlockEntityModel {
    private final ModelPart root;

    public TorsoModel(ModelPart root) {
        super(root);
        this.root = root;  // 保存根部件，而不是只保存 head
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData torso = root.addChild(EntityModelPartNames.HEAD,
                ModelPartBuilder.create().uv(16,16).cuboid(-4.0F, -12.0F, -2.0F, 8.0F, 12.0F, 4.0F),
                ModelTransform.origin(0.0F, 1.0f, 0.0F));

        torso.addChild(EntityModelPartNames.JACKET,
                ModelPartBuilder.create().uv(16, 32).cuboid(-4.0F, -12.0F, -2.0F, 8.0F, 12.0F, 4.0F, new Dilation(0.25F)),
                ModelTransform.origin(0.0F, 1.0F, 0.0F));



        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setHeadRotation(float animationProgress, float yaw, float pitch) {
        this.root.yaw = yaw * (float)(Math.PI / 180.0);
        this.root.pitch = pitch * (float)(Math.PI / 180.0);
    }

//    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        this.root.render(matrices, vertices, light, overlay);
    }
}
