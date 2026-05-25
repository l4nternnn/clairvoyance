package com.shushuwonie.clairvoyance.item.mirror;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class MirrorOfThenAndNowItem extends Item {
	public MirrorOfThenAndNowItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		// The toggle logic is handled by:
		// Client: UseItemCallback in ClairvoyanceClient sends MirrorToggleC2SPacket
		// Server: C2S global receiver in Clairvoyance.java -> MirrorCommand.toggleViewport()
		return ActionResult.SUCCESS;
	}
}
