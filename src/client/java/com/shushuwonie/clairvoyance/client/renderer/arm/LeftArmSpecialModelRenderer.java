package com.shushuwonie.clairvoyance.client.renderer.arm;

import com.mojang.serialization.MapCodec;
import com.shushuwonie.clairvoyance.client.model.ModModelLayers;
import com.shushuwonie.clairvoyance.client.model.arm.LeftArmModel;
import com.shushuwonie.clairvoyance.client.model.arm.LeftArmSlimModel;
import com.shushuwonie.clairvoyance.client.renderer.special.BodyPartSpecialModelRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector3f;

import java.util.Set;

public class LeftArmSpecialModelRenderer extends BodyPartSpecialModelRenderer {
    private final LeftArmModel model;
    private final LeftArmSlimModel slimModel;

    public LeftArmSpecialModelRenderer(LoadedEntityModels entityModels) {
        super(entityModels);
        this.model = new LeftArmModel(entityModels.getModelPart(ModModelLayers.LEFT_ARM));
        this.slimModel = new LeftArmSlimModel(entityModels.getModelPart(ModModelLayers.LEFT_ARM_SLIM));
    }

    @Override
    protected void renderModel(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                               RenderLayer renderLayer, int light, int overlay, Data data) {
        boolean slim = "slim".equals(data.armModel());
        if (slim) {
            slimModel.render(matrices, vertexConsumers.getBuffer(renderLayer), light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            model.render(matrices, vertexConsumers.getBuffer(renderLayer), light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @Override
    public void collectVertices(Set<Vector3f> vertices) {
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.translate(0.5F, 1.0F, 0.5F);
        matrixStack.scale(-1.0F, -1.0F, 1.0F);
        this.model.getRootPart().collectVertices(matrixStack, vertices);
    }

    public static class Unbaked extends BodyPartSpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public MapCodec<Unbaked> getCodec() {
            return CODEC;
        }

        @Override
        public BodyPartSpecialModelRenderer bake(LoadedEntityModels entityModels) {
            return new LeftArmSpecialModelRenderer(entityModels);
        }
    }
}
