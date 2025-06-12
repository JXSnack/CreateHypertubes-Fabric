package com.pedrorok.hypertube.blocks;

import com.pedrorok.hypertube.blocks.blockentities.HyperEntranceBlockEntity;
import com.pedrorok.hypertube.managers.TravelManager;
import com.pedrorok.hypertube.registry.ModBlockEntities;
import com.pedrorok.hypertube.utils.VoxelUtils;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Rok, Pedro Lucas nmm. Created on 21/04/2025
 * @project Create Hypertube
 */
public class HyperEntranceBlock extends KineticBlock implements BlockEntityProvider, ICogWheel, TubeConnection {

    public static final DirectionProperty FACING = Properties.FACING;
    public static final BooleanProperty OPEN = Properties.OPEN;

    private static final VoxelShape SHAPE_NORTH = Block.createCuboidShape(0D, 0D, 0D, 16D, 16D, 23D);
    private static final VoxelShape SHAPE_SOUTH = Block.createCuboidShape(0D, 0D, -7D, 16D, 16D, 16D);
    private static final VoxelShape SHAPE_EAST = Block.createCuboidShape(-7D, 0D, 0D, 16D, 16D, 16D);
    private static final VoxelShape SHAPE_WEST = Block.createCuboidShape(0D, 0D, 0D, 23D, 16D, 16D);
    private static final VoxelShape SHAPE_UP = Block.createCuboidShape(0D, -7D, 0D, 16D, 16D, 16D);
    private static final VoxelShape SHAPE_DOWN = Block.createCuboidShape(0D, 0D, 0D, 16D, 23D, 16D);


    public HyperEntranceBlock(Settings properties) {
        super(properties);
        setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(OPEN, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(OPEN);
        super.appendProperties(builder);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null) {
            return this.getDefaultState().with(FACING, context.getSide().getOpposite())
                    .with(OPEN, false);
        }
        Direction direction = player.getHorizontalFacing();
        if (player.getPitch() < -45) {
            direction = Direction.UP;
        } else if (player.getPitch() > 45) {
            direction = Direction.DOWN;
        }
        return this.getDefaultState()
                .with(FACING, direction)
                .with(OPEN, false);
    }

    @Override
    public @NotNull BlockState rotate(BlockState state, BlockRotation rot) {
        return state.with(FACING, rot.rotate(state.get(FACING)));
    }

    @Override
    public @NotNull BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.with(FACING, mirror.apply(state.get(FACING)));
    }

    @Override
    public boolean hasShaftTowards(WorldView world, BlockPos pos, BlockState state, Direction face) {
        return false;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.get(FACING).getAxis();
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return ModBlockEntities.HYPERTUBE_ENTRANCE.create(blockPos, blockState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull World level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return (level1, pos, state1, be) -> ((HyperEntranceBlockEntity) be).tick();
    }

    public boolean canTravelConnect(WorldAccess world, BlockPos pos, Direction facing) {
        BlockState state = world.getBlockState(pos);
        return facing.getOpposite() == state.get(FACING)
               && state.getBlock() instanceof HyperEntranceBlock;
    }

    public VoxelShape getShape(BlockState state, @Nullable ShapeContext ctx) {
        if (ctx instanceof EntityShapeContext ecc
            && ecc.getEntity() != null
            && ecc.getEntity().getPersistentData().getBoolean(TravelManager.TRAVEL_TAG)) {
            return VoxelUtils.empty();
        }
        return switch (state.get(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            case UP -> SHAPE_UP;
            case DOWN -> SHAPE_DOWN;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public @NotNull VoxelShape getOutlineShape(@NotNull BlockState state, @NotNull BlockView worldIn, @NotNull BlockPos pos, @NotNull ShapeContext context) {
        return getShape(state, context);
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockView worldIn, @NotNull BlockPos pos, @NotNull ShapeContext context) {
        return getShape(state, context);
    }

    @Override
    public @NotNull VoxelShape getSidesShape(@NotNull BlockState state, @NotNull BlockView reader, @NotNull BlockPos pos) {
        return getShape(state);
    }

    @Override
    public @NotNull VoxelShape getRaycastShape(@NotNull BlockState state, @NotNull BlockView worldIn, @NotNull BlockPos pos) {
        return getShape(state);
    }

    @Override
    public @NotNull BlockRenderType getRenderType(@NotNull BlockState state) {
        return BlockRenderType.MODEL;
    }

    public VoxelShape getShape(BlockState state) {
        return getShape(state, null);
    }

    @Override
    public boolean isSmallCog() {
        return true;
    }

    @Override
    public ActionResult onSneakWrenched(BlockState state, ItemUsageContext context) {
        return super.onSneakWrenched(state, context);
    }
}
