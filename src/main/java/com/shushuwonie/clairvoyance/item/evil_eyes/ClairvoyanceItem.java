package com.shushuwonie.clairvoyance.item.evil_eyes;

import com.shushuwonie.clairvoyance.network.clairvoyance.OpenUIPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class ClairvoyanceItem extends Item {
    public ClairvoyanceItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient) {
            ServerPlayNetworking.send((ServerPlayerEntity) user, new OpenUIPacket());
        }
        return ActionResult.SUCCESS;
    }
}