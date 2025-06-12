package com.pedrorok.hypertube.mixin;

import com.pedrorok.hypertube.camera.DetachedCameraController;
import com.pedrorok.hypertube.config.ClientConfig;
import com.pedrorok.hypertube.managers.TravelManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Rok, Pedro Lucas nmm. Created on 22/04/2025
 * @project Create Hypertube
 */
@Mixin(Camera.class)
public class CameraMixin {

    @Shadow
    private boolean thirdPerson;

    @Shadow private Entity focusedEntity;

    @Unique
    public void createHypertube$setDetachedExternal(boolean newDetached) {
        this.thirdPerson = newDetached;
    }

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void onSetup(BlockView p_90576_, Entity renderViewEntity, boolean isFrontView, boolean flipped, float PartialTicks, CallbackInfo ci) {
        GameOptions options = MinecraftClient.getInstance().options;
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (renderViewEntity != player) return;
        if (!TravelManager.hasHyperTubeData(renderViewEntity) || (
                options.getPerspective().isFirstPerson() && ClientConfig.get().ALLOW_FPV_INSIDE_TUBE.get())) {

            if (DetachedCameraController.get().isDetached()) {
                renderViewEntity.setYaw(DetachedCameraController.get().getYaw());
                renderViewEntity.setPitch(DetachedCameraController.get().getPitch());
            }
            DetachedCameraController.get().setDetached(false);
            return;
        }

        if (!ClientConfig.get().ALLOW_FPV_INSIDE_TUBE.get()){
            options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }

        Camera cameraObj = (Camera) (Object) this;
        CameraAccessorMixin camera = (CameraAccessorMixin) cameraObj;

        if (!DetachedCameraController.get().isDetached()) {
            DetachedCameraController.get().startCamera(renderViewEntity);
            DetachedCameraController.get().setDetached(true);
            this.createHypertube$setDetachedExternal(true);
        }
        DetachedCameraController.get().tickCamera(renderViewEntity);

        camera.callSetRotation(DetachedCameraController.get().getYaw() * (flipped ? -1 : 1), DetachedCameraController.get().getPitch());

        camera.callSetPosition(
                MathHelper.lerp(PartialTicks, renderViewEntity.prevX, renderViewEntity.getX()),
                MathHelper.lerp(PartialTicks, renderViewEntity.prevY, renderViewEntity.getY()),
                MathHelper.lerp(PartialTicks, renderViewEntity.prevZ, renderViewEntity.getZ()));

        float f;
        if (renderViewEntity instanceof LivingEntity livingentity) {
            f = livingentity.getScaleFactor();
        } else {
            f = 1.0F;
        }
        camera.callMove(-camera.callGetMaxZoom(4.0F), 0.0F, 0.0F);

        ci.cancel();
    }
}
