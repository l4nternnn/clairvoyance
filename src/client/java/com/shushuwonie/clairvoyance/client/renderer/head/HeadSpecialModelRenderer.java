package com.shushuwonie.clairvoyance.client.renderer.head;

import com.mojang.serialization.MapCodec;
import com.shushuwonie.clairvoyance.client.model.ModModelLayers;
import com.shushuwonie.clairvoyance.client.model.head.HeadModel;
import com.shushuwonie.clairvoyance.client.renderer.special.BodyPartSpecialModelRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector3f;
import java.util.Set;

public class HeadSpecialModelRenderer extends BodyPartSpecialModelRenderer {
    private final HeadModel model;

    public HeadSpecialModelRenderer(LoadedEntityModels entityModels) {
        super(entityModels);
        this.model = new HeadModel(entityModels.getModelPart(ModModelLayers.HEAD));
    }

    @Override
    protected void renderModel(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                               RenderLayer renderLayer, int light, int overlay, Data data) {
        model.render(matrices, vertexConsumers.getBuffer(renderLayer), light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public void collectVertices(Set<Vector3f> vertices) {
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.translate(0.5F, 0.5F, 0.5F);
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
            return new HeadSpecialModelRenderer(entityModels);
        }
    }
}
