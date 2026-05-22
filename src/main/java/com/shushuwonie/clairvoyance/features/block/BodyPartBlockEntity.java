package com.shushuwonie.clairvoyance.features.block;

import com.shushuwonie.clairvoyance.screen.BodyPartScreenHandler;
import com.shushuwonie.clairvoyance.util.ImplementedInventory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class BodyPartBlockEntity extends BlockEntity implements ImplementedInventory, NamedScreenHandlerFactory {
    // ========== 皮肤数据部分 ==========
    @Nullable private ProfileComponent owner;
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
        // 仅在服务端且 owner 不完整时触发异步解析
        if (this.world != null && !this.world.isClient && owner != null && !owner.isCompleted()) {
            if (this.loadingFuture != null) this.loadingFuture.cancel(false);
            this.loadingFuture = owner.getFuture().thenApplyAsync(resolved -> {
                if (resolved != null && !resolved.equals(this.owner)) {
                    this.owner = resolved;
                    this.markDirty();
                    // 同步到客户端
                    if (this.world != null && !this.world.isClient) {
                        ((ServerWorld) this.world).getChunkManager().markForUpdate(this.pos);
                    }
                }
                return resolved;
            }, Util.getMainWorkerExecutor());
        } else {
            // 如果 owner 完整或为客户端，直接同步
            if (this.world != null && !this.world.isClient) {
                ((ServerWorld) this.world).getChunkManager().markForUpdate(this.pos);
            }
        }
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        // 读取物品栏
        Inventories.readData(view, inventory);
        // 读取 owner
        Optional<ProfileComponent> optional = view.read("owner", ProfileComponent.CODEC);
        this.owner = optional.orElse(null);
        // 如果 owner 存在且不完整，尝试解析（仅在服务端）
        if (this.owner != null && !this.owner.isCompleted() && this.world != null && !this.world.isClient) {
            this.setOwner(this.owner);
        } else if (this.owner != null && this.owner.isCompleted() && this.world != null && !this.world.isClient) {
            // 已完成，同步到客户端（确保纹理立即显示）
            ((ServerWorld) this.world).getChunkManager().markForUpdate(this.pos);
        }
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, inventory);
        if (this.owner != null) {
            view.put("owner", ProfileComponent.CODEC, this.owner);
        }
    }

    // ========== 物品栏部分 ==========
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(9, ItemStack.EMPTY);

    @Override
    public DefaultedList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        getItems().set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }
        this.markDirty();
    }

    @Override
    public ItemStack removeStack(int slot, int count) {
        ItemStack result = ImplementedInventory.super.removeStack(slot, count);
        if (!result.isEmpty()) this.markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = ImplementedInventory.super.removeStack(slot);
        if (!result.isEmpty()) this.markDirty();
        return result;
    }

    @Override
    public void clear() {
        getItems().clear();
        this.markDirty();
    }

    // ========== GUI 部分 ==========
    @Override
    public Text getDisplayName() {
        return Text.translatable(getCachedState().getBlock().getTranslationKey());
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new BodyPartScreenHandler(syncId, inv, this);
    }
}