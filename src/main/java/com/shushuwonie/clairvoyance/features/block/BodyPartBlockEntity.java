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
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.util.Base64;

public class BodyPartBlockEntity extends BlockEntity implements ImplementedInventory, NamedScreenHandlerFactory {
    // ========== 皮肤数据部分 ==========
    @Nullable private ProfileComponent owner;
    @Nullable private CompletableFuture<ProfileComponent> loadingFuture;
    @Nullable private UUID playerUuid;
    private String skinType = "default"; // "default" = Steve, "slim" = Alex

    public BodyPartBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    public ProfileComponent getOwner() {
        return owner;
    }

    public void setOwner(ProfileComponent owner) {
        this.owner = owner;
        // 提取UUID和皮肤类型
        if (owner != null && owner.gameProfile() != null) {
            this.playerUuid = owner.gameProfile().getId();
            this.skinType = detectSkinType(owner.gameProfile());
        }
        this.markDirty();
        // 仅在服务端且 owner 不完整时触发异步解析
        if (this.world != null && !this.world.isClient && owner != null && !owner.isCompleted()) {
            if (this.loadingFuture != null) this.loadingFuture.cancel(false);
            this.loadingFuture = owner.getFuture().thenApplyAsync(resolved -> {
                if (resolved != null && !resolved.equals(this.owner)) {
                    this.owner = resolved;
                    if (resolved.gameProfile() != null) {
                        this.playerUuid = resolved.gameProfile().getId();
                        this.skinType = detectSkinType(resolved.gameProfile());
                    }
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

    @Nullable
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getSkinType() {
        return skinType;
    }

    public void setSkinType(String skinType) {
        this.skinType = skinType;
        this.markDirty();
        if (this.world != null && !this.world.isClient) {
            ((ServerWorld) this.world).getChunkManager().markForUpdate(this.pos);
        }
    }

    /**
     * 从GameProfile的纹理属性中检测皮肤类型（"slim"=Alex, "default"=Steve）
     */
    public static String detectSkinType(GameProfile profile) {
        if (profile == null) return "default";
        for (Property prop : profile.getProperties().get("textures")) {
            try {
                String json = new String(Base64.getDecoder().decode(prop.value()));
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                if (root.has("textures") && root.get("textures").isJsonObject()) {
                    JsonObject textures = root.get("textures").getAsJsonObject();
                    if (textures.has("SKIN") && textures.get("SKIN").isJsonObject()) {
                        JsonObject skin = textures.get("SKIN").getAsJsonObject();
                        if (skin.has("metadata") && skin.get("metadata").isJsonObject()) {
                            JsonObject metadata = skin.get("metadata").getAsJsonObject();
                            if (metadata.has("model") && "slim".equals(metadata.get("model").getAsString())) {
                                return "slim";
                            }
                        }
                    }
                }
            } catch (Exception ignored) { }
        }
        return "default";
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        // 读取物品栏
        Inventories.readData(view, inventory);
        // 读取 owner
        Optional<ProfileComponent> optional = view.read("owner", ProfileComponent.CODEC);
        this.owner = optional.orElse(null);
        // 读取持久化的UUID和皮肤类型
        Optional<UUID> uuidOpt = view.read("player_uuid", Uuids.CODEC);
        this.playerUuid = uuidOpt.orElse(null);
        this.skinType = view.read("skin_type", Codec.STRING).orElse("default");
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
        if (this.playerUuid != null) {
            view.put("player_uuid", Uuids.CODEC, this.playerUuid);
        }
        view.put("skin_type", Codec.STRING, this.skinType);
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