package com.shushuwonie.clairvoyance.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.shushuwonie.clairvoyance.item.modblock.moditems.Assembly_ModItems;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
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
    // 内置皮肤列表（文件名不含扩展名）
    public static final String[] LOCAL_SKINS = {
        "anan", "-coco-", "ema", "hanna", "hiro", "mago", "miria", "mll", "nnk",
        "noa", "reiya", "sherry", "yalisa", "yuki", " kanshou"
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("getbodypart-获取肢体")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("target", EntityArgumentType.player())
                        // 原始：从指定玩家获取肢体
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
                                        .then(CommandManager.literal("slim")
                                                .executes(ctx -> giveBodyPart(
                                                        EntityArgumentType.getPlayer(ctx, "target"),
                                                        EntityArgumentType.getPlayer(ctx, "source"),
                                                        "torso",
                                                        true
                                                ))
                                        )
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
                                        .then(CommandManager.literal("slim")
                                                .executes(ctx -> giveBodyPart(
                                                        EntityArgumentType.getPlayer(ctx, "target"),
                                                        EntityArgumentType.getPlayer(ctx, "source"),
                                                        "left_leg",
                                                        true
                                                ))
                                        )
                                )
                                .then(CommandManager.literal("right_leg")
                                        .executes(ctx -> giveBodyPart(
                                                EntityArgumentType.getPlayer(ctx, "target"),
                                                EntityArgumentType.getPlayer(ctx, "source"),
                                                "right_leg",
                                                false
                                        ))
                                        .then(CommandManager.literal("slim")
                                                .executes(ctx -> giveBodyPart(
                                                        EntityArgumentType.getPlayer(ctx, "target"),
                                                        EntityArgumentType.getPlayer(ctx, "source"),
                                                        "right_leg",
                                                        true
                                                ))
                                        )
                                )
                                .then(CommandManager.literal("head")
                                        .executes(ctx -> giveBodyPart(
                                                EntityArgumentType.getPlayer(ctx, "target"),
                                                EntityArgumentType.getPlayer(ctx, "source"),
                                                "head",
                                                false
                                        ))
                                )
                        )
                        // 新增：使用内置皮肤
                        .then(CommandManager.literal("localskin")
                                .then(CommandManager.argument("skinName", StringArgumentType.string())
                                        .suggests((context, builder) -> CommandSource.suggestMatching(LOCAL_SKINS, builder))
                                        .then(CommandManager.literal("all")
                                                .executes(ctx -> giveAllLocalSkinParts(
                                                        EntityArgumentType.getPlayer(ctx, "target"),
                                                        StringArgumentType.getString(ctx, "skinName"),
                                                        false
                                                ))
                                                .then(CommandManager.literal("slim")
                                                        .executes(ctx -> giveAllLocalSkinParts(
                                                                EntityArgumentType.getPlayer(ctx, "target"),
                                                                StringArgumentType.getString(ctx, "skinName"),
                                                                true
                                                        ))
                                                )
                                        )
                                        .then(CommandManager.literal("torso")
                                                .executes(ctx -> giveLocalSkinPart(
                                                        EntityArgumentType.getPlayer(ctx, "target"),
                                                        StringArgumentType.getString(ctx, "skinName"),
                                                        "torso",
                                                        false
                                                ))
                                                .then(CommandManager.literal("slim")
                                                        .executes(ctx -> giveLocalSkinPart(
                                                                EntityArgumentType.getPlayer(ctx, "target"),
                                                                StringArgumentType.getString(ctx, "skinName"),
                                                                "torso",
                                                                true
                                                        ))
                                                )
                                        )
                                        .then(CommandManager.literal("left_arm")
                                                .executes(ctx -> giveLocalSkinPart(
                                                        EntityArgumentType.getPlayer(ctx, "target"),
                                                        StringArgumentType.getString(ctx, "skinName"),
                                                        "left_arm",
                                                        false
                                                ))
                                                .then(CommandManager.literal("slim")
                                                        .executes(ctx -> giveLocalSkinPart(
                                                                EntityArgumentType.getPlayer(ctx, "target"),
                                                                StringArgumentType.getString(ctx, "skinName"),
                                                                "left_arm",
                                                                true
                                                        ))
                                                )
                                        )
                                        .then(CommandManager.literal("right_arm")
                                                .executes(ctx -> giveLocalSkinPart(
                                                        EntityArgumentType.getPlayer(ctx, "target"),
                                                        StringArgumentType.getString(ctx, "skinName"),
                                                        "right_arm",
                                                        false
                                                ))
                                                .then(CommandManager.literal("slim")
                                                        .executes(ctx -> giveLocalSkinPart(
                                                                EntityArgumentType.getPlayer(ctx, "target"),
                                                                StringArgumentType.getString(ctx, "skinName"),
                                                                "right_arm",
                                                                true
                                                        ))
                                                )
                                        )
                                        .then(CommandManager.literal("left_leg")
                                                .executes(ctx -> giveLocalSkinPart(
                                                        EntityArgumentType.getPlayer(ctx, "target"),
                                                        StringArgumentType.getString(ctx, "skinName"),
                                                        "left_leg",
                                                        false
                                                ))
                                                .then(CommandManager.literal("slim")
                                                        .executes(ctx -> giveLocalSkinPart(
                                                                EntityArgumentType.getPlayer(ctx, "target"),
                                                                StringArgumentType.getString(ctx, "skinName"),
                                                                "left_leg",
                                                                true
                                                        ))
                                                )
                                        )
                                        .then(CommandManager.literal("right_leg")
                                                .executes(ctx -> giveLocalSkinPart(
                                                        EntityArgumentType.getPlayer(ctx, "target"),
                                                        StringArgumentType.getString(ctx, "skinName"),
                                                        "right_leg",
                                                        false
                                                ))
                                                .then(CommandManager.literal("slim")
                                                        .executes(ctx -> giveLocalSkinPart(
                                                                EntityArgumentType.getPlayer(ctx, "target"),
                                                                StringArgumentType.getString(ctx, "skinName"),
                                                                "right_leg",
                                                                true
                                                        ))
                                                )
                                        )
                                        .then(CommandManager.literal("head")
                                                .executes(ctx -> giveLocalSkinPart(
                                                        EntityArgumentType.getPlayer(ctx, "target"),
                                                        StringArgumentType.getString(ctx, "skinName"),
                                                        "head",
                                                        false
                                                ))
                                                .then(CommandManager.literal("slim")
                                                        .executes(ctx -> giveLocalSkinPart(
                                                                EntityArgumentType.getPlayer(ctx, "target"),
                                                                StringArgumentType.getString(ctx, "skinName"),
                                                                "head",
                                                                true
                                                        ))
                                                )
                                        )
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
        String[] parts = {"head", "torso", "left_arm", "right_arm", "left_leg", "right_leg"};
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

    private static int giveLocalSkinPart(PlayerEntity target, String skinName, String part, boolean slim) {
        ItemStack stack = createLocalSkinPartStack(skinName, part, slim);
        if (stack.isEmpty()) return 0;
        if (!target.getInventory().insertStack(stack)) {
            target.dropItem(stack, false);
        }
        target.sendMessage(Text.literal("获得了 " + skinName + " 的" + part), false);
        return 1;
    }

    private static int giveAllLocalSkinParts(PlayerEntity target, String skinName, boolean slim) {
        String[] parts = {"head", "torso", "left_arm", "right_arm", "left_leg", "right_leg"};
        for (String part : parts) {
            giveLocalSkinPart(target, skinName, part, slim);
        }
        target.sendMessage(Text.literal("获得内置皮肤 " + skinName + " 所有的身体部件"), false);
        return 1;
    }

    private static ItemStack createPartStack(PlayerEntity source, String part, boolean slim) {
        Item item;
        String chineseName;
        switch (part) {
            case  "head":
                item = Assembly_ModItems.HEAD_ITEM;
                chineseName = "头";
                break;
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
        if (slim) {
            NbtCompound nbt = new NbtCompound();
            nbt.putString("arm_model", "slim");
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        }
        String playerName = source.getName().getString();
        Text displayName = Text.literal("§6§k13§4" + playerName + "§r§6§k13§r" + "的" + chineseName);
        stack.set(DataComponentTypes.CUSTOM_NAME, displayName);
        return stack;
    }

    private static ItemStack createLocalSkinPartStack(String skinName, String part, boolean slim) {
        Item item;
        String chineseName;
        switch (part) {
            case  "head":
                item = Assembly_ModItems.HEAD_ITEM;
                chineseName = "head";
                break;
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
        // 存储内置皮肤信息
        NbtCompound nbt = new NbtCompound();
        nbt.putString("local_skin", skinName);
        if (slim) {
            nbt.putString("arm_model", "slim");
        }
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        Text displayName = Text.literal("§6§k13§4" + skinName + "§r§6§k13§r" + "的" + chineseName);
        stack.set(DataComponentTypes.CUSTOM_NAME, displayName);
        return stack;
    }
}
