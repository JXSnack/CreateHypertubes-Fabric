package com.pedrorok.hypertube.client;

import com.pedrorok.hypertube.HypertubeMod;
import com.pedrorok.hypertube.blocks.IBezierProvider;
import com.pedrorok.hypertube.blocks.blockentities.HypertubeBlockEntity;
import com.pedrorok.hypertube.managers.placement.BezierConnection;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/**
 * @author Rok, Pedro Lucas nmm. Created on 22/05/2025
 * @project Create Hypertube
 */
@Environment(EnvType.CLIENT)
public class BezierTextureRenderer<T extends IBezierProvider> implements BlockEntityRenderer<HypertubeBlockEntity> {

    private static final float TUBE_RADIUS = 0.7F;
    private static final float INNER_TUBE_RADIUS = 0.62F;
    private static final int SEGMENTS_AROUND = 4;

    private static final float TILING_UNIT = 1f;

    private final BlockEntityRendererFactory.Context context;
    private final Identifier textureLocation;

    public BezierTextureRenderer(BlockEntityRendererFactory.Context context) {
        this.context = context;
        this.textureLocation = new Identifier(HypertubeMod.MOD_ID, "textures/block/entity_tube_base.png");
    }

    @Override
    public void render(HypertubeBlockEntity blockEntity, float partialTick, MatrixStack poseStack, VertexConsumerProvider bufferSource,
                       int packedLight, int packedOverlay) {
        BezierConnection connection = blockEntity.getBezierConnection();

        if (connection == null || !connection.getValidation().valid()) {
            return;
        }

        List<Vec3d> bezierPoints = connection.getBezierPoints();
        if (bezierPoints.size() < 2) {
            return;
        }

        poseStack.push();
        Vec3d blockPos = Vec3d.of(blockEntity.getBlockPos());
        poseStack.translate(-blockPos.x, -blockPos.y, -blockPos.z);

        Matrix4f pose = poseStack.peek().getPositionMatrix();
        World level = blockEntity.getWorld();

        VertexConsumer builderExterior = bufferSource.getBuffer(RenderLayer.getEntityTranslucentCull(textureLocation));
        renderTubeSegments(bezierPoints, builderExterior, pose, level, packedLight, packedOverlay, false);

        VertexConsumer builderInterior = bufferSource.getBuffer(RenderLayer.getEntityTranslucent(textureLocation));
        renderTubeSegments(bezierPoints, builderInterior, pose, level, packedLight, packedOverlay, true);

        poseStack.pop();
    }

    private void renderTubeSegments(List<Vec3d> points, VertexConsumer builder, Matrix4f pose, World level, int packedLight, int packedOverlay, boolean isInterior) {
        float currentDistance = 0;
        float radius = isInterior ? INNER_TUBE_RADIUS : TUBE_RADIUS;

        for (int i = 0; i < points.size() - 1; i++) {
            Vec3d current = points.get(i);
            Vec3d next = points.get(i + 1);

            Vec3d direction = next.subtract(current);
            float segmentLength = (float) direction.length();

            if (segmentLength < 0.001f) continue;

            Vec3d dirNormalized = direction.normalize();

            Vector3f dirVector = new Vector3f((float) dirNormalized.x, (float) dirNormalized.y, (float) dirNormalized.z);
            Vector3f perpA = findPerpendicularVector(dirVector);
            Vector3f perpB = new Vector3f();
            perpA.cross(dirVector, perpB);
            perpB.normalize();

            float uStart = currentDistance / TILING_UNIT;
            float uEnd = (currentDistance + segmentLength) / TILING_UNIT;

            for (int j = 0; j < SEGMENTS_AROUND; j++) {
                float angle1 = (float) (j * 2 * Math.PI / SEGMENTS_AROUND) + (float) (Math.PI / 4);
                float angle2 = (float) ((j + 1) * 2 * Math.PI / SEGMENTS_AROUND) + (float) (Math.PI / 4);

                Vector3f offsetStart1 = getOffset(perpA, perpB, angle1, radius);
                Vector3f offsetStart2 = getOffset(perpA, perpB, angle2, radius);

                float v1 = j / (float) SEGMENTS_AROUND;
                float v2 = (j + 1) / (float) SEGMENTS_AROUND;

                if (!isInterior) {
                    addVertex(builder, pose, current, offsetStart1, uStart, v1, packedLight, packedOverlay, false);
                    addVertex(builder, pose, next, offsetStart1, uEnd, v1, packedLight, packedOverlay, false);
                    addVertex(builder, pose, next, offsetStart2, uEnd, v2, packedLight, packedOverlay, false);
                    addVertex(builder, pose, current, offsetStart2, uStart, v2, packedLight, packedOverlay, false);
                }
                addVertex(builder, pose, current, offsetStart2, uStart, v2, packedLight, packedOverlay, true);
                addVertex(builder, pose, next, offsetStart2, uEnd, v2, packedLight, packedOverlay, true);
                addVertex(builder, pose, next, offsetStart1, uEnd, v1, packedLight, packedOverlay, true);
                addVertex(builder, pose, current, offsetStart1, uStart, v1, packedLight, packedOverlay, true);
            }

            currentDistance += segmentLength;
        }
    }

    private void addVertex(VertexConsumer builder, Matrix4f pose,
                           Vec3d pos, Vector3f offset, float u, float v, int light, int overlay, boolean invertLight) {
        float x = (float) pos.x + offset.x;
        float y = (float) pos.y + offset.y;
        float z = (float) pos.z + offset.z;

        float radius = invertLight ? INNER_TUBE_RADIUS : TUBE_RADIUS;

        float normalMultiplier = invertLight ? -0.8f : 0.8f;

        float nx = (offset.x / radius) * normalMultiplier;
        float ny = (offset.y / radius) * normalMultiplier;
        float nz = (offset.z / radius) * normalMultiplier;

        builder.vertex(pose, x, y, z)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(overlay)
                .light(light & 0xFFFF, light >> 16)
                .normal(nx, ny, nz)
                .next();
    }

    private Vector3f findPerpendicularVector(Vector3f vec) {
        Vector3f perpendicular;

        if (Math.abs(vec.x) < Math.abs(vec.y) && Math.abs(vec.x) < Math.abs(vec.z)) {
            perpendicular = new Vector3f(1, 0, 0);
        } else if (Math.abs(vec.y) < Math.abs(vec.z)) {
            perpendicular = new Vector3f(0, 1, 0);
        } else {
            perpendicular = new Vector3f(0, 0, 1);
        }

        Vector3f result = new Vector3f();
        vec.cross(perpendicular, result);
        return result.normalize();
    }

    private Vector3f getOffset(Vector3f perpA, Vector3f perpB, float angle, float radius) {
        return new Vector3f(
                (MathHelper.cos(angle) * perpA.x + MathHelper.sin(angle) * perpB.x) * radius,
                (MathHelper.cos(angle) * perpA.y + MathHelper.sin(angle) * perpB.y) * radius,
                (MathHelper.cos(angle) * perpA.z + MathHelper.sin(angle) * perpB.z) * radius
        );
    }

/*    private float calculateTotalLength(List<Vec3> points) {
        float length = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            length += points.get(i).distanceTo(points.get(i + 1));
        }
        return length;
    }*/

    @Override
    public boolean rendersOutsideBoundingBox(HypertubeBlockEntity blockEntity) {
        return true;
    }

    @Override
    public boolean isInRenderDistance(HypertubeBlockEntity p_173568_, Vec3d p_173569_) {
        return true;
    }

}