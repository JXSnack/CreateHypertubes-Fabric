package com.pedrorok.hypertube.managers.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Pair;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
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

    public static final float MAX_DISTANCE = 40.0f;
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
        this(fromPos, toPos, (int) Math.max(3, fromPos.pos().toCenterPos().distanceTo(toPos.pos().toCenterPos())));
        this.bezierPoints = bezierPoints;
    }

    public BezierConnection(SimpleConnection fromPos, @Nullable SimpleConnection toPos) {
        this(fromPos, toPos, toPos != null ? (int) Math.max(3, fromPos.pos().toCenterPos().distanceTo(toPos.pos().toCenterPos())) : 0);
    }

    public BezierConnection(SimpleConnection fromPos, SimpleConnection toPos, int detailLevel) {
        this.fromPos = fromPos;
        this.toPos = toPos;
        this.detailLevel = detailLevel;
    }

    public List<Vec3d> getBezierPoints() {
        if (bezierPoints != null) return bezierPoints;
        if (toPos == null) return List.of();
        Vec3d fromPos = this.fromPos.pos().toCenterPos();
        Vec3d toPos = new Vec3d(this.toPos.pos().getX() + 0.5, this.toPos.pos().getY() + 0.5, this.toPos.pos().getZ() + 0.5);
        bezierPoints = calculateBezierCurve(fromPos, this.fromPos.direction(), toPos, detailLevel, getToPos().direction());
        return bezierPoints;
    }

    private List<Vec3d> calculateBezierCurve(Vec3d from, Direction direction, Vec3d toVec, int detailLevel, @Nullable Direction finalDirection) {
        valid = null;
        double distance = from.distanceTo(toVec);

        Vec3d controlPoint1 = createFirstControlPoint(from, direction, distance);
        Vec3d controlPoint2 = createSecondControlPoint(toVec, direction, distance, Vec3d.of(finalDirection.getVector()));

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
                direction.getOffsetX() * controlDistance,
                direction.getOffsetY() * controlDistance,
                direction.getOffsetZ() * controlDistance
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
                    oppositeDirection.getOffsetX() * controlDistance,
                    oppositeDirection.getOffsetY() * controlDistance,
                    oppositeDirection.getOffsetZ() * controlDistance
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
        // THIS IS TO PREVENT FROM PLACING BACK
        Vec3d first = getBezierPoints().get(0);
        Vec3d second = getBezierPoints().get(1);
        Direction direction = fromPos.direction();
        Vec3d firstDirection = new Vec3d(direction.getOffsetX(), direction.getOffsetY(), direction.getOffsetZ());
        Vec3d secondDirection = second.subtract(first).normalize();
        float initialAngle = (float) Math.acos(firstDirection.dotProduct(secondDirection) / (firstDirection.length() * secondDirection.length()));
        if (initialAngle >= 2.) {
            return initialAngle;
        }
        // END OF PREVENTION

        return getMaxAngle();
    }

    private float getMaxAngle() {
        float maxAngle = 0;
        Vec3d lastPoint = bezierPoints.get(0);
        for (int i = 1; i < bezierPoints.size() - 1; i++) {
            Vec3d currentPoint = bezierPoints.get(i);
            Vec3d nextPoint = bezierPoints.get(i + 1);

            Vec3d vector1 = currentPoint.subtract(lastPoint);
            Vec3d vector2 = nextPoint.subtract(currentPoint);
            float angle = (float) Math.acos(vector1.dotProduct(vector2) / (vector1.length() * vector2.length()));
            maxAngle = Math.max(maxAngle, angle);
            lastPoint = currentPoint;
        }
        return maxAngle;
    }

    public float distance() {
        if (toPos == null) return 0;
        return (float) fromPos.pos().toCenterPos().distanceTo(toPos.pos().toCenterPos());
    }


    public ResponseDTO getValidation() {
        if (valid != null) return valid;
        if (fromPos == null || toPos == null) {
            valid = ResponseDTO.invalid("placement.create_hypertube.no_valid_points");
            return valid;
        }
        if (getMaxAngleBezierAngle() >= MAX_ANGLE) {
            valid = ResponseDTO.invalid("placement.create_hypertube.angle_too_high");
            return valid;
        }
        if (distance() >= MAX_DISTANCE) {
            valid = ResponseDTO.invalid("placement.create_hypertube.distance_too_high");
            return valid;
        }
        if (distance() <= 1) {
            valid = ResponseDTO.invalid();
            return valid;
        }
        return ResponseDTO.get(true);
    }

    public static BezierConnection of(SimpleConnection from, @Nullable SimpleConnection toPos) {
        return new BezierConnection(from, toPos);
    }


    @Environment(EnvType.CLIENT)
    public void drawPath(LerpedFloat animation, boolean isValid) {
        Vec3d pos1 = fromPos.pos().toCenterPos();
        int id = 0;
        for (Vec3d bezierPoint : getBezierPoints()) {
            line(uuid, id, pos1, bezierPoint, animation, !isValid);
            pos1 = bezierPoint;
            id++;
        }
    }

    @Environment(EnvType.CLIENT)
    public static void line(UUID uuid, int id, Vec3d start, Vec3d end, LerpedFloat animation, boolean hasException) {
        int color = Color.mixColors(0xEA5C2B, 0x95CD41, animation.getValue());
        if (hasException) {
            Vec3d diff = end.subtract(start);
            start = start.add(diff.multiply(0.2));
            end = start.add(diff.multiply(-0.2));
        }
        CreateClient.OUTLINER.showLine(Pair.of(uuid, id), start, end)
                .lineWidth(1 / 8f)
                .disableLineNormals()
                .colored(color);
    }

    @Environment(EnvType.CLIENT)
    public static void outlineBlocks(BlockPos pos) {
        CreateClient.OUTLINER.showAABB(pos.asLong(), new Box(pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1,
                        pos.getX() , pos.getY(), pos.getZ()))
                .colored(0xEA5C2B)
                .lineWidth(1 / 8f)
                .disableLineNormals();
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
