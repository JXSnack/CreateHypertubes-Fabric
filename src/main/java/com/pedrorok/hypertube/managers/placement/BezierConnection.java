package com.pedrorok.hypertube.managers.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pedrorok.hypertube.utils.CodecUtils;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * @author Rok, Pedro Lucas nmm. Created on 24/04/2025
 * @project Create Hypertube
 */
public class BezierConnection {

    public static final Codec<BezierConnection> CODEC = RecordCodecBuilder.create(i -> i.group(
            SimpleConnection.CODEC.fieldOf("fromPos").forGetter(BezierConnection::getFromPos),
            SimpleConnection.CODEC.fieldOf("toPos").forGetter(BezierConnection::getToPos),
            Vec3d.CODEC.listOf().fieldOf("curvePoints").forGetter(BezierConnection::getBezierPoints)
    ).apply(i, BezierConnection::new));

    public static final StreamCodec<ByteBuf, BezierConnection> STREAM_CODEC = StreamCodec.composite(
            SimpleConnection.STREAM_CODEC, BezierConnection::getFromPos,
            SimpleConnection.STREAM_CODEC, BezierConnection::getToPos,
            CodecUtils.VEC3_LIST, BezierConnection::getBezierPoints,
            BezierConnection::new
    );


    public static final float MAX_DISTANCE = 80.0f;
    public static final float MAX_ANGLE = 0.6f;

    @Getter
    private final UUID uuid = UUID.randomUUID();
    @Getter
    private final SimpleConnection fromPos;
    @Getter
    private @Nullable SimpleConnection toPos;
    private List<Vec3d> bezierPoints;

    private ResponseDTO valid;
    private final int detailLevel;


    private BezierConnection(SimpleConnection fromPos, SimpleConnection toPos, List<Vec3d> bezierPoints) {
        this(fromPos, toPos, (int) Math.max(3, fromPos.pos().getCenter().distanceTo(toPos.pos().getCenter())));
        this.bezierPoints = bezierPoints;
    }

    public BezierConnection(SimpleConnection fromPos, @Nullable SimpleConnection toPos) {
        this(fromPos, toPos, toPos != null ? (int) Math.max(3, fromPos.pos().getCenter().distanceTo(toPos.pos().getCenter())) : 0);
    }

    public BezierConnection(SimpleConnection fromPos, SimpleConnection toPos, int detailLevel) {
        this.fromPos = fromPos;
        this.toPos = toPos;
        this.detailLevel = detailLevel;
    }

    public List<Vec3d> getBezierPoints() {
        if (bezierPoints != null) return bezierPoints;
        if (toPos == null) return List.of();
        Vec3d fromPos = this.fromPos.pos().getCenter();
        Vec3d toPos = new Vec3d(this.toPos.pos().getX() + 0.5, this.toPos.pos().getY() + 0.5, this.toPos.pos().getZ() + 0.5);
        bezierPoints = calculateBezierCurve(fromPos, this.fromPos.direction(), toPos, detailLevel, getToPos().direction());
        return bezierPoints;
    }

    private List<Vec3d> calculateBezierCurve(Vec3d from, Direction direction, Vec3d toVec, int detailLevel, @Nullable Direction finalDirection) {
        valid = null;
        double distance = from.distanceTo(toVec);

        Vec3d controlPoint1 = createFirstControlPoint(from, direction, distance);
        Vec3d controlPoint2 = createSecondControlPoint(toVec, direction, distance, Vec3d.atLowerCornerOf(finalDirection.getNormal()));

        List<Vec3d> curvePoints = new ArrayList<>();

        for (int i = 0; i <= detailLevel; i++) {
            double t = (double) i / detailLevel;
            Vec3d point = cubicBezier(from, controlPoint1, controlPoint2, toVec, t);
            curvePoints.add(point);
        }

        return curvePoints;
    }

    private Vec3d createFirstControlPoint(Vec3d from, Direction direction, double distance) {
        double controlDistance = distance * 0.4;
        return from.add(
                direction.getStepX() * controlDistance,
                direction.getStepY() * controlDistance,
                direction.getStepZ() * controlDistance
        );
    }

    private Vec3d createSecondControlPoint(Vec3d to, Direction fromDirection, double distance, @Nullable Vec3d finalDirection) {
        if (finalDirection != null) {
            double controlDistance = distance * 0.4;
            return to.subtract(
                    finalDirection.x * controlDistance,
                    finalDirection.y * controlDistance,
                    finalDirection.z * controlDistance
            );
        } else {
            Direction oppositeDirection = fromDirection.getOpposite();
            double controlDistance = distance * 0.4;
            return to.add(
                    oppositeDirection.getStepX() * controlDistance,
                    oppositeDirection.getStepY() * controlDistance,
                    oppositeDirection.getStepZ() * controlDistance
            );
        }
    }

    private Vec3d cubicBezier(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, double t) {
        // B(t) = (1-t)^3 * P0 + 3(1-t)^2 * t * P1 + 3(1-t) * t^2 * P2 + t^3 * P3
        double oneMinusT = 1 - t;
        double oneMinusTCubed = oneMinusT * oneMinusT * oneMinusT;
        double oneMinusTSquared = oneMinusT * oneMinusT;
        double tSquared = t * t;
        double tCubed = tSquared * t;

        double x = oneMinusTCubed * p0.x + 3 * oneMinusTSquared * t * p1.x + 3 * oneMinusT * tSquared * p2.x + tCubed * p3.x;
        double y = oneMinusTCubed * p0.y + 3 * oneMinusTSquared * t * p1.y + 3 * oneMinusT * tSquared * p2.y + tCubed * p3.y;
        double z = oneMinusTCubed * p0.z + 3 * oneMinusTSquared * t * p1.z + 3 * oneMinusT * tSquared * p2.z + tCubed * p3.z;

        return new Vec3d(x, y, z);
    }

    public float getMaxAngleBezierAngle() {
        if (bezierPoints == null) {
            bezierPoints = getBezierPoints();
        }
        float maxAngle = 0;
        Vec3d lastPoint = bezierPoints.get(0);
        for (int i = 1; i < bezierPoints.size() - 1; i++) {
            Vec3d currentPoint = bezierPoints.get(i);
            Vec3d nextPoint = bezierPoints.get(i + 1);

            Vec3d vector1 = currentPoint.subtract(lastPoint);
            Vec3d vector2 = nextPoint.subtract(currentPoint);
            float angle = (float) Math.acos(vector1.dot(vector2) / (vector1.length() * vector2.length()));
            maxAngle = Math.max(maxAngle, angle);
            lastPoint = currentPoint;
        }
        return maxAngle;
    }

    public float distance() {
        if (toPos == null) return 0;
        return (float) fromPos.pos().getCenter().distanceTo(toPos.pos().getCenter());
    }


    public ResponseDTO getValidation() {
        if (valid != null) return valid;
        if (fromPos==null || toPos==null) {
            valid = ResponseDTO.invalid("Both positions must be set.");
            return valid;
        }
        if (getMaxAngleBezierAngle() >= MAX_ANGLE) {
            valid = ResponseDTO.invalid("The angle between points is too high.");
            return valid;
        }
        if (distance() >= MAX_DISTANCE) {
            valid = ResponseDTO.invalid("The distance between points is too high.");
            return valid;
        }
        if (distance() == 0) {
            valid = ResponseDTO.invalid("");
            return valid;
        }
        return ResponseDTO.get(true);
    }

    public static BezierConnection of(SimpleConnection from, @Nullable SimpleConnection toPos) {
        return new BezierConnection(from, toPos);
    }


    @Environment(EnvType.CLIENT)
    public void drawPath(LerpedFloat animation) {
        Vec3d pos1 = fromPos.pos().getCenter();
        int id = 0;
        for (Vec3d bezierPoint : getBezierPoints()) {
            line(uuid, id, pos1, bezierPoint, animation, valid != null && !valid.valid());
            pos1 = bezierPoint;
            id++;
        }
    }

    @Environment(EnvType.CLIENT)
    public static void line(UUID uuid, int id, Vec3d start, Vec3d end, LerpedFloat animation, boolean hasException) {
        int color = Color.mixColors(0xEA5C2B, 0x95CD41, animation.getValue());
        if (hasException) {
            Vec3d diff = end.subtract(start);
            start = start.add(diff.scale(0.2));
            end = start.add(diff.scale(-0.2));
        }
        Outliner.getInstance().showLine(Pair.of(uuid, id), start, end)
                .lineWidth(1 / 8f)
                .disableLineNormals()
                .colored(color);
    }


    public BezierConnection invert() {
        List<Vec3d> newBezier = new ArrayList<>(bezierPoints);
        Collections.reverse(newBezier);
        return new BezierConnection(new SimpleConnection(toPos.pos(), toPos.direction().getOpposite()), fromPos, newBezier);
    }


    @Override
    public String toString() {
        return "BezierConnection{" +
               "fromPos=" + fromPos +
               ", toPos=" + toPos +
               ", isValid=" + valid +
               '}';
    }
}
