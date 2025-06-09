package com.pedrorok.hypertube.registry;

import com.pedrorok.hypertube.HypertubeMod;
import com.pedrorok.hypertube.blocks.HyperEntranceBlock;
import com.pedrorok.hypertube.blocks.HypertubeBlock;
import com.pedrorok.hypertube.items.HypertubeItem;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.block.AbstractBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.sound.BlockSoundGroup;

import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;


/**
 * @author Rok, Pedro Lucas nmm. Created on 17/04/2025
 * @project Create Hypertube
 */
public class ModBlocks {

    public static final AbstractBlock.Settings PROPERTIES = AbstractBlock.Settings.create()
            .hardness(1.0f)
            .resistance(10.0f)
            .sounds(BlockSoundGroup.METAL)
            .nonOpaque()
            .blockVision((state, level, pos) -> false)
            .suffocates((state, level, pos) -> false);

    private static final CreateRegistrate REGISTRATE = HypertubeMod.get();

    public static final BlockEntry<HypertubeBlock> HYPERTUBE = REGISTRATE.block("hypertube", HypertubeBlock::new)
            .item(HypertubeItem::new).build()
            .properties((a) -> PROPERTIES)
            .transform(axeOrPickaxe())
            .defaultBlockstate()
            .defaultLoot()
            .register();

    public static final BlockEntry<HyperEntranceBlock> HYPERTUBE_ENTRANCE = REGISTRATE.block("hypertube_entrance", HyperEntranceBlock::new)
            .simpleItem()
            .properties((a) -> PROPERTIES)
            .transform(axeOrPickaxe())
            .defaultBlockstate()
            .defaultLoot()
            .item(BlockItem::new)
            .transform(customItemModel())
            .register();

    public static void register() {
    }
}
