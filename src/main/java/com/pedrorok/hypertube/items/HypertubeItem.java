package com.pedrorok.hypertube.items;

import com.pedrorok.hypertube.blocks.HypertubeBlock;
import com.pedrorok.hypertube.blocks.blockentities.HypertubeBlockEntity;
import com.pedrorok.hypertube.managers.placement.ResponseDTO;
import com.pedrorok.hypertube.managers.placement.SimpleConnection;
import com.pedrorok.hypertube.managers.placement.TubePlacement;
import com.pedrorok.hypertube.registry.ModBlockEntities;
import com.pedrorok.hypertube.registry.ModDataComponent;
import com.pedrorok.hypertube.utils.MessageUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import java.util.Optional;

/**
 * @author Rok, Pedro Lucas nmm. Created on 23/04/2025
 * @project Create Hypertube
 */
public class HypertubeItem extends BlockItem {
    public HypertubeItem(Block pBlock, Settings pProperties) {
        super(pBlock, pProperties);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext pContext) {
        ItemStack stack = pContext.getStack();
        BlockPos pos = pContext.getBlockPos();
        World level = pContext.getWorld();
        BlockState state = level.getBlockState(pos);
        PlayerEntity player = pContext.getPlayer();
        if (level.isClient) return ActionResult.SUCCESS;

        if (player == null)
            return super.useOnBlock(pContext);
        if (pContext.getHand() == Hand.OFF_HAND)
            return super.useOnBlock(pContext);
        if (player.isSneaking() && !hasGlint(stack))
            return super.useOnBlock(pContext);

        Direction direction = pContext.getSide();

        MessageUtils.sendActionMessage(player, "");
        if (!hasGlint(stack)) {
            ResponseDTO select = select(level, pos, direction, stack);
            if (select.valid()) {
                level.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, 0.75f, 1);
                return ActionResult.SUCCESS;
            }
            if (!select.errorMessage().isEmpty()) {
                MessageUtils.sendActionMessage(player, select.getMessageComponent());
            }
            return super.useOnBlock(pContext);
        }

        SimpleConnection simpleConnection = ModDataComponent.decodeSimpleConnection(stack);
        if (player.isSneaking() && simpleConnection.pos().equals(pos)) {
            MessageUtils.sendActionMessage(player, Text.translatable("placement.create_hypertube.conn_cleared").styled(style -> style.withColor(Formatting.YELLOW)));
            clearConnection(stack);
            return ActionResult.SUCCESS;
        }

        if (simpleConnection.pos().equals(pos)) {
            player.playSound(SoundEvents.ENTITY_ITEM_FRAME_REMOVE_ITEM, 1.0f, 1.0f);
            return ActionResult.FAIL;
        }

        boolean isHypertubeClicked = (state.getBlock() instanceof HypertubeBlock);
        boolean success = false;

        if (isHypertubeClicked) {
            Optional<HypertubeBlockEntity> blockEntity = level.getBlockEntity(pos, ModBlockEntities.HYPERTUBE.get());
            if (blockEntity.isPresent()) {
                success = TubePlacement.handleHypertubeClicked(blockEntity.get(), player, simpleConnection, pos, direction, level, stack);
            }
            BlockSoundGroup soundtype = state.getSoundGroup();
            if (success) {
                level.playSound(null, pos, soundtype.getPlaceSound(), SoundCategory.BLOCKS,
                        (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
            } else {
                level.playSound(player, pos, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.BLOCKS,
                        1, 0.5f);
            }
        }

        return isHypertubeClicked ? ActionResult.FAIL : super.useOnBlock(pContext);
    }

    public static ResponseDTO select(WorldAccess world, BlockPos pos, Direction direction, ItemStack heldItem) {
        BlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();
        if (!(block instanceof HypertubeBlock tube))
            return ResponseDTO.get(false);
        HypertubeBlockEntity blockEntity = (HypertubeBlockEntity) world.getBlockEntity(pos);
        if (blockEntity == null) {
            return ResponseDTO.get(false);
        }
        if (!blockEntity.getFacesConnectable().contains(direction)) {
            return ResponseDTO.get(false, "placement.create_hypertube.cant_conn_to_face");
        }

        ModDataComponent.encodeSimpleConnection(pos, direction, heldItem);
        heldItem.getNbt().putBoolean("foil", true);
        return ResponseDTO.get(true);
    }

    public static void clearConnection(ItemStack stack) {
        ModDataComponent.removeSimpleConnection(stack);
        stack.getNbt().remove("foil");
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return stack.getNbt() != null && (stack.getNbt().contains("foil") || stack.hasEnchantments());
    }
}
