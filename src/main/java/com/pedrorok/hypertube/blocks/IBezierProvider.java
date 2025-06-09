package com.pedrorok.hypertube.blocks;

import com.pedrorok.hypertube.managers.placement.BezierConnection;
import net.minecraft.util.math.BlockPos;

/**
 * @author Rok, Pedro Lucas nmm. Created on 21/04/2025
 * @project Create Hypertube
 */
public interface IBezierProvider {

    BezierConnection getBezierConnection();

    BlockPos getBlockPos();
}