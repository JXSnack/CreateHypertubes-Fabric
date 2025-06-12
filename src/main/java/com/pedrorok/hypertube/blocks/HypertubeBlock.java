package com.pedrorok.hypertube.blocks;

import com.pedrorok.hypertube.blocks.blockentities.HypertubeBlockEntity;
import com.pedrorok.hypertube.managers.TravelManager;
import com.pedrorok.hypertube.managers.placement.BezierConnection;
import com.pedrorok.hypertube.managers.placement.SimpleConnection;
import com.pedrorok.hypertube.registry.ModBlockEntities;
import com.pedrorok.hypertube.registry.ModBlocks;
import com.pedrorok.hypertube.registry.ModDataComponent;
import com.pedrorok.hypertube.utils.MessageUtils;
import com.pedrorok.hypertube.utils.RayCastUtils;
import com.pedrorok.hypertube.utils.TubeUtils;
import com.pedrorok.hypertube.utils.VoxelUtils;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Rok, Pedro Lucas nmm. Created on 21/05/2025
 * @project Create Hypertube
 */
public class HypertubeBlock extends TransparentBlock implements TubeConnection, IBE<HypertubeBlockEntity>, IWrenchable {

    public static final BooleanProperty NORTH_SOUTH = BooleanProperty.of("north_south");
    public static final BooleanProperty EAST_WEST = BooleanProperty.of("east_west");
    public static final BooleanProperty UP_DOWN = BooleanProperty.of("up_down");

    public static final VoxelShape SHAPE_NORTH_SOUTH = Block.createCuboidShape(0D, 0D, 4D, 16D, 16D, 11D);
    public static final VoxelShape SHAPE_EAST_WEST = Block.createCuboidShape(5D, 0D, 0D, 12D, 16D, 16D);
    public static final VoxelShape SHAPE_UP_DOWN = Block.createCuboidShape(0D, 4D, 0D, 16D, 11D, 16D);

    public HypertubeBlock(AbstractBlock.Settings properties) {
        super(properties);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH_SOUTH, EAST_WEST, UP_DOWN);
    }

    @Override
    public @Nullable BlockState getPlacementState(@NotNull ItemPlacementContext context) {
        BlockState state = super.getPlacementState(context);
        if (state == null) return null;
        for (Direction direction : Direction.values()) {
            BlockPos relative = context.getBlockPos().offset(direction);
            BlockState otherState = context.getWorld().getBlockState(relative);
            if (otherState.getBlock() instanceof TubeConnection) {
                return getState(Set.of(direction));
            }
        }
        return state.with(NORTH_SOUTH, false)
                .with(EAST_WEST, false)
                .with(UP_DOWN, false);
    }

    public VoxelShape getShape(BlockState state, @Nullable ShapeContext ctx) {
        if (ctx instanceof EntityShapeContext ecc
            && ecc.getEntity() != null
            && ecc.getEntity().getPersistentData().getBoolean(TravelManager.TRAVEL_TAG)) {
            return VoxelUtils.empty();
        }
        if (state.get(EAST_WEST)) {
            return SHAPE_EAST_WEST;
        }
        if (state.get(UP_DOWN)) {
            return SHAPE_UP_DOWN;
        }
        return SHAPE_NORTH_SOUTH;
    }

    @Override
    public void neighborUpdate(@NotNull BlockState state, @NotNull World world, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos pos1, boolean b) {
        super.neighborUpdate(state, world, pos, block, pos1, b);
        BlockState newState = getStateFromBlockEntity(world, pos);
        world.setBlockState(pos, newState);
    }

    private BlockState getStateFromBlockEntity(World world, BlockPos pos) {
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof HypertubeBlockEntity hypertube)) {
            return getState(world, pos);
        }

        BezierConnection connTo = hypertube.getConnectionTo();
        if (connTo != null) {
            Direction dirTo = connTo.getFromPos().direction();
            if (dirTo != null) {
                return getState(Set.of(dirTo));
            }
        }

        SimpleConnection connFrom = hypertube.getConnectionFrom();
        if (connFrom == null) {
            return getState(world, pos);
        }

        BlockEntity otherBE = world.getBlockEntity(connFrom.pos());
        if (!(otherBE instanceof HypertubeBlockEntity other)) {
            return getState(world, pos);
        }

        BezierConnection otherTo = other.getConnectionTo();
        if (otherTo != null) {
            Direction dirFrom = otherTo.getToPos().direction();
            if (dirFrom != null) {
                return getState(Set.of(dirFrom));
            }
        }
        return getState(world, pos);
    }


    public BlockState getState(Collection<Direction> activeDirections) {
        if (activeDirections == null) {
            return getDefaultState()
                    .with(NORTH_SOUTH, false)
                    .with(EAST_WEST, false)
                    .with(UP_DOWN, false);
        }
        boolean northSouth = activeDirections.contains(Direction.NORTH) || activeDirections.contains(Direction.SOUTH);
        boolean eastWest = activeDirections.contains(Direction.EAST) || activeDirections.contains(Direction.WEST);
        boolean upDown = activeDirections.contains(Direction.UP) || activeDirections.contains(Direction.DOWN);
        // only one axis can be true at a time
        return getDefaultState()
                .with(NORTH_SOUTH, northSouth)
                .with(EAST_WEST, eastWest && !northSouth)
                .with(UP_DOWN, upDown && !northSouth && !eastWest);
    }

    private BlockState getState(World world, BlockPos pos) {
        boolean northSouth = isConnected(world, pos, Direction.NORTH) || isConnected(world, pos, Direction.SOUTH);
        boolean eastWest = isConnected(world, pos, Direction.EAST) || isConnected(world, pos, Direction.WEST);
        boolean upDown = isConnected(world, pos, Direction.UP) || isConnected(world, pos, Direction.DOWN);

        return getDefaultState()
                .with(NORTH_SOUTH, northSouth)
                .with(EAST_WEST, eastWest && !northSouth)
                .with(UP_DOWN, upDown && !northSouth && !eastWest);
    }

    public void updateBlockStateFromEntity(World world, BlockPos pos) {
        if (world.isClient()) return;

        BlockState newState = getStateFromBlockEntity(world, pos);
        updateBlockState(world, pos, newState);
    }

    public void updateBlockState(World world, BlockPos pos, BlockState newState) {
        if (world.isClient()) return;

        BlockState currentState = world.getBlockState(pos);

        if (!currentState.equals(newState)) {
            world.setBlockState(pos, newState);
        }
    }

    public List<Direction> getConnectedFaces(BlockState state) {
        List<Direction> directions = new ArrayList<>();
        if (state.get(NORTH_SOUTH)) {
            directions.add(Direction.NORTH);
            directions.add(Direction.SOUTH);
        }
        if (state.get(EAST_WEST)) {
            directions.add(Direction.EAST);
            directions.add(Direction.WEST);
        }
        if (state.get(UP_DOWN)) {
            directions.add(Direction.UP);
            directions.add(Direction.DOWN);
        }
        return directions;
    }

    public boolean isConnected(World world, BlockPos pos, Direction facing) {
        return canConnect(world, pos, facing);
    }

    public boolean canConnect(WorldAccess world, BlockPos pos, Direction facing) {
        return world.getBlockState(pos.offset(facing)).getBlock() instanceof TubeConnection;
    }

    @Override
    public boolean canTravelConnect(WorldAccess world, BlockPos posSelf, Direction facing) {
        BlockPos relative = posSelf.offset(facing);
        BlockState otherState = world.getBlockState(relative);
        Block block = otherState.getBlock();
        return block instanceof TubeConnection
               && (!(block instanceof HypertubeBlock hypertubeBlock)
                   || canOtherConnectTo(world, relative, hypertubeBlock, facing));
    }

    private boolean canOtherConnectTo(WorldAccess world, BlockPos otherPos, HypertubeBlock otherTube, Direction facing) {
        List<Direction> connectedFaces = otherTube.getConnectedFaces(otherTube.getState((World) world, otherPos));
        return connectedFaces.isEmpty() || connectedFaces.contains(facing);
    }

    @Override
    public Class<HypertubeBlockEntity> getBlockEntityClass() {
        return HypertubeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends HypertubeBlockEntity> getBlockEntityType() {
        return ModBlockEntities.HYPERTUBE.get();
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos blockPos, BlockState state) {
        return ModBlockEntities.HYPERTUBE.create(blockPos, state);
    }

    @Override
    public void onBreak(@NotNull World level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull PlayerEntity player) {
        onBreak(level, pos, state, player, false);
    }

    private void onBreak(@NotNull World level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull PlayerEntity player, boolean wrenched) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof HypertubeBlockEntity hypertubeEntity))
            return;

        SimpleConnection connectionFrom = hypertubeEntity.getConnectionFrom();
        BezierConnection connectionTo = hypertubeEntity.getConnectionTo();

        int toDrop = 0;
        if (connectionFrom != null) {
            BlockPos otherPos = connectionFrom.pos();
            BlockEntity otherBlock = level.getBlockEntity(otherPos);
            if (otherBlock instanceof HypertubeBlockEntity otherHypertubeEntity
                && otherHypertubeEntity.getConnectionTo() != null) {
                toDrop += (int) otherHypertubeEntity.getConnectionTo().distance() - 1;
                otherHypertubeEntity.setConnectionTo(null);
            }
        }

        if (connectionTo != null) {
            BlockPos otherPos = connectionTo.getToPos().pos();
            BlockEntity otherBlock = level.getBlockEntity(otherPos);
            if (otherBlock instanceof HypertubeBlockEntity otherHypertubeEntity
                && otherHypertubeEntity.getConnectionFrom() != null) {
                toDrop += (int) connectionTo.distance() - 1;
                otherHypertubeEntity.setConnectionFrom(null, null);
            }
        }

        if (!player.isCreative()) {
            if (toDrop != 0 || wrenched) {
                ItemStack stack = new ItemStack(ModBlocks.HYPERTUBE.get(), toDrop + 1);
                if (wrenched)
                    player.getInventory().offerOrDrop(stack);
                else
                    ItemScatterer.spawn(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        super.onBreak(level, pos, state, player);
    }

    @Override
    public void onPlaced(World level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.onPlaced(level, pos, state, placer, stack);
        if (!(placer instanceof PlayerEntity player)) return;
        if (level.isClient()) return;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof HypertubeBlockEntity hypertubeEntity)) return;
        if (!stack.hasGlint()) {
            // [Fabric] Removed 3 arguments from getSoundGroup, couldn't find alternative
            level.playSound(null, pos, getSoundGroup(state).getPlaceSound(), SoundCategory.BLOCKS,
                    1, level.random.nextFloat() * 0.1f + 0.9f);
            return;
        }

        SimpleConnection connectionFrom = ModDataComponent.decodeSimpleConnection(stack);
        if (connectionFrom == null) return;

        Direction finalDirection = RayCastUtils.getDirectionFromHitResult(player, null, true);
        SimpleConnection connectionTo = new SimpleConnection(pos, finalDirection);
        BezierConnection bezierConnection = BezierConnection.of(connectionFrom, connectionTo);

        if (!TubeUtils.checkPlayerPlacingBlockValidation(player, bezierConnection, level)) {
            level.playSound(placer, pos, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.BLOCKS,
                    1, 0.5f);
            return;
        }

        // [Fabric] Removed 3 arguments from getSoundGroup, couldn't find alternative
        level.playSound(null, pos, getSoundGroup(state).getPlaceSound(), SoundCategory.BLOCKS,
                1, level.random.nextFloat() * 0.1f + 0.9f);

        BlockEntity otherBlockEntity = level.getBlockEntity(connectionFrom.pos());
        boolean inverted = false;

        if (otherBlockEntity instanceof HypertubeBlockEntity otherHypertubeEntity) {
            if (otherHypertubeEntity.getConnectionTo() == null) {
                otherHypertubeEntity.setConnectionTo(bezierConnection);
            } else if (otherHypertubeEntity.getConnectionFrom() == null) {
                bezierConnection = bezierConnection.invert();
                connectionTo = bezierConnection.getFromPos();
                otherHypertubeEntity.setConnectionFrom(connectionTo, bezierConnection.getToPos().direction());
                inverted = true;
            } else {
                MessageUtils.sendActionMessage(player, Text.translatable("placement.create_hypertube.invalid_conn").styled(style -> style.withColor(Formatting.RED)), true);
                return;
            }
        }

        if (inverted)
            hypertubeEntity.setConnectionTo(bezierConnection);
        else
            hypertubeEntity.setConnectionFrom(connectionFrom, bezierConnection.getToPos().direction());

        MessageUtils.sendActionMessage(player, Text.empty(), true);
        if (!(level.getBlockState(pos).getBlock() instanceof HypertubeBlock hypertubeBlock)) return;
        hypertubeBlock.updateBlockState(level, pos, hypertubeBlock.getState(List.of(finalDirection)));
    }

    @Override
    public ItemStack getPickStack(BlockView p_49823_, BlockPos p_49824_, BlockState p_49825_) {
        return ModBlocks.HYPERTUBE.asStack();
    }

    @Override
    public boolean isTransparent(@NotNull BlockState state, @NotNull BlockView reader, @NotNull BlockPos pos) {
        return true;
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
    public ActionResult onSneakWrenched(BlockState state, ItemUsageContext context) {
        if (context.getPlayer() == null) return ActionResult.PASS;
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();

        onBreak(world, context.getBlockPos(), state, context.getPlayer(), true);

        if (!(world instanceof ServerWorld))
            return ActionResult.SUCCESS;

        /* BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(world, pos, world.getBlockState(pos), player);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled())
            return ActionResult.SUCCESS; */
        boolean canceled = !PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(world, player, pos, world.getBlockState(pos), null);
        if (canceled) return ActionResult.SUCCESS;

        world.breakBlock(pos, false);
        this.playRemoveSound(world, pos);
        return ActionResult.SUCCESS;
    }
}