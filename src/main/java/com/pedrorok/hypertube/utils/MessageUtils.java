package com.pedrorok.hypertube.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

/**
 * @author Rok, Pedro Lucas nmm. Created on 27/05/2025
 * @project Create Hypertube
 */
public class MessageUtils {

    public static void sendActionMessage(PlayerEntity player, String message) {
        player.sendMessage(Text.literal(message), true);
    }

    @Environment(EnvType.CLIENT)
    public static void sendClientActionMessage(String message) {
        sendActionMessage(MinecraftClient.getInstance().player, message);
    }
}
