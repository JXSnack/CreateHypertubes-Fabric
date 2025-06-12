package com.pedrorok.hypertube.mixin;

import com.pedrorok.hypertube.managers.TravelManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(Entity.class)
public class EntityTravelingMixin {

    @Inject(method = "hasNoGravity", at = @At("HEAD"), cancellable = true)
    private void cancelLerpMotion(CallbackInfoReturnable<Boolean> cir) {
        if (!(((Entity) (Object) this) instanceof PlayerEntity player)
            || !player.getPersistentData().getBoolean(TravelManager.TRAVEL_TAG)) return;
        //cir.setReturnValue(true);
    }

    @Inject(method = "getPose", at = @At("HEAD"), cancellable = true)
    private void cancelPose(CallbackInfoReturnable<EntityPose> cir) {
        if (!(((Entity) (Object) this) instanceof PlayerEntity player)
            || !player.getPersistentData().getBoolean(TravelManager.TRAVEL_TAG)) return;
        cir.setReturnValue(EntityPose.STANDING);
    }
}
