package com.pedrorok.hypertube.camera;

import com.pedrorok.hypertube.config.ClientConfig;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * @author Rok, Pedro Lucas nmm. Created on 22/04/2025
 * @project Create Hypertube
 */
@Environment(EnvType.CLIENT)
public class DetachedCameraController {

    private static DetachedCameraController INSTANCE;

    public static DetachedCameraController get() {
        if (INSTANCE == null) {
            INSTANCE = new DetachedCameraController();
        }
        return INSTANCE;
    }

    @Getter
    private float yaw = 0;
    @Getter
    private float pitch = 0;

    @Getter
    private Vec3d currentPos = Vec3d.ZERO;

    private Vec3d targetPos = Vec3d.ZERO;
    @Getter
    private float targetYaw = 0;
    @Getter
    private float targetPitch = 0;

    private static final double SMOOTHING = 0.1;
    private static final double SMOOTHING_ROTATION = 0.1;

    private float lastMouseMov = 0;

    @Getter
    @Setter
    private boolean detached = false;

    private DetachedCameraController() {
    }

    public void startCamera(Entity renderViewEntity) {
        Vec3d cameraPos = getRelativeCameraPos(renderViewEntity);
        this.currentPos = cameraPos;
        this.targetPos = cameraPos;
        this.lastMouseMov = 0;
        this.yaw = this.targetYaw = renderViewEntity.getYaw();
        this.pitch = this.targetPitch = 30;
    }

    public void updateCameraRotation(float deltaYaw, float deltaPitch, boolean isCamera) {
        this.targetYaw += deltaYaw;
        this.targetPitch += deltaPitch;


        if (lastMouseMov != 0) {
            lastMouseMov = Math.max(0, lastMouseMov - 0.02f);
        }
        if (isCamera && deltaYaw != 0) {
            lastMouseMov = 2;
        }

        this.targetPitch = MathHelper.clamp(this.targetPitch, -90, 90);
    }

    private float getCameraYaw(Vec3d entityPos, Vec3d cameraPos) {
        Vec3d cameraToPlayerNormal = cameraPos.subtract(entityPos).multiply(1, 0, 1).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(cameraToPlayerNormal.z, cameraToPlayerNormal.x)) + 90;
        return (((yaw - this.yaw + 540) % 360) - 180) * (1 - Math.min(lastMouseMov, 1));
    }

    private float getCameraPitch() {
        // targe 30 degress ignoring player pos, only stay in 30 getting the actual pitch
        return (((30 - this.pitch + 540) % 360) - 180) * (1 - Math.min(lastMouseMov, 1));
    }

    private Vec3d getRelativeCameraPos(Entity renderViewEntity) {
        Vec3d deltaMovement = renderViewEntity.getVelocity();
        return renderViewEntity
                .getPos()
                .subtract(deltaMovement.multiply(8, 8, 8))
                .add(0, 3, 0);
    }

    public void tickCamera(Entity renderViewEntity) {
        Vec3d entityPos = renderViewEntity.getPos();
        Vec3d relativeCameraPos = getRelativeCameraPos(renderViewEntity);

        updateCameraRotation(getCameraYaw(entityPos, relativeCameraPos) * 0.1f, getCameraPitch() * 0.1f, false);

        updateTargetPosition(relativeCameraPos);
        tickCameraPosRot();
    }

    public void updateTargetPosition(Vec3d pos) {
        this.targetPos = pos;
    }

    public void tickCameraPosRot() {
        this.currentPos = this.currentPos.lerp(this.targetPos, SMOOTHING);
        this.yaw = (float) MathHelper.lerp(SMOOTHING_ROTATION, this.yaw, this.targetYaw);
        this.pitch = (float) MathHelper.lerp(SMOOTHING_ROTATION, this.pitch, this.targetPitch);
    }


    private static double lastMouseX = 0;
    private static double lastMouseY = 0;

    public static void cameraTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if ((mc.options.getPerspective().isFirstPerson() && ClientConfig.get().ALLOW_FPV_INSIDE_TUBE.get())
            || mc.isPaused()
            || !mc.isWindowFocused())
            return;

        Mouse mouse = mc.mouse;
        double dx = mouse.getX() - lastMouseX;
        double dy = mouse.getY() - lastMouseY;

        double sensitivity = mc.options.getMouseSensitivity().getValue();
        double factor = sensitivity * 0.3 + 0.1;
        factor = factor * factor * factor * 8.0;
        DetachedCameraController.get().updateCameraRotation((float) (dx * factor), (float) (dy * factor), true);
        lastMouseX = mc.mouse.getX();
        lastMouseY = mc.mouse.getY();
    }
}
