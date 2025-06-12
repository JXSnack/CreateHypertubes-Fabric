package com.pedrorok.hypertube;

import com.pedrorok.hypertube.config.ClientConfig;
import com.pedrorok.hypertube.registry.*;
import com.simibubi.create.foundation.data.CreateRegistrate;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import net.fabricmc.api.ModInitializer;
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

    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(HypertubeMod.MOD_ID);
    //.defaultCreativeTab((ResourceKey<CreativeModeTab>) null);

    public HypertubeMod() {
        /* [REMOVED, FORGE ONLY]
        REGISTRATE.registerEventListeners(modEventBus); */
    }

    // NEW FABRIC METHOD, moved everything from constructor to here
    @Override
    public void onInitialize() {
        ForgeConfigRegistry.INSTANCE.register(MOD_ID, ModConfig.Type.CLIENT, ClientConfig.SPEC, "-client.toml");

        ModPartialModels.init();

        ModBlocks.register();
        ModBlockEntities.register();

        ModCreativeTab.register();

        ModSounds.register();
    }

    /* [REMOVED, FORGE ONLY]
     private void commonSetup(final FMLCommonSetupEvent event) {

    } */

    public static CreateRegistrate get() {
        return REGISTRATE;
    }
}
