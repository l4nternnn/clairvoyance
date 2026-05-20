package com.shushuwonie.clairvoyance.features.block.torso;

import com.shushuwonie.clairvoyance.entity.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class TorsoBlockEntity extends BlockEntity {
    @Nullable private ProfileComponent owner;

    public TorsoBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TORSO_BLOCK_ENTITY, pos, state);
        System.out.println("TorsoBlockEntity constructor called");
    }

    @Nullable
    public ProfileComponent getOwner() {
        return this.owner;
    }
    public void setOwner(ProfileComponent owner) {
        this.owner = owner;
        this.markDirty();
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.owner = view.read("owner", ProfileComponent.CODEC).orElse(null);
        System.out.println("Read owner: " + (this.owner != null ? this.owner.gameProfile().getName() : "null"));
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        if (this.owner != null) {
            view.put("owner", ProfileComponent.CODEC, this.owner);
            System.out.println("Write owner: " + this.owner.gameProfile().getName());
        }
    }
}