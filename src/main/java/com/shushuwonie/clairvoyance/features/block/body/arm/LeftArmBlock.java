package com.shushuwonie.clairvoyance.features.block.body.arm;

import com.mojang.serialization.MapCodec;
import com.shushuwonie.clairvoyance.features.block.body.BodyPartBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class LeftArmBlock extends BlockWithEntity {
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = VoxelShapes.cuboid(0.25, 0, 0.25, 0.75, 0.75, 0.75);



    public LeftArmBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return createCodec(LeftArmBlock::new);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new LeftArmBlockEntity(pos, state);   // 确保这个类存在
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (world.getBlockEntity(pos) instanceof LeftArmBlockEntity armEntity) {
            ProfileComponent itemProfile = itemStack.get(DataComponentTypes.PROFILE);
            if (itemProfile != null) {
                armEntity.setOwner(itemProfile);
            } else if (placer instanceof PlayerEntity player) {
                armEntity.setOwner(new ProfileComponent(player.getGameProfile()));
            }
            // 读取物品栈中的手臂模型覆盖标记
            NbtComponent customData = itemStack.get(DataComponentTypes.CUSTOM_DATA);
            if (customData != null) {
                NbtCompound nbt = customData.copyNbt();
                if (nbt.contains("arm_model")) {
                    nbt.getString("arm_model").ifPresent(armEntity::setSkinType);
				}
				if (nbt.contains("local_skin")) {
					nbt.getString("local_skin").ifPresent(armEntity::setLocalSkin);
				}
            }
            armEntity.markDirty();
        }
    }

//    @Override
    public Identifier getParticleTexture(BlockState state) {
        // 可以改为任意你想要的纹理，例如石头的粒子
        return Identifier.of("minecraft", "block/stone");
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {

        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof BodyPartBlockEntity bodyPart) {
                player.openHandledScreen(bodyPart);
            }
        }
        return ActionResult.SUCCESS;
    }
}