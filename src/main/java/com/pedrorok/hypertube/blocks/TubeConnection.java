package com.pedrorok.hypertube.blocks;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;

/**
 * @author Rok, Pedro Lucas nmm. Created on 21/04/2025
 * @project Create Hypertube
 */
public interface TubeConnection {


    boolean canTravelConnect(WorldAccess world, BlockPos pos, Direction facing);
}
