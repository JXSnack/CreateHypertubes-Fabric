package com.pedrorok.hypertube.blocks.blockentities;

import com.pedrorok.hypertube.blocks.HypertubeBlock;
import com.pedrorok.hypertube.blocks.IBezierProvider;
import com.pedrorok.hypertube.managers.placement.BezierConnection;
import com.pedrorok.hypertube.managers.placement.SimpleConnection;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Rok, Pedro Lucas nmm. Created on 24/04/2025
 * @project Create Hypertube
 */
@Getter
public class HypertubeBlockEntity extends BlockEntity implements IBezierProvider {

    private BezierConnection connectionTo;
    private SimpleConnection connectionFrom;

    public HypertubeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setConnectionTo(BezierConnection connection) {
        this.connectionTo = connection;
        if (world != null && !world.isClient()) {
            if (world.getBlockState(pos).getBlock() instanceof HypertubeBlock hypertubeBlock) {
                hypertubeBlock.updateBlockStateFromEntity(world, pos);
            }
        }
        markDirty();
        sync();
    }

    public void setConnectionFrom(SimpleConnection connectionFrom, Direction direction) {
        this.connectionFrom = connectionFrom;

        if (world != null && !world.isClient()) {
            if (world.getBlockState(pos).getBlock() instanceof HypertubeBlock hypertubeBlock) {
                hypertubeBlock.updateBlockStateFromEntity(world, pos);
                if (direction != null) {
                    BlockState state = hypertubeBlock.getState(List.of(direction));
                    hypertubeBlock.updateBlockState(world, pos, state);
                }
            }
        }
        markDirty();
        sync();
    }

    public boolean isConnected() {
        return connectionTo != null || connectionFrom != null;
    }

    public void sync() {
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_ALL);
        }
    }


    @Override
    protected void writeNbt(NbtCompound tag) {
        super.writeNbt(tag);
        writeConnection(tag);
    }

    private void writeConnection(NbtCompound tag) {
        if (connectionTo != null) {
            tag.put("ConnectionTo", BezierConnection.CODEC.encodeStart(NbtOps.INSTANCE, connectionTo)
                    .get().orThrow());
        }
        if (connectionFrom != null) {
            tag.put("ConnectionFrom", SimpleConnection.CODEC.encodeStart(NbtOps.INSTANCE, connectionFrom)
                    .get().orThrow());
        }
    }

    @Override
    public void readNbt(@NotNull NbtCompound tag) {
        super.readNbt(tag);

        if (tag.contains("ConnectionTo")) {
            this.connectionTo = BezierConnection.CODEC.parse(NbtOps.INSTANCE, tag.get("ConnectionTo"))
                    .get().orThrow();
        }
        if (tag.contains("ConnectionFrom")) {
            this.connectionFrom = SimpleConnection.CODEC.parse(NbtOps.INSTANCE, tag.get("ConnectionFrom"))
                    .get().orThrow();
        }
    }

    public List<Direction> getFacesConnectable() {
        // Se já tem conexões em ambas as direções, não pode mais conectar
        if (connectionTo != null && connectionFrom != null) return List.of();

        // Primeiro, determinar as direções possíveis baseadas no estado do bloco
        List<Direction> possibleDirections = new ArrayList<>();

        boolean eastWest = Boolean.TRUE.equals(getCachedState().get(HypertubeBlock.EAST_WEST));
        if (eastWest) {
            possibleDirections.addAll(List.of(Direction.EAST, Direction.WEST));
        }

        boolean upDown = Boolean.TRUE.equals(getCachedState().get(HypertubeBlock.UP_DOWN));
        if (upDown) {
            possibleDirections.addAll(List.of(Direction.UP, Direction.DOWN));
        }

        boolean northSouth = Boolean.TRUE.equals(getCachedState().get(HypertubeBlock.NORTH_SOUTH));
        if (northSouth) {
            possibleDirections.addAll(List.of(Direction.NORTH, Direction.SOUTH));
        }

        // Se nenhuma propriedade direcional estiver ativa, permite todas as direções
        if (possibleDirections.isEmpty()) {
            possibleDirections.addAll(List.of(Direction.values()));
        }

        // Agora, remover direções que não podem ser conectadas
        possibleDirections.removeIf(direction -> {
            // Remove se já tem um HypertubeBlock adjacente
            if (world.getBlockState(pos.offset(direction)).getBlock() instanceof HypertubeBlock) {
                return true;
            }

            // Remove se já tem uma conexão ativa saindo nessa direção
            if (connectionTo != null && connectionTo.getFromPos().direction().equals(direction)) {
                return true;
            }

            // Remove se já tem uma conexão ativa chegando dessa direção
            if (connectionFrom != null) {
                BlockEntity blockEntity = world.getBlockEntity(connectionFrom.pos());
                if (blockEntity instanceof HypertubeBlockEntity hypertubeBlockEntity
                    && hypertubeBlockEntity.getConnectionTo() != null
                    && hypertubeBlockEntity.getConnectionTo().getToPos().pos().equals(this.pos)
                    && hypertubeBlockEntity.getConnectionTo().getToPos().direction().getOpposite().equals(direction)) {
                    return true;
                }
            }

            return false;
        });

        return possibleDirections;
    }

    @Override
    public @NotNull NbtCompound toInitialChunkDataNbt() {
        NbtCompound tag = super.toInitialChunkDataNbt();
        writeNbt(tag);
        return tag;
    }

    @Override
    public @NotNull BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    // Unneeded in Fabric
    /* @Override
    public void onDataPacket(ClientConnection net, BlockEntityUpdateS2CPacket pkt) {
        NbtCompound tag = pkt.getNbt();
        if (tag == null) {
            return;
        }
        readNbt(tag);
    } */

    @Override
    public BezierConnection getBezierConnection() {
        return connectionTo;
    }

    public Box getRenderBounds() {
        return new Box(pos).expand(512);
    }

    // For IBezierProvider, BlockEntity on Forge has the getPos method named getBlockPos
    @Override
    public BlockPos getBlockPos() {
        return getPos();
    }
}