package com.pedrorok.hypertube.events;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pedrorok.hypertube.camera.DetachedCameraController;
import com.pedrorok.hypertube.managers.TravelManager;
import com.pedrorok.hypertube.managers.placement.TubePlacement;
import com.pedrorok.hypertube.managers.sound.TubeSoundManager;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

/**
 * @author Rok, Pedro Lucas nmm. Created on 23/04/2025
 * @project Create Hypertube
 */
public class ClientEvents {

    public static void onTickPre() {
        onTick(true);
    }

    public static void onTickPost() {
        onTick(false);
    }

    private static void onTick(boolean isPreEvent) {
        if (!isGameActive()) return;

        if (isPreEvent) {
            TubeSoundManager.tickClientPlayerSounds();
            return;
        }
        TubePlacement.clientTick();
        DetachedCameraController.cameraTick();
    }


    public static void onRenderWorld(WorldRenderContext event) {
        MatrixStack ms = event.matrixStack();
        ms.push();
        SuperRenderTypeBuffer buffer = SuperRenderTypeBuffer.getInstance();
        Vec3d camera = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();

        TubePlacement.drawCustomBlockSelection(ms, buffer, camera);

        buffer.draw();
        RenderSystem.enableCull();
        ms.pop();
    }

    protected static boolean isGameActive() {
        return !(MinecraftClient.getInstance().world == null || MinecraftClient.getInstance().player == null);
    }

    public static void onRenderPlayer(PlayerEntity player, MatrixStack poseStack) {
        if (!player.getPersistentData().getBoolean(TravelManager.TRAVEL_TAG)) return;

        poseStack.push();
        poseStack.translate(0, 0.2, 0);
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-player.getYaw()));
        poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(player.getPitch() + 90));
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(player.getYaw()));
        poseStack.translate(0, -0.5, 0);
        poseStack.scale(0.8f, 0.8f, 0.8f);
    }

    public static void onRenderPlayerPost(PlayerEntity player, MatrixStack poseStack) {
        if (!player.getPersistentData().getBoolean(TravelManager.TRAVEL_TAG)) return;

        poseStack.pop();
    }
}
