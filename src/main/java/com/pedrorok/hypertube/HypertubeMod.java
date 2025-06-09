package com.pedrorok.hypertube;

import com.pedrorok.hypertube.config.ClientConfig;
import com.pedrorok.hypertube.registry.*;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Rok, Pedro Lucas nmm. Created on 17/04/2025
 * @project Create Hypertube
 */
public class HypertubeMod implements ModInitializer {
    public static final String MOD_ID = "create_hypertube";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(HypertubeMod.MOD_ID)
            .defaultCreativeTab((ResourceKey<CreativeModeTab>) null);

    public void onInitialize() {
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, MOD_ID + "-client.toml");
        REGISTRATE.registerEventListeners(modEventBus);

        ModPartialModels.init();

        ModBlocks.register();
        ModBlockEntities.register();

        ModCreativeTab.register();
        ModDataComponent.register();

        ModSounds.register();
    }

    public static CreateRegistrate get() {
        return REGISTRATE;
    }
}
