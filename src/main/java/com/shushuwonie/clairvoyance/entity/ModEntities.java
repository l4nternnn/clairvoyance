package com.shushuwonie.clairvoyance.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModEntities {

    // 1. 先用 EntityType.Builder 构建 EntityType 对象
    public static final EntityType<TestMannequinEntity> TEST_MANNEQUIN =
            EntityType.Builder.create(TestMannequinEntity::new, SpawnGroup.MISC)
                    .dimensions(0.6f,1.8f)   // 设置为玩家大小的碰撞箱
                    .build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of("clairvoyance", "test_mannequin"))) ; // 不需要传 Identifier，build() 会生成一个临时 ID，后面再注册

    // 2. 显式注册到注册表（这是标准做法）
    public static void register() {
        Registry.register(Registries.ENTITY_TYPE,
                Identifier.of("clairvoyance", "test_mannequin"),
                TEST_MANNEQUIN
        );

        // 3. 关键：注册实体属性（生命值、移动速度等）
        FabricDefaultAttributeRegistry.register(TEST_MANNEQUIN, TestMannequinEntity.createMobAttributes());
    }

    // 你可以保留 initialize() 来调用 register()
    public static void initialize() {
        register();
    }
}