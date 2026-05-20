package com.shushuwonie.clairvoyance.client.renderer.torso;

import com.shushuwonie.clairvoyance.client.model.ModModelLayers;
import com.shushuwonie.clairvoyance.client.model.torso.TorsoModel;
import com.shushuwonie.clairvoyance.features.block.torso.TorsoBlock;
import com.shushuwonie.clairvoyance.features.block.torso.TorsoBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class TorsoBlockEntityRenderer implements BlockEntityRenderer<TorsoBlockEntity> {
//    private final ModelPart head;
    private final TorsoModel model;

    public TorsoBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
//        this.head = ctx.getLayerModelPart(EntityModelLayers.PLAYER_HEAD);
        this.model = new TorsoModel(ctx.getLayerModelPart(ModModelLayers.TORSO));
    }

    @Override
    public void render(TorsoBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay, Vec3d cameraPos) {
        Direction direction = entity.getCachedState().get(TorsoBlock.FACING);
        float yaw = getYawFromDirection(direction);

        matrices.push();
        matrices.translate(0.5F, 0.0F, 0.5F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - yaw));
        matrices.scale(1.0F, -1.0F, -1.0F);
        matrices.translate(0.0F, 0.5F, 0.0F);

        Identifier texture = getSkinTexture(entity.getOwner());
        RenderLayer renderLayer = RenderLayer.getEntityTranslucent(texture);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);
//        head.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
        model.setHeadRotation(0, yaw, 0); // 如果需要动画可传入参数
        model.render(matrices, vertexConsumers.getBuffer(renderLayer), light, OverlayTexture.DEFAULT_UV);
        matrices.pop();
    }

    private Identifier getSkinTexture(@Nullable ProfileComponent owner) {
        if (owner != null && owner.gameProfile() != null) {
            return MinecraftClient.getInstance()
                    .getSkinProvider()
                    .getSkinTextures(owner.gameProfile())
                    .texture();
        }
        return Identifier.of("clairvoyance", "textures/block/torso.png");
    }

    private float getYawFromDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> 180.0F;
            case EAST -> -90.0F;
            case SOUTH -> 0.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }
}