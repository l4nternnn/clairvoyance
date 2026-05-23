package com.shushuwonie.clairvoyance.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.shushuwonie.clairvoyance.item.modblock.moditems.Assembly_ModItems;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;

import java.util.List;
import java.util.Set;

public class ReplaceBodyPartCommand {
    private static final Set<Item> BODY_PART_ITEMS = Set.of(
            Assembly_ModItems.HEAD_ITEM, Assembly_ModItems.TORSO_ITEM,
            Assembly_ModItems.LEFT_ARM_ITEM, Assembly_ModItems.RIGHT_ARM_ITEM,
            Assembly_ModItems.LEFT_LEG_ITEM, Assembly_ModItems.RIGHT_LEG_ITEM
    );

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("replacebodypart-替换肢体")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("localskin")
                        .then(CommandManager.argument("skinName", StringArgumentType.string())
                                .suggests((context, builder) -> CommandSource.suggestMatching(GiveBodyPartCommand.LOCAL_SKINS, builder))
                                .executes(ctx -> replaceWithLocalSkin(ctx, false))
                                .then(CommandManager.literal("slim")
                                        .executes(ctx -> replaceWithLocalSkin(ctx, true))
                                )
                        )
                )
                .then(CommandManager.literal("player")
                        .then(CommandManager.argument("playerName", StringArgumentType.string())
                                .executes(ctx -> replaceWithPlayer(ctx, false))
                                .then(CommandManager.literal("slim")
                                        .executes(ctx -> replaceWithPlayer(ctx, true))
                                )
                        )
                )
        );
    }

    private static int replaceWithLocalSkin(CommandContext<ServerCommandSource> ctx, boolean slim) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        String skinName = StringArgumentType.getString(ctx, "skinName");
        int count = replaceItemDisplays(player, skinName, slim);
        ctx.getSource().sendFeedback(() -> Text.literal("已将 " + count + " 个肢体展示实体替换为内置皮肤 " + skinName), true);
        return count;
    }

    private static int replaceWithPlayer(CommandContext<ServerCommandSource> ctx, boolean slim) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        String targetName = StringArgumentType.getString(ctx, "playerName");
        ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(targetName);
        if (target == null) {
            source.sendError(Text.literal("玩家 " + targetName + " 不在线"));
            return 0;
        }
        int count = replaceItemDisplays(player, target, slim);
        source.sendFeedback(() -> Text.literal("已将 " + count + " 个肢体展示实体替换为玩家 " + targetName), true);
        return count;
    }

    private static int replaceItemDisplays(ServerPlayerEntity player, String localSkin, boolean slim) {
        ServerWorld world = (ServerWorld) player.getWorld();
        Box box = new Box(player.getBlockPos()).expand(3);
        List<ItemDisplayEntity> entities = world.getEntitiesByClass(ItemDisplayEntity.class, box,
                e -> BODY_PART_ITEMS.contains(e.getItemStack().getItem()));

        for (ItemDisplayEntity display : entities) {
            Item item = display.getItemStack().getItem();
            ItemStack newStack = createStackWithLocalSkin(item, localSkin, slim);
            display.setItemStack(newStack);
        }
        return entities.size();
    }

    private static int replaceItemDisplays(ServerPlayerEntity player, ServerPlayerEntity target, boolean slim) {
        ServerWorld world = (ServerWorld) player.getWorld();
        Box box = new Box(player.getBlockPos()).expand(3);
        List<ItemDisplayEntity> entities = world.getEntitiesByClass(ItemDisplayEntity.class, box,
                e -> BODY_PART_ITEMS.contains(e.getItemStack().getItem()));

        ProfileComponent profile = new ProfileComponent(target.getGameProfile());
        for (ItemDisplayEntity display : entities) {
            Item item = display.getItemStack().getItem();
            ItemStack newStack = createStackWithProfile(item, profile, slim);
            display.setItemStack(newStack);
        }
        return entities.size();
    }

    private static ItemStack createStackWithLocalSkin(Item item, String skinName, boolean slim) {
        ItemStack stack = new ItemStack(item);
        NbtCompound nbt = new NbtCompound();
        nbt.putString("local_skin", skinName);
        if (slim) {
            nbt.putString("arm_model", "slim");
        }
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§6§k13§4" + skinName + "§r§6§k13§r的" + chineseName(item)));
        return stack;
    }

    private static ItemStack createStackWithProfile(Item item, ProfileComponent profile, boolean slim) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.PROFILE, profile);
        if (slim) {
            NbtCompound nbt = new NbtCompound();
            nbt.putString("arm_model", "slim");
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        }
        String name = profile.gameProfile().getName();
        if (name != null) {
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§6§k13§4" + name + "§r§6§k13§r的" + chineseName(item)));
        }
        return stack;
    }

    private static String chineseName(Item item) {
        if (item == Assembly_ModItems.HEAD_ITEM) return "头";
        if (item == Assembly_ModItems.TORSO_ITEM) return "躯干";
        if (item == Assembly_ModItems.LEFT_ARM_ITEM) return "左臂";
        if (item == Assembly_ModItems.RIGHT_ARM_ITEM) return "右臂";
        if (item == Assembly_ModItems.LEFT_LEG_ITEM) return "左腿";
        if (item == Assembly_ModItems.RIGHT_LEG_ITEM) return "右腿";
        return "";
    }
}
