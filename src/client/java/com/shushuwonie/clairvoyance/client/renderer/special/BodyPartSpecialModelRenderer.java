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
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.Set;

public abstract class BodyPartSpecialModelRenderer implements SpecialModelRenderer<BodyPartSpecialModelRenderer.Data> {
    public record Data(RenderLayer layer, String armModel) {
        static Data of(SkinTextures textures) {
            return new Data(RenderLayer.getEntityTranslucent(textures.texture()), "default");
        }
    }

    private final Data defaultData;

    public BodyPartSpecialModelRenderer(LoadedEntityModels entityModels) {
        var session = MinecraftClient.getInstance().getSession();
        GameProfile profile = new GameProfile(session.getUuidOrNull(), session.getUsername());
        SkinTextures textures = MinecraftClient.getInstance().getSkinProvider().getSkinTextures(profile);
        this.defaultData = new Data(RenderLayer.getEntityTranslucent(textures.texture()), "default");
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
        matrices.translate(0.0F, 0.0F, 0.0F);

        renderModel(matrices, vertexConsumers, renderLayer, light, overlay, actualData);

        matrices.pop();
    }

    protected abstract void renderModel(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                        RenderLayer renderLayer, int light, int overlay, Data data);

    @Nullable
    @Override
    public Data getData(ItemStack stack) {
        String armModel = "default";

        // 先检查是否为内置皮肤
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            NbtCompound nbt = customData.copyNbt();
            Optional<String> localSkin = nbt.getString("local_skin");
            if (localSkin.isPresent()) {
                armModel = nbt.getString("arm_model").orElse("default");
                Identifier localTexture = Identifier.of("clairvoyance", "textures/local_skin/" + localSkin.get() + ".png");
                return new Data(RenderLayer.getEntityTranslucent(localTexture), armModel);
            }
        }

        ProfileComponent profile = stack.get(DataComponentTypes.PROFILE);
        if (profile == null) return null;
        ProfileComponent resolved = profile.resolve();
        if (resolved == null) return null;
        SkinTextures textures = MinecraftClient.getInstance().getSkinProvider()
                .getSkinTextures(resolved.gameProfile(), null);
        if (textures == null) return null;

        if (customData != null) {
            String model = customData.copyNbt().getString("arm_model").orElse("");
            if (!model.isEmpty()) {
                armModel = model;
            }
        }

        return new Data(RenderLayer.getEntityTranslucent(textures.texture()), armModel);
    }

    @Override
    public abstract void collectVertices(Set<Vector3f> vertices);

    public abstract static class Unbaked implements SpecialModelRenderer.Unbaked {
        @Override
        public abstract SpecialModelRenderer<?> bake(LoadedEntityModels entityModels);
    }
}
