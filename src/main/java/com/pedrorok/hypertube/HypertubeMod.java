package com.pedrorok.hypertube;

import com.pedrorok.hypertube.config.ClientConfig;
import com.pedrorok.hypertube.registry.*;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.fabricmc.api.ModInitializer;
import net.minecraft.item.ItemGroup; // CreativeModeTab
import net.minecraft.registry.RegistryKey; // ResourceKey
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Rok, Pedro Lucas nmm. Created on 17/04/2025
 * @project Create Hypertube
 */
public class HypertubeMod implements ModInitializer {
    public static final String MOD_ID = "create_hypertube";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID)
            .defaultCreativeTab((RegistryKey<ItemGroup>) null);

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
