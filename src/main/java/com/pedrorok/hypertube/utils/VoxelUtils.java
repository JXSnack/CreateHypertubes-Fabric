package com.pedrorok.hypertube.utils;

import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

/**
 * @author Rok, Pedro Lucas nmm. Created on 21/04/2025
 * @project Create Hypertube
 */
public class VoxelUtils {

    public static VoxelShape combine(VoxelShape... shapes) {
        if (shapes.length == 0) {
            return VoxelShapes.empty();
        }
        VoxelShape combined = shapes[0];

        for (int i = 1; i < shapes.length; i++) {
            combined = VoxelShapes.union(combined, shapes[i]);
        }

        return combined;
    }

    public static VoxelShape empty() {
        return VoxelShapes.empty();
    }
}
