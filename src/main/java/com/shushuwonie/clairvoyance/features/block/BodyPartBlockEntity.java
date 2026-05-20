package com.shushuwonie.clairvoyance.features.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class BodyPartBlockEntity extends BlockEntity {
    @Nullable private ProfileComponent owner;
    private Identifier skinTexture = null; // 客户端缓存，不持久化
    @Nullable private CompletableFuture<ProfileComponent> loadingFuture;


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
        // 如果有正在进行的加载，取消它
        if (this.loadingFuture != null) {
            this.loadingFuture.cancel(false);
        }
        // 异步解析完整档案
        this.loadingFuture = owner.getFuture().thenApplyAsync(resolved -> {
            if (resolved != null && !resolved.equals(this.owner)) {
                this.owner = resolved;
                this.markDirty();
                // 通知客户端该方块实体数据已更新，重新渲染
                if (this.world != null && !this.world.isClient) {
                    ((ServerWorld) this.world).getChunkManager().markForUpdate(this.pos);
                }
            }
            return resolved;
        }, Util.getMainWorkerExecutor());
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        // 读取 owner
        Optional<ProfileComponent> optional = view.read("owner", ProfileComponent.CODEC);
        this.owner = optional.orElse(null);
        // 读取后触发异步解析
        if (this.owner != null && !this.owner.isCompleted()) {
            this.setOwner(this.owner); // 会触发异步解析
        }
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        if (this.owner != null) {
            view.put("owner", ProfileComponent.CODEC, this.owner);
        }
    }
}