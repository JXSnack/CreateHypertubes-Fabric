package com.pedrorok.hypertube.registry;

import com.pedrorok.hypertube.HypertubeMod;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;

/**
 * @author Rok, Pedro Lucas nmm. Created on 21/04/2025
 * @project Create Hypertube
 */
public class ModCreativeTab {

    public static final RegistryKey<ItemGroup> CREATIVE_MODE_TABS =
            RegistryKey.of(Registries.ITEM_GROUP.getKey(), HypertubeMod.id("item_group"));

    public static final ItemGroup TUBE_TAB = FabricItemGroup.builder()
            .displayName(Text.translatable("itemGroup." + HypertubeMod.MOD_ID))
            .icon(() -> ModBlocks.HYPERTUBE.get())
            .build();

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(CREATIVE_MODE_TABS).register(ModCreativeTab::addCreative);
    }

    public static void addCreative(FabricItemGroupEntries entries) {
        CreateRegistrate REGISTRATE = HypertubeMod.get();
        for (var entry : REGISTRATE.getAll(Registries.BLOCK.getKey())) {
            var block = entry.get();
            if (block.asItem() == Items.AIR) continue;

            entries.add(block);
        }
    }

}