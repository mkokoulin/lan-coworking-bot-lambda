package com.lan.app.flows.cwlink;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.service.GuestService;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@QuarkusTest
class CwLoginConfirmHandlerTest {

    @Inject
    CwLoginConfirmHandler handler;

    @InjectMock
    TelegramClient telegramClient;

    @InjectMock
    I18n i18n;

    @InjectMock
    GuestService guestService;

    @BeforeEach
    void stubTranslations() {
        lenient().when(i18n.t(any(), any())).thenReturn("translated");
    }

    private static Session session() {
        return Session.newDefault(100L, 200L);
    }

    private static UpdateContext callbackCtx(String data) {
        return new UpdateContext(100L, "private", 200L, 55, null, data, "q1", data != null, "bob", null, null, null);
    }

    @Test
    void nullCallbackData_returnsFinishWithoutSideEffects() {
        Session s = session();

        StepResult result = handler.handle(callbackCtx(null), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verifyNoInteractions(telegramClient, guestService);
    }

    @Test
    void malformedUuid_returnsFinishWithoutSideEffects() {
        Session s = session();

        StepResult result = handler.handle(callbackCtx("cw_confirm_not-a-uuid"), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verifyNoInteractions(telegramClient, guestService);
    }

    @Test
    void confirmBranch_confirmsLinkAndSendsConfirmedMessage() {
        Session s = session();
        UUID id = UUID.randomUUID();

        StepResult result = handler.handle(callbackCtx("cw_confirm_" + id), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verify(guestService).confirmLink(id);
        verify(guestService, never()).rejectLink(any());
        verify(telegramClient).answerCallbackQuery("q1");
        verify(telegramClient).editMessageRemoveKeyboard(100L, 55);
        verify(telegramClient).sendHtml(eq(100L), any(), eq(null));
    }

    @Test
    void rejectBranch_rejectsLinkAndSendsRejectedMessage() {
        Session s = session();
        UUID id = UUID.randomUUID();

        StepResult result = handler.handle(callbackCtx("cw_reject_" + id), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verify(guestService).rejectLink(id);
        verify(guestService, never()).confirmLink(any());
        verify(telegramClient).answerCallbackQuery("q1");
        verify(telegramClient).editMessageRemoveKeyboard(100L, 55);
        verify(telegramClient).sendHtml(eq(100L), any(), eq(null));
    }
}
