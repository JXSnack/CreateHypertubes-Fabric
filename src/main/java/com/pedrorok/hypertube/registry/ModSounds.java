package com.pedrorok.hypertube.registry;

import com.pedrorok.hypertube.HypertubeMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * @author Rok, Pedro Lucas nmm. Created on 26/05/2025
 * @project Create Hypertube
 */
public class ModSounds {

    public static final SoundEvent HYPERTUBE_SUCTION = register("suction");
    public static final SoundEvent TRAVELING = register("traveling");

    private static SoundEvent register(String name) {
        Identifier id = new Identifier(HypertubeMod.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void register() {
    }
}
