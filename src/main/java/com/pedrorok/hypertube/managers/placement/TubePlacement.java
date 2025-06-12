package com.pedrorok.hypertube.managers.placement;

import com.pedrorok.hypertube.blocks.HypertubeBlock;
import com.pedrorok.hypertube.blocks.blockentities.HypertubeBlockEntity;
import com.pedrorok.hypertube.items.HypertubeItem;
import com.pedrorok.hypertube.registry.ModBlocks;
import com.pedrorok.hypertube.registry.ModDataComponent;
import com.pedrorok.hypertube.utils.MessageUtils;
import com.pedrorok.hypertube.utils.RayCastUtils;
import com.pedrorok.hypertube.utils.TubeUtils;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

/**
 * @author Rok, Pedro Lucas nmm. Created on 23/04/2025
 * @project Create Hypertube
 */
public class TubePlacement {

    static BlockPos hoveringPos;
    static boolean canPlace = false;
    static LerpedFloat animation = LerpedFloat.linear()
            .startWithValue(0);

    @Environment(EnvType.CLIENT)
    public static void clientTick() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        ItemStack stack = player.getMainHandStack();
        HitResult hitResult = MinecraftClient.getInstance().crosshairTarget;

        if (hitResult == null)
            return;
        if (hitResult.getType() != HitResult.Type.BLOCK)
            return;

        Item tubeItem = ModBlocks.HYPERTUBE.asItem();
        if (!stack.getItem().equals(tubeItem)) {
            stack = player.getOffHandStack();
            if (!stack.getItem().equals(tubeItem))
                return;
        }

        if (!stack.hasGlint())
            return;

        World level = player.getWorld();
        BlockHitResult bhr = (BlockHitResult) hitResult;
        BlockPos pos = bhr.getBlockPos();
        BlockState hitState = level.getBlockState(pos);
        boolean hypertubeHitResult = hitState.getBlock() instanceof HypertubeBlock;
        if (hitState.isAir() || hypertubeHitResult) {
            hoveringPos = pos;
        } else {
            pos = pos.offset(bhr.getSide());
        }

        SimpleConnection connectionFrom = ModDataComponent.decodeSimpleConnection(stack);

        animation.setValue(0.8);
        if (connectionFrom == null) {
            animation.setValue(0);
            return;
        }

        Direction finalDirection = RayCastUtils.getDirectionFromHitResult(player, ModBlocks.HYPERTUBE.get());

        SimpleConnection connectionTo = new SimpleConnection(pos, finalDirection);
        BezierConnection bezierConnection = BezierConnection.of(connectionFrom, connectionTo);

        // Exception & visual
        ResponseDTO response = bezierConnection.getValidation();

        if (response.valid()) {
            response = TubeUtils.checkSurvivalItems(player, (int) bezierConnection.distance(), true);
        }
        if (response.valid()) {
            response = TubeUtils.checkBlockCollision(level, bezierConnection);
        }
        if (response.valid() && hypertubeHitResult) {
            response = TubeUtils.checkClickedHypertube(level, pos, finalDirection.getOpposite());
        }

        animation.setValue(!response.valid() ? 0.2 : 0.8);

        canPlace = response.valid();
        bezierConnection.drawPath(animation, canPlace);

        if (!response.valid()) {
            MessageUtils.sendActionMessage(player, response.getMessageComponent());
            return;
        }

        MessageUtils.sendActionMessage(player, "");
    }

    public static boolean handleHypertubeClicked(HypertubeBlockEntity tubeEntity, PlayerEntity player, SimpleConnection simpleConnection, BlockPos pos, Direction direction, World level, ItemStack stack) {

        boolean thisTubeCanConnTo = tubeEntity.getConnectionTo() == null;
        boolean thisTubeCanConnFrom = tubeEntity.getConnectionFrom() == null;
        HypertubeBlockEntity otherBlockEntity = (HypertubeBlockEntity) level.getBlockEntity(simpleConnection.pos());

        if (otherBlockEntity == null) {
            MessageUtils.sendActionMessage(player, Text.translatable("placement.create_hypertube.no_other_tube_found")
                    .styled(style -> style.withColor(Formatting.RED)));
            return false;
        }

        boolean otherTubeCanConnTo = otherBlockEntity.getConnectionTo() == null;
        boolean otherTubeCanConnFrom = otherBlockEntity.getConnectionFrom() == null;

        boolean usingConnectingTo = thisTubeCanConnFrom && otherTubeCanConnTo;

        if (!usingConnectingTo) {
            if (!thisTubeCanConnTo || !otherTubeCanConnFrom) {
                MessageUtils.sendActionMessage(player, Text.translatable("placement.create_hypertube.cant_conn_tubes")
                        .styled(style -> style.withColor(Formatting.RED)));
                return false;
            }
        }

        BezierConnection connection = new BezierConnection(
                usingConnectingTo ? simpleConnection : new SimpleConnection(pos, direction),
                usingConnectingTo ? new SimpleConnection(pos, direction.getOpposite()) : new SimpleConnection(simpleConnection.pos(), simpleConnection.direction().getOpposite()));


        ResponseDTO validation = connection.getValidation();
        if (validation.valid()) {
            validation = TubeUtils.checkSurvivalItems(player, (int) connection.distance(), true);
        }
        if (validation.valid()) {
            validation = TubeUtils.checkBlockCollision(level, connection);
        }
        if (validation.valid()) {
            validation = TubeUtils.checkClickedHypertube(level, pos, direction);
        }

        if (!validation.valid()) {
            MessageUtils.sendActionMessage(player, validation.getMessageComponent().styled(style -> style.withColor(Formatting.RED)), true);
            return false;
        }
        TubeUtils.checkSurvivalItems(player, (int) connection.distance(), false);

        if (level.isClient) {
            connection.drawPath(LerpedFloat.linear()
                    .startWithValue(0), true);
        }

        if (usingConnectingTo) {
            tubeEntity.setConnectionFrom(connection.getFromPos(), direction);
            otherBlockEntity.setConnectionTo(connection);
        } else {
            tubeEntity.setConnectionTo(connection);
            otherBlockEntity.setConnectionFrom(connection.getFromPos(), direction);
        }

        MessageUtils.sendActionMessage(player, Text.translatable("placement.create_hypertube.success_conn")
                .styled(style -> style.withColor(Formatting.GREEN)), true);
        player.playSound(SoundEvents.ENTITY_GLOW_ITEM_FRAME_ADD_ITEM, 1.0f, 1.0f);


        HypertubeItem.clearConnection(player.getStackInHand(Hand.MAIN_HAND));
        return true;
    }

    // SERVER BLOCK VALIDATION
    public static void tickPlayerServer(@NotNull PlayerEntity player) {
        if (player.age % 20 != 0) return;
        ItemStack itemInHand = player.getStackInHand(Hand.MAIN_HAND);
        World level = player.getWorld();
        if (!(itemInHand.getItem() instanceof HypertubeItem)) return;
        if (!itemInHand.hasGlint()) return;
        SimpleConnection connection = ModDataComponent.decodeSimpleConnection(itemInHand);
        if (connection == null) return;
        if (!(level.getBlockEntity(new BlockPos(connection.pos())) instanceof HypertubeBlockEntity)) {
            HypertubeItem.clearConnection(itemInHand);
            MessageUtils.sendActionMessage(player,
                    Text.translatable("placement.create_hypertube.conn_cleared_invalid_block").styled(style -> style.withColor(Formatting.RED))
            );
        }
    }

    @Environment(EnvType.CLIENT)
    public static void drawCustomBlockSelection(MatrixStack ms, VertexConsumerProvider buffer, Vec3d camera) {
        ItemStack mainHandItem = MinecraftClient.getInstance().player.getMainHandStack();
        if (!mainHandItem.isOf(ModBlocks.HYPERTUBE.asItem())) return;
        if (!mainHandItem.hasGlint()) return;
        SimpleConnection connection = ModDataComponent.decodeSimpleConnection(mainHandItem);
        if (connection == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        BlockState blockState = mc.world.getBlockState(connection.pos());
        if (!(blockState.getBlock() instanceof HypertubeBlock)) return;
        HypertubeBlock block = (HypertubeBlock) blockState.getBlock();

        VertexConsumer vb = buffer.getBuffer(RenderLayer.getLines());
        ms.push();
        ms.translate(connection.pos().getX() - camera.x, connection.pos().getY() - camera.y, connection.pos().getZ() - camera.z);
        TrackBlockOutline.renderShape(block.getShape(blockState), ms, vb, canPlace);
        ms.pop();
    }
}
