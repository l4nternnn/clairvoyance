//package com.shushuwonie.clairvoyance.client.renderer.itemrenderer;
//
//import com.shushuwonie.clairvoyance.client.model.ModModelLayers;
//import com.shushuwonie.clairvoyance.client.model.torso.TorsoModel;
//import com.shushuwonie.clairvoyance.client.model.arm.LeftArmModel;
//import com.shushuwonie.clairvoyance.client.model.arm.RightArmModel;
//import com.shushuwonie.clairvoyance.client.model.leg.LeftLegModel;
//import com.shushuwonie.clairvoyance.client.model.leg.RightLegModel;
//import com.shushuwonie.clairvoyance.item.modblock.moditems.Assembly_ModItems;
//import net.fabricmc.fabric.api.client.rendering.v1.DynamicItemRenderer;
//import net.minecraft.client.MinecraftClient;
//import net.minecraft.client.render.RenderLayer;
//import net.minecraft.client.render.VertexConsumerProvider;
//import net.minecraft.client.render.entity.model.EntityModelLoader;
//import net.minecraft.client.util.math.MatrixStack;
//import net.minecraft.component.DataComponentTypes;
//import net.minecraft.component.type.ProfileComponent;
//import net.minecraft.item.ItemDisplayContext;
//import net.minecraft.item.ItemStack;
//import net.minecraft.util.Identifier;
//import net.minecraft.util.math.RotationAxis;
//
//public class BodyPartItemRenderer implements DynamicItemRenderer {
//    private final TorsoModel torsoModel;
//    private final LeftArmModel leftArmModel;
//    private final RightArmModel rightArmModel;
//    private final LeftLegModel leftLegModel;
//    private final RightLegModel rightLegModel;
//
//    public BodyPartItemRenderer(EntityModelLoader modelLoader) {
//        this.torsoModel = new TorsoModel(modelLoader.getModelPart(ModModelLayers.TORSO));
//        this.leftArmModel = new LeftArmModel(modelLoader.getModelPart(ModModelLayers.LEFT_ARM));
//        this.rightArmModel = new RightArmModel(modelLoader.getModelPart(ModModelLayers.RIGHT_ARM));
//        this.leftLegModel = new LeftLegModel(modelLoader.getModelPart(ModModelLayers.LEFT_LEG));
//        this.rightLegModel = new RightLegModel(modelLoader.getModelPart(ModModelLayers.RIGHT_LEG));
//    }
//
//    @Override
//    public void render(ItemStack stack, ItemDisplayContext displayContext, MatrixStack matrices,
//                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
//        var model = getModelForItem(stack.getItem());
//        if (model == null) return;
//
//        matrices.push();
//        // 使用与玩家头类似的变换
//        matrices.translate(0.5F, 0.0F, 0.5F);
//        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
//        matrices.scale(-1.0F, -1.0F, 1.0F);
//        matrices.translate(0.0F, -1.5F, 0.0F);
//
//        Identifier texture = getTextureForItem(stack);
//        RenderLayer renderLayer = RenderLayer.getEntityTranslucent(texture);
//        model.render(matrices, vertexConsumers.getBuffer(renderLayer), light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
//        matrices.pop();
//    }
//
//    private Object getModelForItem(net.minecraft.item.Item item) {
//        if (item == Assembly_ModItems.TORSO_ITEM.asItem()) return torsoModel;
//        if (item == Assembly_ModItems.LEFT_ARM_ITEM.asItem()) return leftArmModel;
//        if (item == Assembly_ModItems.RIGHT_ARM_ITEM.asItem()) return rightArmModel;
//        if (item == Assembly_ModItems.LEFT_LEG_ITEM.asItem()) return leftLegModel;
//        if (item == Assembly_ModItems.RIGHT_LEG_ITEM.asItem()) return rightLegModel;
//        return null;
//    }
//
//    private Identifier getTextureForItem(ItemStack stack) {
//        ProfileComponent profile = stack.get(DataComponentTypes.PROFILE);
//        if (profile != null && profile.gameProfile() != null) {
//            return MinecraftClient.getInstance()
//                    .getSkinProvider()
//                    .getSkinTextures(profile.gameProfile())
//                    .texture();
//        }
//        return Identifier.of("clairvoyance", "textures/entity/torso/default.png");
//    }
//}