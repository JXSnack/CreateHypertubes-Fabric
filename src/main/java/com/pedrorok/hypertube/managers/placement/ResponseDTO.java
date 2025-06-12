package com.pedrorok.hypertube.managers.placement;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * @author Rok, Pedro Lucas nmm. Created on 27/05/2025
 * @project Create Hypertube
 */
public record ResponseDTO(boolean valid, String errorMessage) {

    public static ResponseDTO get(boolean valid) {
        return new ResponseDTO(valid, "");
    }
    public static ResponseDTO get(boolean valid, String errorMessage) {
        return new ResponseDTO(valid, errorMessage);
    }

    public static ResponseDTO invalid(String errorMessageKey) {
        return new ResponseDTO(false, errorMessageKey);
    }
    public static ResponseDTO invalid() {
        return new ResponseDTO(false, "");
    }

    public MutableText getMessageComponent() {
        if (errorMessage.isEmpty()) {
            return Text.empty();
        }
        MutableText translatable = Text.translatable(errorMessage);
        if (valid) return translatable;
        return translatable.styled(style -> style.withColor(Formatting.RED));
    }
}
