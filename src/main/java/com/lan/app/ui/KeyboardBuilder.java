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
        return Map.of(
                "text", text,
                "callback_data", "/" + command
        );
    }

    public static Map<String, String> urlBtn(String text, String url) {
        return Map.of("text", text, "url", url);
    }
}