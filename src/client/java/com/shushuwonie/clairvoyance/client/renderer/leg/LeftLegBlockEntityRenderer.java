package com.shushuwonie.clairvoyance.client.renderer.leg;

import com.shushuwonie.clairvoyance.client.model.ModModelLayers;
import com.shushuwonie.clairvoyance.client.model.arm.RightArmModel;
import com.shushuwonie.clairvoyance.client.model.leg.LeftLegModel;
import com.shushuwonie.clairvoyance.features.block.arm.RightArmBlock;
import com.shushuwonie.clairvoyance.features.block.arm.RightArmBlockEntity;
import com.shushuwonie.clairvoyance.features.block.leg.LeftLegBlock;
import com.shushuwonie.clairvoyance.features.block.leg.LeftLegBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class LeftLegBlockEntityRenderer implements BlockEntityRenderer<LeftLegBlockEntity> {
    private final LeftLegModel model;

    public LeftLegBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.model = new LeftLegModel(ctx.getLayerModelPart(ModModelLayers.LEFT_LEG));
    }

    @Override
    public void render(LeftLegBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay, Vec3d cameraPos) {
        Direction direction = entity.getCachedState().get(LeftLegBlock.FACING);
        float yaw = getYawFromDirection(direction);

        matrices.push();
        matrices.translate(0.5F, 0.0F, 0.5F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - yaw));
        matrices.scale(1.0F, -1.0F, -1.0F);
        matrices.translate(0.0F, 0.5F, 0.0F);

        Identifier texture = getSkinTexture(entity.getOwner());
        RenderLayer renderLayer = RenderLayer.getEntityTranslucent(texture);
        model.setHeadRotation(0, yaw, 0);
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
        // 使用一个有效的默认纹理，建议放在 textures/entity/arm/ 下
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