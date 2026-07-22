package com.lan.app.ui;

import java.util.List;
import java.util.Map;

public class KeyboardBuilder {

    public static Map<String, Object> inline(List<List<Map<String, String>>> rows) {
        return Map.of("inline_keyboard", rows);
    }

    public static List<Map<String, String>> row(Map<String, String>... buttons) {
        return List.of(buttons);
    }

    public static Map<String, String> cbCmd(String text, String command) {
        String normalized = command.startsWith("/") ? command : "/" + command;
        return Map.of(
                "text", text,
                "callback_data", normalized
        );
    }

    public static Map<String, String> urlBtn(String text, String url) {
        return Map.of("text", text, "url", url);
    }

    /** Button with raw callback_data, not prefixed with "/" — for prefix-matched routing (e.g. "evt_reg_<id>"). */
    public static Map<String, String> rawBtn(String text, String callbackData) {
        return Map.of("text", text, "callback_data", callbackData);
    }
}