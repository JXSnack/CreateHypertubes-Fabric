package com.pedrorok.hypertube.mixin;

import com.pedrorok.hypertube.managers.TravelManager;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Rok, Pedro Lucas nmm. Created on 23/05/2025
 * @project Create Hypertube
 */
@Mixin(value = PlayerEntityModel.class, priority = 1001)
public abstract class PlayerModelMixin {

    @Inject(method = "setAngles*", at = @At("RETURN"))
    private void onSetupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount,
                             float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof PlayerEntity player)
            || !player.getPersistentData().getBoolean(TravelManager.TRAVEL_TAG)) return;


        PlayerEntityModel<?> model = (PlayerEntityModel<?>) (Object) this;

        model.rightArm.pitch = 0;
        model.rightArm.yaw = 0;
        model.rightArm.roll = 0;

        model.rightSleeve.pitch = 0;
        model.rightSleeve.yaw = 0;
        model.rightSleeve.roll = 0;


        model.leftArm.pitch = 0;
        model.leftArm.yaw = 0;
        model.leftArm.roll = 0;

        model.leftSleeve.pitch = 0;
        model.leftSleeve.yaw = 0;
        model.leftSleeve.roll = 0;

        model.rightLeg.pitch = 0;
        model.rightLeg.yaw = 0;
        model.rightLeg.roll = 0;

        model.rightPants.pitch = 0;
        model.rightPants.yaw = 0;
        model.rightPants.roll = 0;

        model.leftLeg.pitch = 0;
        model.leftLeg.yaw = 0;
        model.leftLeg.roll = 0;

        model.leftPants.pitch = 0;
        model.leftPants.yaw = 0;
        model.leftPants.roll = 0;

        model.body.pitch = 0;
        model.body.yaw = 0;
        model.body.roll = 0;

        model.head.pitch = -1.2F;
        model.head.yaw = 0;
        model.head.roll = 0;

        model.hat.pitch = -1.2F;
        model.hat.yaw = 0;
        model.hat.roll = 0;

        model.jacket.pitch = 0;
        model.jacket.yaw = 0;
        model.jacket.roll = 0;
    }
}
