package com.lan.app.ui;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeyboardBuilderTest {

    @Test
    void inline_wrapsRowsUnderInlineKeyboardKey() {
        List<List<Map<String, String>>> rows = List.of(
                KeyboardBuilder.row(KeyboardBuilder.cbCmd("A", "a"))
        );

        Map<String, Object> result = KeyboardBuilder.inline(rows);

        assertThat(result).containsOnlyKeys("inline_keyboard");
        assertThat(result.get("inline_keyboard")).isEqualTo(rows);
    }

    @Test
    void row_collectsButtonsIntoList() {
        Map<String, String> b1 = KeyboardBuilder.cbCmd("A", "a");
        Map<String, String> b2 = KeyboardBuilder.cbCmd("B", "b");

        List<Map<String, String>> result = KeyboardBuilder.row(b1, b2);

        assertThat(result).containsExactly(b1, b2);
    }

    @Test
    void cbCmd_addsLeadingSlashWhenMissing() {
        Map<String, String> button = KeyboardBuilder.cbCmd("Label", "start");

        assertThat(button.get("text")).isEqualTo("Label");
        assertThat(button.get("callback_data")).isEqualTo("/start");
    }

    @Test
    void cbCmd_leavesAlreadyPrefixedCommandUnchanged() {
        Map<String, String> button = KeyboardBuilder.cbCmd("Label", "/start");

        assertThat(button.get("callback_data")).isEqualTo("/start");
    }

    @Test
    void urlBtn_setsTextAndUrlKeys() {
        Map<String, String> button = KeyboardBuilder.urlBtn("Open", "https://example.com");

        assertThat(button).containsEntry("text", "Open");
        assertThat(button).containsEntry("url", "https://example.com");
        assertThat(button).doesNotContainKey("callback_data");
    }

    @Test
    void rawBtn_doesNotPrefixCallbackDataWithSlash() {
        Map<String, String> button = KeyboardBuilder.rawBtn("Label", "evt_reg_42");

        assertThat(button.get("callback_data")).isEqualTo("evt_reg_42");
        assertThat(button.get("text")).isEqualTo("Label");
    }
}
