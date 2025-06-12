package com.pedrorok.hypertube.utils;

import com.pedrorok.hypertube.blocks.blockentities.HypertubeBlockEntity;
import com.pedrorok.hypertube.items.HypertubeItem;
import com.pedrorok.hypertube.managers.placement.BezierConnection;
import com.pedrorok.hypertube.managers.placement.ResponseDTO;
import com.pedrorok.hypertube.managers.placement.SimpleConnection;
import com.pedrorok.hypertube.registry.ModBlocks;
import com.pedrorok.hypertube.registry.ModDataComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Rok, Pedro Lucas nmm. Created on 11/06/2025
 * @project Create Hypertube
 */
public class TubeUtils {


    public static ResponseDTO checkClickedHypertube(World level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof HypertubeBlockEntity tubeEntity
            && !tubeEntity.getFacesConnectable().contains(direction)) {
            return ResponseDTO.invalid("placement.create_hypertube.cant_conn_to_face");
        }
        return ResponseDTO.get(true);
    }

    public static boolean checkPlayerPlacingBlock(@NotNull PlayerEntity player, World level, BlockPos pos) {

        ItemStack itemInHand = player.getStackInHand(Hand.MAIN_HAND);
        if (itemInHand.getItem() != ModBlocks.HYPERTUBE.asItem()) {
            return true;
        }
        if (!itemInHand.hasGlint()) {
            return true;
        }

        SimpleConnection connectionFrom = ModDataComponent.decodeSimpleConnection(itemInHand);

        Direction finalDirection = RayCastUtils.getDirectionFromHitResult(player, null, true);
        SimpleConnection connectionTo = new SimpleConnection(pos, finalDirection);
        BezierConnection bezierConnection = BezierConnection.of(connectionFrom, connectionTo);

        return checkPlayerPlacingBlockValidation(player, bezierConnection, level);
    }

    public static boolean checkPlayerPlacingBlockValidation(PlayerEntity player, @NotNull BezierConnection bezierConnection, World level) {
        ResponseDTO validation = bezierConnection.getValidation();
        if (validation.valid()) {
            validation = checkSurvivalItems(player, (int) bezierConnection.distance(), true);
        }

        if (validation.valid()) {
            validation = checkBlockCollision(level, bezierConnection);
        }

        if (!validation.valid()) {
            MessageUtils.sendActionMessage(player, validation.getMessageComponent());
            return false;
        }
        HypertubeItem.clearConnection(player.getStackInHand(Hand.MAIN_HAND));

        checkSurvivalItems(player, (int) bezierConnection.distance() + 1, false);
        return true;
    }


    private static final float CHECK_DISTANCE_THRESHOLD = 0.4f;

    public static ResponseDTO checkBlockCollision(@NotNull World level, @NotNull BezierConnection bezierConnection) {
        List<Vec3d> positions = new ArrayList<>(bezierConnection.getBezierPoints());
        positions.remove(positions.size() -1);
        positions.remove(0);

        for (int i = 1; i < positions.size() - 1; i++) {
            Vec3d pos = positions.get(i);
            if (hasCollision(level, pos) ||
                hasCollision(level, pos.add(CHECK_DISTANCE_THRESHOLD, 0, 0)) ||
                hasCollision(level, pos.add(0, 0, CHECK_DISTANCE_THRESHOLD)) ||
                hasCollision(level, pos.add(CHECK_DISTANCE_THRESHOLD, 0, CHECK_DISTANCE_THRESHOLD)) ||
                hasCollision(level, pos.add(-CHECK_DISTANCE_THRESHOLD, 0, 0)) ||
                hasCollision(level, pos.add(0, 0, -CHECK_DISTANCE_THRESHOLD)) ||
                hasCollision(level, pos.add(-CHECK_DISTANCE_THRESHOLD, 0, -CHECK_DISTANCE_THRESHOLD))) {
                return ResponseDTO.invalid("placement.create_hypertube.block_collision");
            }
        }
        return ResponseDTO.get(true);
    }

    private static boolean hasCollision(World level, Vec3d pos) {
        BlockPos blockPos = BlockPos.ofFloored(pos);
        boolean hasCollision = !level.getBlockState(blockPos).getCollisionShape(level, blockPos).isEmpty();
        if (hasCollision && level.isClient) {
            BezierConnection.outlineBlocks(blockPos);
        }
        return hasCollision;
    }


    public static ResponseDTO checkSurvivalItems(@NotNull PlayerEntity player, int neededTubes, boolean simulate) {
        if (!player.isCreative()
            && !checkPlayerInventory(player, neededTubes, simulate)) {
            return ResponseDTO.invalid("placement.create_hypertube.no_enough_tubes");
        }
        return ResponseDTO.get(true);
    }

    private static boolean checkPlayerInventory(@NotNull PlayerEntity player, int neededTubes, boolean simulate) {
        int foundTubes = 0;

        PlayerInventory inv = player.getInventory();
        int size = inv.size();
        for (int j = 0; j <= size + 1; j++) {
            int i = j;
            boolean offhand = j == size + 1;
            if (j == size)
                i = inv.selectedSlot;
            else if (offhand)
                i = 0;
            else if (j == inv.selectedSlot)
                continue;

            ItemStack stackInSlot = (offhand ? inv.offHand : inv.main).get(i);
            boolean isTube = ModBlocks.HYPERTUBE.asStack().isOf(stackInSlot.getItem());
            if (!isTube)
                continue;
            if (foundTubes >= neededTubes)
                continue;

            int count = stackInSlot.getCount();

            if (!simulate) {
                int remainingItems =
                        count - Math.min(neededTubes - foundTubes, count);
                ItemStack newItem = stackInSlot.copyWithCount(remainingItems);
                if (offhand)
                    player.setStackInHand(Hand.OFF_HAND, newItem);
                else
                    inv.setStack(i, newItem);
            }

            foundTubes += count;
        }
        return foundTubes >= neededTubes;
    }

}
