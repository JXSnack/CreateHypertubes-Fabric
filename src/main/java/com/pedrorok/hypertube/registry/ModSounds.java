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

    public static void register() {}

    // FABRIC ONLY, replacement for Forge's DeferredRegister and whatnot
    public static SoundEvent register(String name) {
        Identifier id = HypertubeMod.id(name);
        return Registry.register(
                Registries.SOUND_EVENT,
                id,
                SoundEvent.of(id)
        );
    }

}
