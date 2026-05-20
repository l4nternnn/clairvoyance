package com.shushuwonie.clairvoyance.features.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class BodyPartBlockEntity extends BlockEntity {
    @Nullable private ProfileComponent owner;
    private Identifier skinTexture = null; // 客户端缓存，不持久化

    public BodyPartBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    public ProfileComponent getOwner() {
        return owner;
    }

    public void setOwner(ProfileComponent owner) {
        this.owner = owner;
        this.markDirty();
        this.skinTexture = null; // 清除缓存，下次渲染时重新计算
        if (world != null && !world.isClient) {
            // 同步数据到客户端
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
            // 或发送自定义 S2C 包，但 updateListeners 通常已够用
        }
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.owner = view.read("owner", ProfileComponent.CODEC).orElse(null);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        if (this.owner != null) {
            view.put("owner", ProfileComponent.CODEC, this.owner);
        }
    }
}