package com.pedrorok.hypertube.utils;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * @author Rok, Pedro Lucas nmm. Created on 25/04/2025
 * @project Create Hypertube
 */
public class RayCastUtils {

    public static <T extends Block> Direction getDirectionFromHitResult(PlayerEntity player, @Nullable T filter) {
        return getDirectionFromHitResult(player, filter, false);
    }

    public static <T extends Block> Direction getDirectionFromHitResult(PlayerEntity player, @Nullable T filter, boolean ignoreFilter) {
        HitResult hitResult = player.raycast(5, 0, false);
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return player.getHorizontalFacing().getOpposite();
        }
        BlockHitResult blockHitResult = (BlockHitResult) hitResult;
        World level = player.getWorld();
        if ((filter != null && !level.getBlockState(blockHitResult.getBlockPos()).isOf(filter)) || ignoreFilter) {
            return player.getHorizontalFacing().getOpposite();
        }
        return blockHitResult.getSide().getOpposite();
    }
}
