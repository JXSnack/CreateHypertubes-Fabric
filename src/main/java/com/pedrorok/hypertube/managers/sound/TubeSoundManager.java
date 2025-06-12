package com.pedrorok.hypertube.managers.sound;

import com.pedrorok.hypertube.registry.ModSounds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Rok, Pedro Lucas nmm. Created on 04/06/2025
 * @project Create Hypertube
 */
@Environment(EnvType.CLIENT)
public class TubeSoundManager {

    private static final Map<UUID, TubeAmbientSound> ambientSounds = new HashMap<>();

    public static void tickClientPlayerSounds() {
        TravelSound.tickClientPlayerSounds();
    }

    public static TubeAmbientSound getAmbientSound(UUID uuid) {
        return ambientSounds.computeIfAbsent(uuid, k -> new TubeAmbientSound());
    }

    public static void removeAmbientSound(UUID uuid) {
        TubeAmbientSound sound = ambientSounds.remove(uuid);
        if (sound != null) {
            sound.stopSound();
        }
    }

    public static class TubeAmbientSound {
        private boolean isClientNear;

        private TubeSound travelSound;

        public void enableClientPlayerSound(Entity e, Vec3d normal, double distance, boolean isOpen) {
            if (e != MinecraftClient.getInstance()
                    .getCameraEntity())
                return;

            if (distance > 32) {
                tickClientPlayerSounds();
                return;
            }
            isClientNear = true;


            float pitch = isOpen ? 1.5f : 0.5f;
            float maxVolume = Math.max(0, (float) (1.0 - (distance / 48)));

            if (travelSound == null || travelSound.isDone()) {
                travelSound = new TubeSound(ModSounds.TRAVELING, pitch);
                MinecraftClient.getInstance()
                        .getSoundManager()
                        .play(travelSound);
            }
            travelSound.updateLocation(normal);
            travelSound.setPitch(pitch);
            travelSound.fadeIn(maxVolume);
        }

        public void tickClientPlayerSounds() {
            if (!isClientNear && travelSound != null)
                if (travelSound.isFaded())
                    travelSound.stopSound();
                else
                    travelSound.fadeOut();
            isClientNear = false;
        }

        public void stopSound() {
            if (travelSound != null) {
                travelSound.stopSound();
                travelSound = null;
            }
        }
    }

    public static class TravelSound {
        private static boolean isClientPlayerInTravel;

        private static TubeSound travelSound;

        public static void enableClientPlayerSound(Entity e, float maxVolume, float pitch) {
            if (e != MinecraftClient.getInstance()
                    .getCameraEntity())
                return;

            isClientPlayerInTravel = true;

            if (travelSound == null || travelSound.isDone()) {
                travelSound = new TubeSound(ModSounds.TRAVELING, pitch);
                MinecraftClient.getInstance()
                        .getSoundManager()
                        .play(travelSound);
            }
            travelSound.setPitch(pitch);
            travelSound.fadeIn(maxVolume);
        }

        private static void tickClientPlayerSounds() {
            if (!isClientPlayerInTravel && travelSound != null)
                if (travelSound.isFaded())
                    travelSound.stopSound();
                else
                    travelSound.fadeOut();
            isClientPlayerInTravel = false;
        }
    }
}
