package com.lan.app.flows.printout;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class PrintWaitDetailsHandlerTest {

    @Inject
    PrintWaitDetailsHandler handler;

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

    private static UpdateContext callbackCtx() {
        return new UpdateContext(100L, "private", 200L, 55, null, "irrelevant", "q1", true, "bob", null, null, null);
    }

    @Test
    void callbackUpdate_isIgnored_staysOnWaitDetails() {
        Session s = session();

        StepResult result = handler.handle(callbackCtx(), s);

        assertThat(result).isEqualTo(StepResult.stay(PrintFlowDef.FLOW, PrintFlowDef.STEP_WAIT_DETAILS));
        verify(telegramClient, never()).sendHtml(any(), any(), any());
    }

    @Test
    void blankText_sendsEmptyErrorAndStaysOnWaitDetails() {
        Session s = session();

        StepResult result = handler.handle(textCtx("   "), s);

        assertThat(result).isEqualTo(StepResult.stay(PrintFlowDef.FLOW, PrintFlowDef.STEP_WAIT_DETAILS));
        verify(telegramClient).sendHtml(eq(100L), any(), eq(null));
    }

    @Test
    void textStartingWithSlash_treatedAsEmptyAndStaysOnWaitDetails() {
        Session s = session();

        StepResult result = handler.handle(textCtx("/cancel"), s);

        assertThat(result).isEqualTo(StepResult.stay(PrintFlowDef.FLOW, PrintFlowDef.STEP_WAIT_DETAILS));
        verify(telegramClient).sendHtml(eq(100L), any(), eq(null));
    }

    @Test
    void tooLongText_sendsErrorAndStaysOnWaitDetails() {
        Session s = session();
        String longText = "a".repeat(501);

        StepResult result = handler.handle(textCtx(longText), s);

        assertThat(result).isEqualTo(StepResult.stay(PrintFlowDef.FLOW, PrintFlowDef.STEP_WAIT_DETAILS));
        verify(telegramClient).sendHtml(eq(100L), any(), eq(null));
    }

    @Test
    void validDetails_storesThemAndAdvancesToWaitFile() {
        Session s = session();

        StepResult result = handler.handle(textCtx("5 pages, double-sided"), s);

        assertThat(result).isEqualTo(StepResult.stay(PrintFlowDef.FLOW, PrintFlowDef.STEP_WAIT_FILE));
        assertThat(PrintSession.getDetails(s)).isEqualTo("5 pages, double-sided");
        verify(telegramClient).sendHtml(eq(100L), any(), eq(null));
    }
}
