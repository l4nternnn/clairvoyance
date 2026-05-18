package com.shushuwonie.clairvoyance.item.gazeguidance;

import net.minecraft.item.Item;

public class MagicStickItem extends Item {
    public MagicStickItem(Settings settings) {
        super(settings);
    }

//    @Override
//    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
//        if (!user.getWorld().isClient()) {
//            double maxRange = 200.0;
//            if (user.squaredDistanceTo(entity) <= maxRange * maxRange) {
//                // 添加发光效果，持续5秒（100 tick）
////                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 100, 0, false, false), user);
//                // 生成粒子效果
//                if (user.getWorld() instanceof ServerWorld serverWorld) {
////                    serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
////                            entity.getX(), entity.getY() + entity.getHeight() / 2, entity.getZ(),
////                            10, 0.5, 0.5, 0.5, 0.1);
//                }
//            }
//        }
//        // 返回 SUCCESS 不会造成伤害或消耗
//        return ActionResult.SUCCESS;
//    }
}