package com.pedrorok.hypertube.blocks.blockentities;

import com.pedrorok.hypertube.blocks.HyperEntranceBlock;
import com.pedrorok.hypertube.managers.TravelManager;
import com.pedrorok.hypertube.managers.sound.TubeSoundManager;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Rok, Pedro Lucas nmm. Created on 21/04/2025
 * @project Create Hypertube
 */
public class HyperEntranceBlockEntity extends KineticBlockEntity {

    private static final float RADIUS = 1.0f;

    private static final float SPEED_TO_START = 16;

    private final UUID tubeSoundId = UUID.randomUUID();

    public HyperEntranceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }


    @Override
    protected void write(NbtCompound compound, boolean clientPacket) {
        super.write(compound, clientPacket);
    }

    @Override
    protected void read(NbtCompound compound, boolean clientPacket) {
        super.read(compound, clientPacket);
    }

    @Override
    public void remove() {
        if (world.isClient) {
            removeClient();
        }
        super.remove();
    }

    @Environment(EnvType.CLIENT)
    private void removeClient() {
        TubeSoundManager.getAmbientSound(tubeSoundId).stopSound();
        TubeSoundManager.removeAmbientSound(tubeSoundId);
    }

    @Override
    public void tick() {
        super.tick();
        if (world.isClient) {
            tickClient();
            return;
        }
        BlockState state = this.getCachedState();
        BlockPos pos = this.getPos();

        float actualSpeed = Math.abs(this.getSpeed());
        Boolean isOpen = state.get(HyperEntranceBlock.OPEN);

        if (actualSpeed < SPEED_TO_START) {
            if (isOpen) {
                world.setBlockState(pos, state.with(HyperEntranceBlock.OPEN, false), 3);
            }
            return;
        }

        Optional<ServerPlayerEntity> nearbyPlayer = getNearbyPlayers((ServerWorld) world, pos.toCenterPos());
        if (nearbyPlayer.isEmpty()) {
            if (isOpen) {
                world.setBlockState(pos, state.with(HyperEntranceBlock.OPEN, false), 3);
            }
            return;
        }
        if (!isOpen) {
            world.setBlockState(pos, state.with(HyperEntranceBlock.OPEN, true), 3);
        }

        Optional<ServerPlayerEntity> inRangePlayer = getInRangePlayers((ServerWorld) world, pos.toCenterPos(), state.get(HyperEntranceBlock.FACING));
        if (inRangePlayer.isEmpty()) return;

        ServerPlayerEntity player = inRangePlayer.get();
        if (player.isSneaking()) return;
        TravelManager.tryStartTravel(player, pos, state, actualSpeed / 512);
    }


    @Environment(EnvType.CLIENT)
    private void tickClient() {

        // this is just for sound
        BlockState state = this.getCachedState();
        BlockPos pos = this.getPos();

        float actualSpeed = Math.abs(this.getSpeed());

        TubeSoundManager.TubeAmbientSound sound = TubeSoundManager.getAmbientSound(tubeSoundId);
        if (actualSpeed < SPEED_TO_START) {
            sound.tickClientPlayerSounds();
            return;
        }

        boolean isOpen = state.get(HyperEntranceBlock.OPEN);

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        Vec3d source = pos.toCenterPos();
        Vec3d listener = player.getPos();

        Vec3d worldDirection = source.subtract(listener).normalize();

        Vec3d forward = player.getRotationVector().normalize();
        Vec3d up = player.getOppositeRotationVector(1.0F).normalize();
        Vec3d right = forward.crossProduct(up).normalize();

        double x = worldDirection.dotProduct(right);
        double y = worldDirection.dotProduct(up);
        double z = worldDirection.dotProduct(forward);

        Vec3d rotatedDirection = new Vec3d(x, y, z).normalize();

        double distance = player.squaredDistanceTo(source);

        sound.enableClientPlayerSound(
                player,
                rotatedDirection,
                distance,
                isOpen
        );
    }


    private Optional<ServerPlayerEntity> getInRangePlayers(ServerWorld level, Vec3d centerPos, Direction facing) {
        return level.getPlayers().stream()
                .filter(player -> player.getBoundingBox()
                        .expand(RADIUS - 0.25)
                        .contains(centerPos.add(Vec3d.of(facing.getOpposite().getVector()))))
                .findFirst();
    }

    private Optional<ServerPlayerEntity> getNearbyPlayers(ServerWorld level, Vec3d centerPos) {
        return level.getPlayers().stream()
                .filter(player -> player.getBoundingBox()
                        .expand(RADIUS * 3)
                        .contains(centerPos))
                .findFirst();
    }

    @Override
    public boolean addToGoggleTooltip(List<Text> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        IRotate.SpeedLevel.getFormattedSpeedText(speed, speed < 16)
                .forGoggles(tooltip);
        return true;
    }
}
