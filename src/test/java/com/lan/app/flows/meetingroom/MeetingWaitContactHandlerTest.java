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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * telegram.admin-chat-id resolves to 999999 in the test environment (see build.gradle.kts /
 * TG_ADMIN_CHAT_ID), so admin notifications are asserted against that fixed chat id.
 */
@QuarkusTest
class MeetingWaitContactHandlerTest {

    @Inject
    MeetingWaitContactHandler handler;

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

    private static UpdateContext callbackCtx() {
        return new UpdateContext(100L, "private", 200L, 55, null, "irrelevant", "q1", true, "bob", null, null, null);
    }

    @Test
    void callbackUpdate_isIgnored_staysOnWaitContact() {
        Session s = session();

        StepResult result = handler.handle(callbackCtx(), s);

        assertThat(result).isEqualTo(StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_CONTACT));
        verifyNoInteractions(telegramClient, meetingRoomService);
    }

    @Test
    void blankText_sendsEmptyErrorAndStaysOnWaitContact() {
        Session s = session();

        StepResult result = handler.handle(textCtx("   "), s);

        assertThat(result).isEqualTo(StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_CONTACT));
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }

    @Test
    void tooLongText_sendsErrorAndStaysOnWaitContact() {
        Session s = session();
        String longText = "a".repeat(65);

        StepResult result = handler.handle(textCtx(longText), s);

        assertThat(result).isEqualTo(StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_CONTACT));
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }

    @Test
    void missingIntervalInSession_sendsBrokenFlowAndResetsToWaitDate() {
        Session s = session();

        StepResult result = handler.handle(textCtx("call me"), s);

        assertThat(result).isEqualTo(StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_DATE));
        verify(telegramClient, times(2)).sendHtml(eq(100L), any(), any());
        verifyNoInteractions(meetingRoomService);
    }

    @Test
    void validContact_notifiesAdminBooksAndCompletesFlow() {
        Session s = session();
        MeetingSession.setDate(s, "2026-04-12");
        MeetingSession.setStart(s, "10:00");
        MeetingSession.setEnd(s, "11:00");

        StepResult result = handler.handle(textCtx("+37491123456"), s);

        assertThat(result).isEqualTo(StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_DONE));
        assertThat(MeetingSession.getDate(s)).isNull();
        assertThat(MeetingSession.getStart(s)).isNull();
        assertThat(MeetingSession.getEnd(s)).isNull();
        verify(telegramClient).sendHtml(eq(999999L), any(), any());
        verify(telegramClient).sendHtml(eq(100L), any(), any());
        verify(meetingRoomService).tryCreateBooking(
            eq(100L), eq("2026-04-12"), eq("10:00"), eq("11:00"), eq("+37491123456 | id:200"));
    }
}
