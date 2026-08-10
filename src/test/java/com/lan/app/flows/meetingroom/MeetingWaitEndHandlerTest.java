package com.lan.app.flows.meetingroom;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.service.MeetingRoomService;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * telegram.admin-chat-id resolves to 999999 in the test environment (see build.gradle.kts /
 * TG_ADMIN_CHAT_ID), so admin notifications are asserted against that fixed chat id.
 */
@QuarkusTest
class MeetingWaitEndHandlerTest {

    @Inject
    MeetingWaitEndHandler handler;

    @InjectMock
    TelegramClient telegramClient;

    @InjectMock
    I18n i18n;

    @InjectMock
    MeetingRoomService meetingRoomService;

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

    private static UpdateContext callbackCtx(String data, String username) {
        return new UpdateContext(100L, "private", 200L, 55, null, data, "q1", true, username, null, null, null);
    }

    @Test
    void nonCallbackUpdate_staysOnWaitEnd() {
        Session s = session();

        StepResult result = handler.handle(textCtx("hello"), s);

        assertThat(result).isEqualTo(StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_END));
        verifyNoInteractions(telegramClient, meetingRoomService);
    }

    @Test
    void callbackWithWrongPrefix_staysOnWaitEnd() {
        Session s = session();

        StepResult result = handler.handle(callbackCtx(MeetingFlowDef.CB_START_PFX + "10:00", "bob"), s);

        assertThat(result).isEqualTo(StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_END));
        verifyNoInteractions(telegramClient, meetingRoomService);
    }

    @Test
    void missingDateOrStartInSession_sendsBrokenFlowAndResetsToWaitDate() {
        Session s = session();

        StepResult result = handler.handle(callbackCtx(MeetingFlowDef.CB_END_PFX + "11:00", "bob"), s);

        assertThat(result).isEqualTo(StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_DATE));
        verify(telegramClient, times(2)).sendHtml(eq(100L), any(), any());
        verifyNoInteractions(meetingRoomService);
    }

    @Test
    void invalidInterval_sendsErrorAndRepromptsEndTime() {
        Session s = session();
        MeetingSession.setDate(s, "2026-04-12");
        MeetingSession.setStart(s, "10:00");

        StepResult result = handler.handle(callbackCtx(MeetingFlowDef.CB_END_PFX + "09:00", "bob"), s);

        assertThat(result).isEqualTo(StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_END));
        verify(telegramClient, times(2)).sendHtml(eq(100L), any(), any());
        verifyNoInteractions(meetingRoomService);
    }

    @Test
    void validIntervalNoUsername_asksForContactAndAdvancesToWaitContact() {
        Session s = session();
        MeetingSession.setDate(s, "2026-04-12");
        MeetingSession.setStart(s, "10:00");

        StepResult result = handler.handle(callbackCtx(MeetingFlowDef.CB_END_PFX + "11:00", null), s);

        assertThat(result).isEqualTo(StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_CONTACT));
        assertThat(MeetingSession.getEnd(s)).isEqualTo("11:00");
        verify(telegramClient).sendHtml(eq(100L), any(), any());
        verifyNoInteractions(meetingRoomService);
    }

    @Test
    void validIntervalWithUsername_notifiesAdminBooksAndCompletesFlow() {
        Session s = session();
        MeetingSession.setDate(s, "2026-04-12");
        MeetingSession.setStart(s, "10:00");

        StepResult result = handler.handle(callbackCtx(MeetingFlowDef.CB_END_PFX + "11:00", "bob"), s);

        assertThat(result).isEqualTo(StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_DONE));
        assertThat(MeetingSession.getDate(s)).isNull();
        assertThat(MeetingSession.getStart(s)).isNull();
        assertThat(MeetingSession.getEnd(s)).isNull();
        verify(telegramClient).sendHtml(eq(999999L), any(), any());
        verify(telegramClient).sendHtml(eq(100L), any(), any());
        verify(meetingRoomService).tryCreateBooking(eq(100L), eq("2026-04-12"), eq("10:00"), eq("11:00"), eq("@bob"));
    }
}
