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

/**
 * telegram.admin-chat-id resolves to 999999 in the test environment (see build.gradle.kts /
 * TG_ADMIN_CHAT_ID), so admin deliveries are asserted against that fixed chat id.
 */
@QuarkusTest
class PrintWaitFileHandlerTest {

    @Inject
    PrintWaitFileHandler handler;

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

    private static UpdateContext callbackCtx() {
        return new UpdateContext(100L, "private", 200L, 55, null, "irrelevant", "q1", true, "bob", null, null, null);
    }

    private static UpdateContext photoCtx(String fileId, String fileName) {
        return new UpdateContext(100L, "private", 200L, null, null, null, null, false, "bob", null, fileId, fileName);
    }

    private static UpdateContext noPhotoCtx() {
        return new UpdateContext(100L, "private", 200L, null, null, null, null, false, "bob", null, null, null);
    }

    @Test
    void callbackUpdate_isIgnored_staysOnWaitFile() {
        Session s = session();

        StepResult result = handler.handle(callbackCtx(), s);

        assertThat(result).isEqualTo(StepResult.stay(PrintFlowDef.FLOW, PrintFlowDef.STEP_WAIT_FILE));
        verify(telegramClient, never()).sendHtml(any(), any(), any());
        verify(telegramClient, never()).sendDocumentByFileId(any(), any(), any());
    }

    @Test
    void noPhoto_sendsErrorAndStaysOnWaitFile() {
        Session s = session();

        StepResult result = handler.handle(noPhotoCtx(), s);

        assertThat(result).isEqualTo(StepResult.stay(PrintFlowDef.FLOW, PrintFlowDef.STEP_WAIT_FILE));
        verify(telegramClient).sendHtml(eq(100L), any(), eq(null));
        verify(telegramClient, never()).sendDocumentByFileId(any(), any(), any());
    }

    @Test
    void photoWithoutDetails_recoversByClearingAndReturningToWaitDetails() {
        Session s = session(); // PrintSession.getDetails(s) is null

        StepResult result = handler.handle(photoCtx("file-1", "doc.pdf"), s);

        assertThat(result).isEqualTo(StepResult.stay(PrintFlowDef.FLOW, PrintFlowDef.STEP_WAIT_DETAILS));
        verify(telegramClient).sendHtml(eq(100L), any(), eq(null));
        verify(telegramClient, never()).sendDocumentByFileId(any(), any(), any());
    }

    @Test
    void photoWithDetails_forwardsToAdminClearsSessionAndCompletesFlow() {
        Session s = session();
        PrintSession.setDetails(s, "5 pages, double-sided");

        StepResult result = handler.handle(photoCtx("file-1", "doc.pdf"), s);

        assertThat(result).isEqualTo(StepResult.stay(PrintFlowDef.FLOW, PrintFlowDef.STEP_DONE));
        assertThat(PrintSession.getDetails(s)).isNull();
        verify(telegramClient).sendDocumentByFileId(eq(999999L), eq("file-1"), any());
        verify(telegramClient).sendHtml(eq(100L), any(), eq(null));
    }
}
