package com.shushuwonie.clairvoyance.client.renderer.special;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector3f;

import java.util.Set;

public class CombinedBodySpecialModelRenderer extends BodyPartSpecialModelRenderer {
    private final PlayerEntityModel model;
    private final PlayerEntityModel slimModel;

    public CombinedBodySpecialModelRenderer(LoadedEntityModels entityModels) {
        super(entityModels);
        this.model = new PlayerEntityModel(entityModels.getModelPart(EntityModelLayers.PLAYER), false);
        this.slimModel = new PlayerEntityModel(entityModels.getModelPart(EntityModelLayers.PLAYER_SLIM), true);
    }

    @Override
    protected void renderModel(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                               RenderLayer renderLayer, int light, int overlay, Data data) {
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);
        boolean slim = "slim".equals(data.armModel());
        (slim ? slimModel : model).render(matrices, vertexConsumer, light, overlay);
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
            return new CombinedBodySpecialModelRenderer(entityModels);
        }
    }
}
