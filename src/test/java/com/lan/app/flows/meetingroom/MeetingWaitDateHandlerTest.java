package com.lan.app.flows.meetingroom;

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
class MeetingWaitDateHandlerTest {

    @Inject
    MeetingWaitDateHandler handler;

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
    void nonCallbackUpdate_staysOnWaitDate() {
        Session s = session();

        StepResult result = handler.handle(textCtx("hello"), s);

        assertThat(result).isEqualTo(StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_DATE));
        verify(telegramClient, never()).sendHtml(any(), any(), any());
    }

    @Test
    void callbackWithWrongPrefix_staysOnWaitDate() {
        Session s = session();

        StepResult result = handler.handle(callbackCtx("meet:start:10:00"), s);

        assertThat(result).isEqualTo(StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_DATE));
        verify(telegramClient, never()).sendHtml(any(), any(), any());
    }

    @Test
    void validDateCallback_storesDateAndAdvancesToWaitStart() {
        Session s = session();

        StepResult result = handler.handle(callbackCtx(MeetingFlowDef.CB_DATE_PFX + "2026-04-12"), s);

        assertThat(result).isEqualTo(StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_START));
        assertThat(MeetingSession.getDate(s)).isEqualTo("2026-04-12");
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }
}
