package com.lan.app.flows.eventchange;

import com.lan.app.domain.IncomingUpdate;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class EventChangeHandlerTest {

    @Inject
    EventChangeHandler handler;

    @InjectMock
    TelegramClient telegramClient;

    @InjectMock
    I18n i18n;

    @BeforeEach
    void stubTranslations() {
        when(i18n.t(any(), any())).thenReturn("translated");
    }

    private static Session session() {
        return Session.newDefault(100L, 200L);
    }

    private static UpdateContext callbackCtx(String data) {
        IncomingUpdate u = new IncomingUpdate();
        u.setChatId(100L);
        u.setUserId(200L);
        u.setCallbackData(data);
        return UpdateContext.fromIncomingUpdate(u);
    }

    private static UpdateContext textCtx(String text) {
        return textCtx(text, null);
    }

    private static UpdateContext textCtx(String text, String username) {
        IncomingUpdate u = new IncomingUpdate();
        u.setChatId(100L);
        u.setUserId(200L);
        u.setText(text);
        u.setUsername(username);
        return UpdateContext.fromIncomingUpdate(u);
    }

    @Test
    void callback_withRegId_storesItAndPromptsForMessage() {
        Session s = session();

        StepResult result = handler.handle(callbackCtx(EventChangeFlowDef.CB_PREFIX + "42"), s);

        assertThat(result).isEqualTo(StepResult.stay(EventChangeFlowDef.FLOW, EventChangeFlowDef.STEP_WAIT_MESSAGE));
        assertThat(EventChangeSession.getRegId(s)).isEqualTo("42");
        verify(telegramClient).sendHtml(eq(100L), any(), isNull());
    }

    @Test
    void callback_withoutRecognizedPrefix_storesNullRegId() {
        Session s = session();

        handler.handle(callbackCtx("something_else"), s);

        assertThat(EventChangeSession.getRegId(s)).isNull();
    }

    @Test
    void blankMessage_sendsEmptyPromptAndStaysOnStep() {
        Session s = session();

        StepResult result = handler.handle(textCtx("   "), s);

        assertThat(result).isEqualTo(StepResult.stay(EventChangeFlowDef.FLOW, EventChangeFlowDef.STEP_WAIT_MESSAGE));
        verify(telegramClient).sendHtml(eq(100L), any(), isNull());
    }

    @Test
    void nullMessageText_sendsEmptyPromptAndStaysOnStep() {
        Session s = session();

        StepResult result = handler.handle(textCtx(null), s);

        assertThat(result).isEqualTo(StepResult.stay(EventChangeFlowDef.FLOW, EventChangeFlowDef.STEP_WAIT_MESSAGE));
    }

    @Test
    void validMessage_notifiesAdminAndThanksUserAndClearsSession() {
        Session s = session();
        EventChangeSession.setRegId(s, "42");

        StepResult result = handler.handle(textCtx("Please change my slot", "bob"), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verify(telegramClient).sendHtml(eq(999999L), any(), isNull());
        verify(telegramClient).sendHtml(eq(100L), any(), isNull());
        assertThat(EventChangeSession.getRegId(s)).isNull();
    }

    @Test
    void validMessage_formatsAdminNotificationWithGuestRegIdAndEscapedText() {
        Session s = session();
        EventChangeSession.setRegId(s, "77");
        when(i18n.t("en", "event_change_admin_notify")).thenReturn("%s|%s|%s");

        handler.handle(textCtx("<script>hi</script>", "bob"), s);

        verify(telegramClient).sendHtml(eq(999999L), eq("@bob|77|&lt;script&gt;hi&lt;/script&gt;"), isNull());
    }

    @Test
    void validMessage_noUsername_fallsBackToIdInAdminNotification() {
        Session s = session();
        EventChangeSession.setRegId(s, "77");
        when(i18n.t("en", "event_change_admin_notify")).thenReturn("%s|%s|%s");

        handler.handle(textCtx("hello", null), s);

        verify(telegramClient).sendHtml(eq(999999L), eq("id:200|77|hello"), isNull());
    }

    @Test
    void validMessage_noRegId_usesDashPlaceholder() {
        Session s = session();
        when(i18n.t("en", "event_change_admin_notify")).thenReturn("%s|%s|%s");

        handler.handle(textCtx("hello", "bob"), s);

        verify(telegramClient).sendHtml(eq(999999L), eq("@bob|-|hello"), isNull());
    }
}
