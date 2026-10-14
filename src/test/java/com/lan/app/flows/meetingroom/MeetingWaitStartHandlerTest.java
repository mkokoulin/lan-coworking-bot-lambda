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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class MeetingWaitStartHandlerTest {

    @Inject
    MeetingWaitStartHandler handler;

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
    void nonCallbackUpdate_staysOnWaitStart() {
        Session s = session();

        StepResult result = handler.handle(textCtx("hello"), s);

        assertThat(result).isEqualTo(StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_START));
        verify(telegramClient, never()).sendHtml(any(), any(), any());
    }

    @Test
    void callbackWithWrongPrefix_staysOnWaitStart() {
        Session s = session();

        StepResult result = handler.handle(callbackCtx(MeetingFlowDef.CB_DATE_PFX + "2026-04-12"), s);

        assertThat(result).isEqualTo(StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_START));
        verify(telegramClient, never()).sendHtml(any(), any(), any());
    }

    @Test
    void validStartCallback_noDateInSession_sendsErrorAndResetsToWaitDate() {
        Session s = session();

        StepResult result = handler.handle(callbackCtx(MeetingFlowDef.CB_START_PFX + "10:00"), s);

        assertThat(result).isEqualTo(StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_DATE));
        verify(telegramClient, times(2)).sendHtml(eq(100L), any(), any());
    }

    @Test
    void validStartCallback_withDateInSession_storesStartAndAdvancesToWaitEnd() {
        Session s = session();
        MeetingSession.setDate(s, "2026-04-12");

        StepResult result = handler.handle(callbackCtx(MeetingFlowDef.CB_START_PFX + "10:00"), s);

        assertThat(result).isEqualTo(StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_END));
        assertThat(MeetingSession.getStart(s)).isEqualTo("10:00");
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }
}
