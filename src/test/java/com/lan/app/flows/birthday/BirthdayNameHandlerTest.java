package com.lan.app.flows.birthday;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class BirthdayNameHandlerTest {

    @Inject
    BirthdayNameHandler handler;

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

    private static UpdateContext textCtx(String text) {
        return new UpdateContext(100L, "private", 200L, null, text, null, null, false, "bob", null, null, null);
    }

    private static UpdateContext callbackCtx(String data) {
        return new UpdateContext(100L, "private", 200L, 55, null, data, "q1", true, "bob", null, null, null);
    }

    @Test
    void name_storesNameAndAdvancesToText() {
        Session s = session();

        StepResult result = handler.handle(textCtx("Ann"), s);

        assertThat(result).isEqualTo(StepResult.stay(BirthdayFlowDef.FLOW, BirthdayFlowDef.STEP_TEXT));
        assertThat(BirthdaySession.getName(s)).isEqualTo("Ann");
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }

    @Test
    void anonymousCallback_storesNullNameAndAdvancesToText() {
        Session s = session();

        StepResult result = handler.handle(callbackCtx(BirthdayFlowDef.CB_ANONYMOUS), s);

        assertThat(result).isEqualTo(StepResult.stay(BirthdayFlowDef.FLOW, BirthdayFlowDef.STEP_TEXT));
        assertThat(BirthdaySession.getName(s)).isNull();
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }

    @Test
    void blankText_repromptsAndStaysOnName() {
        Session s = session();

        StepResult result = handler.handle(textCtx("   "), s);

        assertThat(result).isEqualTo(StepResult.stay(BirthdayFlowDef.FLOW, BirthdayFlowDef.STEP_NAME));
        assertThat(BirthdaySession.getName(s)).isNull();
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }
}
