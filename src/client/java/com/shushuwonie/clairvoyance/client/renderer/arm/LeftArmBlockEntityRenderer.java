package com.shushuwonie.clairvoyance.client.renderer.arm;

import com.shushuwonie.clairvoyance.client.model.ModModelLayers;
import com.shushuwonie.clairvoyance.client.model.arm.LeftArmModel;
import com.shushuwonie.clairvoyance.features.block.arm.LeftArmBlock;
import com.shushuwonie.clairvoyance.features.block.arm.LeftArmBlockEntity;
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

public class LeftArmBlockEntityRenderer implements BlockEntityRenderer<LeftArmBlockEntity> {
    private final LeftArmModel model;

    public LeftArmBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.model = new LeftArmModel(ctx.getLayerModelPart(ModModelLayers.LEFT_ARM));
    }

    @Override
    public void render(LeftArmBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay, Vec3d cameraPos) {
        // 与躯干类似的渲染逻辑，但可能需要微调平移
        Direction direction = entity.getCachedState().get(LeftArmBlock.FACING);
        float yaw = getYawFromDirection(direction);

        matrices.push();
        matrices.translate(0.6F, 0.0F, 0.6F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - yaw));
        matrices.scale(1.0F, -1.0F, -1.0F);
        matrices.translate(0.0F, 0.0F, 0.0F);

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
        return Identifier.of("clairvoyance", "textures/block/torso.png");
    }
    // 复用 getSkinTexture 和 getYawFromDirection
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