package com.pedrorok.hypertube.utils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

/**
 * @author Rok, Pedro Lucas nmm. Created on 27/05/2025
 * @project Create Hypertube
 */
public class MessageUtils {

    public static void sendActionMessage(PlayerEntity player, Text message) {
        sendActionMessage(player, message, false);
    }

    public static void sendActionMessage(PlayerEntity player, Text message, boolean forceStay) {
        if (player.getPersistentData().getLong("last_action_message_stay") > System.currentTimeMillis()) {
            return; // Don't send if the last message is still active
        }
        if (forceStay) {
            player.getPersistentData().putLong("last_action_message_stay", System.currentTimeMillis() + 2000);
        }
        player.sendMessage(message, true);
    }

    public static void sendActionMessage(PlayerEntity player, String message) {
        sendActionMessage(player, Text.translatable(message));
    }
}
