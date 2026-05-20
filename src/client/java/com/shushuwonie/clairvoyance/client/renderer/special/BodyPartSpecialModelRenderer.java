package com.shushuwonie.clairvoyance.client.renderer.special;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import java.util.Set;

public abstract class BodyPartSpecialModelRenderer implements SpecialModelRenderer<BodyPartSpecialModelRenderer.Data> {
    public record Data(RenderLayer layer) {
        static Data of(SkinTextures textures) {
            return new Data(RenderLayer.getEntityTranslucent(textures.texture()));
        }
    }

    private final Data defaultData;

    public BodyPartSpecialModelRenderer(LoadedEntityModels entityModels) {
        // 获取当前登录玩家的档案（用于默认纹理）
        var session = MinecraftClient.getInstance().getSession();
        GameProfile profile = new GameProfile(session.getUuidOrNull(), session.getUsername());
        SkinTextures textures = MinecraftClient.getInstance().getSkinProvider().getSkinTextures(profile);
        this.defaultData = Data.of(textures);
    }

    @Override
    public void render(@Nullable Data data, ItemDisplayContext displayContext, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay, boolean leftHanded) {
        Data actualData = data != null ? data : this.defaultData;
        RenderLayer renderLayer = actualData.layer();

        matrices.push();
        matrices.translate(0.5F, 0.0F, 0.5F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
        matrices.scale(-1.0F, -1.0F, 1.0F);
        matrices.translate(0.0F, -1.5F, 0.0F); // 根据模型高度微调

        renderModel(matrices, vertexConsumers, renderLayer, light, overlay);

        matrices.pop();
    }

    protected abstract void renderModel(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                        RenderLayer renderLayer, int light, int overlay);

    @Nullable
    @Override
    public Data getData(ItemStack stack) {
        ProfileComponent profile = stack.get(DataComponentTypes.PROFILE);
        if (profile == null) return null;
        ProfileComponent resolved = profile.resolve();
        if (resolved == null) return null;
        SkinTextures textures = MinecraftClient.getInstance().getSkinProvider()
                .getSkinTextures(resolved.gameProfile(), null);
        if (textures == null) return null;
        return Data.of(textures);
    }

    // 子类必须实现 collectVertices
    @Override
    public abstract void collectVertices(Set<Vector3f> vertices);

    public abstract static class Unbaked implements SpecialModelRenderer.Unbaked {
        @Override
        public abstract SpecialModelRenderer<?> bake(LoadedEntityModels entityModels);
    }
}