package com.pedrorok.hypertube.client.renderer;

import com.pedrorok.hypertube.blocks.HyperEntranceBlock;
import com.pedrorok.hypertube.blocks.blockentities.HyperEntranceBlockEntity;
import com.pedrorok.hypertube.registry.ModPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;

/**
 * @author Rok, Pedro Lucas nmm. Created on 02/06/2025
 * @project Create Hypertube
 */
public class EntranceBlockEntityRenderer extends KineticBlockEntityRenderer<HyperEntranceBlockEntity> {

    public EntranceBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(HyperEntranceBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
                              int light, int overlay) {

        BlockState blockState = be.getCachedState();
        if (!(blockState.getBlock() instanceof HyperEntranceBlock)) {
            return;
        }

        Direction facing = blockState.get(HyperEntranceBlock.FACING);
        SuperByteBuffer cogwheelModel = CachedBufferer.partialFacingVertical(ModPartialModels.COGWHEEL_HOLE, blockState, facing);

        float angle = getAngleForTe(be, be.getPos(), facing.getAxis());
        Direction.Axis rotationAxisOf = getRotationAxisOf(be);


        kineticRotationTransform(cogwheelModel, be, rotationAxisOf, angle, light);
        cogwheelModel.renderInto(ms, buffer.getBuffer(RenderLayer.getSolid()));
    }
}