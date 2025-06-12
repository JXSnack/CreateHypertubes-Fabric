package com.pedrorok.hypertube.mixin;

import com.pedrorok.hypertube.managers.TravelManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Rok, Pedro Lucas nmm. Created on 22/04/2025
 * @project Create Hypertube
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerMovementMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        if (!player.getPersistentData().getBoolean(TravelManager.TRAVEL_TAG)) return;

        Vec3d velocity = new Vec3d(player.getVelocity().x, player.getVelocity().y, player.getVelocity().z);

        if (!(velocity.lengthSquared() > 0.001D)) return;
        Vec3d lastMovementDirection = velocity.normalize();

        float yaw = (float) Math.toDegrees(Math.atan2(-lastMovementDirection.x, lastMovementDirection.z));
        float pitch = (float) Math.toDegrees(Math.atan2(-lastMovementDirection.y, Math.sqrt(lastMovementDirection.x * lastMovementDirection.x + lastMovementDirection.z * lastMovementDirection.z)));

        player.setYaw(yaw);
        player.setPitch(pitch);
    }

    /*@Inject(method = "canPlayerFitWithinBlocksAndEntitiesWhen", at = @At("HEAD"), cancellable = true)
    private void onCanPlayerFitWithinBlocksAndEntitiesWhen(Pose p_294172_, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        if (!player.getPersistentData().getBoolean(TravelManager.TRAVEL_TAG)) return;
        cir.setReturnValue(true);
    }*/

}
