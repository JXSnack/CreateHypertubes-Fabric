package com.pedrorok.hypertube;

import com.pedrorok.hypertube.config.ClientConfig;
import com.pedrorok.hypertube.registry.*;
import com.simibubi.create.foundation.data.CreateRegistrate;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.minecraft.item.ItemGroup; // CreativeModeTab
import net.minecraft.registry.RegistryKey; // ResourceKey
import net.minecraftforge.fml.config.ModConfig;
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
        // modEventBus -> ForgeConfigRegistry, added MOD_ID as argument
        ForgeConfigRegistry.INSTANCE.register(MOD_ID, ModConfig.Type.CLIENT, ClientConfig.SPEC, MOD_ID + "-client.toml");

        // No need for REGISTRATE event registration
        // REGISTRATE.registerEventListeners();

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
