package com.shushuwonie.clairvoyance.features.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class BodyPartBlockEntity extends BlockEntity {
    @Nullable private ProfileComponent owner;

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