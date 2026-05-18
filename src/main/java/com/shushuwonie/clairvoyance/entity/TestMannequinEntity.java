package com.shushuwonie.clairvoyance.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.util.Arm;
import net.minecraft.world.World;

public class TestMannequinEntity extends LivingEntity {
    public TestMannequinEntity(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
        // 防止被推动或攻击消失（测试用）
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

//    @Override
    public Iterable<net.minecraft.item.ItemStack> getArmorItems() {
        return java.util.Collections.emptyList();
    }

    @Override
    public net.minecraft.item.ItemStack getEquippedStack(net.minecraft.entity.EquipmentSlot slot) {
        return net.minecraft.item.ItemStack.EMPTY;
    }

    @Override
    public void equipStack(net.minecraft.entity.EquipmentSlot slot, net.minecraft.item.ItemStack stack) {
    }

    @Override
    public void onDeath(DamageSource source) {
        // 测试实体不产生掉落物
        super.onDeath(source);
    }
    // ========== LivingEntity 中还有一个抽象方法 getMainArm() ==========
    @Override
    public Arm getMainArm() {
        // 假人默认右手为主手（不影响显示）
        return Arm.RIGHT;
    }
    // ========== 可选：覆写属性注册，使假人拥有基础的移动速度（虽然我们设为 noGravity） ==========
    public static DefaultAttributeContainer.Builder createMobAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(EntityAttributes.MAX_HEALTH, 20.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.0); // 速度0，确保不会移动
    }
}