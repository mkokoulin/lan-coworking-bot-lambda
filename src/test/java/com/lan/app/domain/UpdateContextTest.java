package com.lan.app.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateContextTest {

    private static IncomingUpdate baseUpdate() {
        IncomingUpdate u = new IncomingUpdate();
        u.setChatId(1L);
        u.setUserId(2L);
        return u;
    }

    @Test
    void fromIncomingUpdate_copiesAllFields() {
        IncomingUpdate u = baseUpdate();
        u.setChatType("private");
        u.setCallbackMessageId(7);
        u.setText("/start hello");
        u.setCallbackData("cb");
        u.setCallbackQueryId("qid");
        u.setUsername("bob");
        u.setSharedPhone("+37410000000");
        u.setFileId("file1");
        u.setFileName("name.pdf");

        UpdateContext ctx = UpdateContext.fromIncomingUpdate(u);

        assertThat(ctx.chatId()).isEqualTo(1L);
        assertThat(ctx.userId()).isEqualTo(2L);
        assertThat(ctx.chatType()).isEqualTo("private");
        assertThat(ctx.messageId()).isEqualTo(7);
        assertThat(ctx.messageText()).isEqualTo("/start hello");
        assertThat(ctx.callbackData()).isEqualTo("cb");
        assertThat(ctx.callbackQueryId()).isEqualTo("qid");
        assertThat(ctx.username()).isEqualTo("bob");
        assertThat(ctx.sharedPhone()).isEqualTo("+37410000000");
        assertThat(ctx.fileId()).isEqualTo("file1");
        assertThat(ctx.fileName()).isEqualTo("name.pdf");
    }

    @Test
    void hasCallback_true_whenCallbackDataNonBlank() {
        IncomingUpdate u = baseUpdate();
        u.setCallbackData("cb_data");

        UpdateContext ctx = UpdateContext.fromIncomingUpdate(u);

        assertThat(ctx.hasCallback()).isTrue();
        assertThat(ctx.callback()).isTrue();
    }

    @Test
    void hasCallback_false_whenCallbackDataBlank() {
        IncomingUpdate u = baseUpdate();
        u.setCallbackData("   ");

        UpdateContext ctx = UpdateContext.fromIncomingUpdate(u);

        assertThat(ctx.hasCallback()).isFalse();
    }

    @Test
    void isCommand_true_whenTextStartsWithSlash() {
        IncomingUpdate u = baseUpdate();
        u.setText("/help");

        assertThat(UpdateContext.fromIncomingUpdate(u).isCommand()).isTrue();
    }

    @Test
    void isCommand_false_forPlainText() {
        IncomingUpdate u = baseUpdate();
        u.setText("hello");

        assertThat(UpdateContext.fromIncomingUpdate(u).isCommand()).isFalse();
    }

    @Test
    void command_fromTextCommand_stripsSlashAndArgs() {
        IncomingUpdate u = baseUpdate();
        u.setText("/start payload_here");

        assertThat(UpdateContext.fromIncomingUpdate(u).command()).isEqualTo("start");
    }

    @Test
    void command_fromCallbackStartingWithSlash_stripsSlash() {
        IncomingUpdate u = baseUpdate();
        u.setCallbackData("/profile");

        assertThat(UpdateContext.fromIncomingUpdate(u).command()).isEqualTo("profile");
    }

    @Test
    void command_fromPlainCallback_returnsNull() {
        IncomingUpdate u = baseUpdate();
        u.setCallbackData("tariff_list");

        assertThat(UpdateContext.fromIncomingUpdate(u).command()).isNull();
    }

    @Test
    void command_noTextNoCallback_returnsNull() {
        UpdateContext ctx = UpdateContext.fromIncomingUpdate(baseUpdate());

        assertThat(ctx.command()).isNull();
    }

    @Test
    void commandArgs_returnsRemainderAfterCommand() {
        IncomingUpdate u = baseUpdate();
        u.setText("/start reg_abc123");

        assertThat(UpdateContext.fromIncomingUpdate(u).commandArgs()).isEqualTo("reg_abc123");
    }

    @Test
    void commandArgs_noArgs_returnsEmptyString() {
        IncomingUpdate u = baseUpdate();
        u.setText("/start");

        assertThat(UpdateContext.fromIncomingUpdate(u).commandArgs()).isEmpty();
    }

    @Test
    void commandArgs_notACommand_returnsNull() {
        IncomingUpdate u = baseUpdate();
        u.setText("hello world");

        assertThat(UpdateContext.fromIncomingUpdate(u).commandArgs()).isNull();
    }

    @Test
    void callbackPayload_stripsLeadingSlashIfPresent() {
        IncomingUpdate u = baseUpdate();
        u.setCallbackData("/profile");

        assertThat(UpdateContext.fromIncomingUpdate(u).callbackPayload()).isEqualTo("profile");
    }

    @Test
    void callbackPayload_noLeadingSlash_returnsAsIs() {
        IncomingUpdate u = baseUpdate();
        u.setCallbackData("evt_reg_42");

        assertThat(UpdateContext.fromIncomingUpdate(u).callbackPayload()).isEqualTo("evt_reg_42");
    }

    @Test
    void callbackPayload_nullCallbackData_returnsNull() {
        UpdateContext ctx = UpdateContext.fromIncomingUpdate(baseUpdate());

        assertThat(ctx.callbackPayload()).isNull();
    }

    @Test
    void hasSharedPhone_hasPhoto_blankVsPresent() {
        IncomingUpdate u = baseUpdate();
        u.setSharedPhone("");
        u.setFileId("f1");

        UpdateContext ctx = UpdateContext.fromIncomingUpdate(u);

        assertThat(ctx.hasSharedPhone()).isFalse();
        assertThat(ctx.hasPhoto()).isTrue();
    }
}
