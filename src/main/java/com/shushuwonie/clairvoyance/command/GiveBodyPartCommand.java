package com.shushuwonie.clairvoyance.command;

import com.mojang.brigadier.CommandDispatcher;
import com.shushuwonie.clairvoyance.item.modblock.moditems.Assembly_ModItems;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class GiveBodyPartCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("getbodypart-获取肢体")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("target", EntityArgumentType.player())
                        .then(CommandManager.argument("source", EntityArgumentType.player())
                                .then(CommandManager.literal("all")
                                        .executes(ctx -> giveAllParts(
                                                EntityArgumentType.getPlayer(ctx, "target"),
                                                EntityArgumentType.getPlayer(ctx, "source"),
                                                false
                                        ))
                                        .then(CommandManager.literal("slim")
                                                .executes(ctx -> giveAllParts(
                                                        EntityArgumentType.getPlayer(ctx, "target"),
                                                        EntityArgumentType.getPlayer(ctx, "source"),
                                                        true
                                                ))
                                        )
                                )
                                .then(CommandManager.literal("torso")
                                        .executes(ctx -> giveBodyPart(
                                                EntityArgumentType.getPlayer(ctx, "target"),
                                                EntityArgumentType.getPlayer(ctx, "source"),
                                                "torso",
                                                false
                                        ))
                                )
                                .then(CommandManager.literal("left_arm")
                                        .executes(ctx -> giveBodyPart(
                                                EntityArgumentType.getPlayer(ctx, "target"),
                                                EntityArgumentType.getPlayer(ctx, "source"),
                                                "left_arm",
                                                false
                                        ))
                                        .then(CommandManager.literal("slim")
                                                .executes(ctx -> giveBodyPart(
                                                        EntityArgumentType.getPlayer(ctx, "target"),
                                                        EntityArgumentType.getPlayer(ctx, "source"),
                                                        "left_arm",
                                                        true
                                                ))
                                        )
                                )
                                .then(CommandManager.literal("right_arm")
                                        .executes(ctx -> giveBodyPart(
                                                EntityArgumentType.getPlayer(ctx, "target"),
                                                EntityArgumentType.getPlayer(ctx, "source"),
                                                "right_arm",
                                                false
                                        ))
                                        .then(CommandManager.literal("slim")
                                                .executes(ctx -> giveBodyPart(
                                                        EntityArgumentType.getPlayer(ctx, "target"),
                                                        EntityArgumentType.getPlayer(ctx, "source"),
                                                        "right_arm",
                                                        true
                                                ))
                                        )
                                )
                                .then(CommandManager.literal("left_leg")
                                        .executes(ctx -> giveBodyPart(
                                                EntityArgumentType.getPlayer(ctx, "target"),
                                                EntityArgumentType.getPlayer(ctx, "source"),
                                                "left_leg",
                                                false
                                        ))
                                )
                                .then(CommandManager.literal("right_leg")
                                        .executes(ctx -> giveBodyPart(
                                                EntityArgumentType.getPlayer(ctx, "target"),
                                                EntityArgumentType.getPlayer(ctx, "source"),
                                                "right_leg",
                                                false
                                        ))
                                )
                        )
                )
        );
    }

    private static int giveBodyPart(PlayerEntity target, PlayerEntity source, String part, boolean slim) {
        ItemStack stack = createPartStack(source, part, slim);
        if (stack.isEmpty()) return 0;
        if (!target.getInventory().insertStack(stack)) {
            target.dropItem(stack, false);
        }
        target.sendMessage(Text.literal("获得了 " + source.getName().getString() + "的" + part), false);
        return 1;
    }

    private static int giveAllParts(PlayerEntity target, PlayerEntity source, boolean slim) {
        String[] parts = {"torso", "left_arm", "right_arm", "left_leg", "right_leg"};
        for (String part : parts) {
            giveBodyPart(target, source, part, slim);
        }
        // 额外给予玩家头颅
        ItemStack headStack = new ItemStack(Items.PLAYER_HEAD);
        headStack.set(DataComponentTypes.PROFILE, new ProfileComponent(source.getGameProfile()));
        if (!target.getInventory().insertStack(headStack)) {
            target.dropItem(headStack, false);
        }
        target.sendMessage(Text.literal("获得" + source.getName().getString() + "所有的身体部件"), false);
        return 1;
    }

    private static ItemStack createPartStack(PlayerEntity source, String part, boolean slim) {
        Item item;
        String chineseName;
        switch (part) {
            case "torso":
                item = Assembly_ModItems.TORSO_ITEM;
                chineseName = "躯干";
                break;
            case "left_arm":
                item = Assembly_ModItems.LEFT_ARM_ITEM;
                chineseName = "左臂";
                break;
            case "right_arm":
                item = Assembly_ModItems.RIGHT_ARM_ITEM;
                chineseName = "右臂";
                break;
            case "left_leg":
                item = Assembly_ModItems.LEFT_LEG_ITEM;
                chineseName = "左腿";
                break;
            case "right_leg":
                item = Assembly_ModItems.RIGHT_LEG_ITEM;
                chineseName = "右腿";
                break;
            default:
                return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.PROFILE, new ProfileComponent(source.getGameProfile()));
        // 手臂部位存储模型类型覆盖
        if (slim && (part.equals("left_arm") || part.equals("right_arm"))) {
            NbtCompound nbt = new NbtCompound();
            nbt.putString("arm_model", "slim");
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        }
        // 使用中文名称：玩家名 + "的" + 中文部位名
        String playerName = source.getName().getString();
        Text displayName = Text.literal("§6§k13§4" + playerName + "§r§6§k13§r" + "的" + chineseName);
        stack.set(DataComponentTypes.CUSTOM_NAME, displayName);
        return stack;
    }
}
