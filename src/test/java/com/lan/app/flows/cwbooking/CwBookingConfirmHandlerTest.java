package com.lan.app.flows.cwbooking;

import com.lan.app.config.TelegramConfig;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * app.site-url is unset in the test environment (defaults to "" per @ConfigProperty),
 * so confirmOnSite() short-circuits without making a real HTTP call, and the site-link
 * button never appears — both are exploited here to keep this test purely local.
 */
@QuarkusTest
class CwBookingConfirmHandlerTest {

    @Inject
    CwBookingConfirmHandler handler;

    @InjectMock
    TelegramClient telegramClient;

    @InjectMock
    TelegramConfig telegramConfig;

    @InjectMock
    I18n i18n;

    @BeforeEach
    void stubTranslations() {
        lenient().when(i18n.t(any(), any())).thenReturn("translated");
    }

    private static Session session() {
        return Session.newDefault(100L, 200L);
    }

    private static UpdateContext textCtx(String text) {
        return new UpdateContext(100L, "private", 200L, null, text, null, null, false, "bob", null, null, null);
    }

    @Test
    void noDeepLinkArgs_sendsBookingPromptWithHomeButtonOnly_andFinishes() {
        Session s = session();

        StepResult result = handler.handle(textCtx("/coworking"), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }

    @Test
    void withBookingId_adminConfigured_sendsConfirmMessageAndNotifiesAdmin() {
        Session s = session();
        when(telegramConfig.adminChatId()).thenReturn(999999L);

        StepResult result = handler.handle(textCtx("/start cwbooking_abc123"), s);

        assertThat(result).isEqualTo(StepResult.finish());
        // confirm message + confirm_next (with home button) both go to the chat
        verify(telegramClient, times(2)).sendHtml(eq(100L), any(), any());
        // admin gets notified once, without a keyboard
        verify(telegramClient).sendHtml(eq(999999L), any(), eq(null));
    }

    @Test
    void withBookingId_adminNotConfigured_skipsAdminNotification() {
        Session s = session();
        when(telegramConfig.adminChatId()).thenReturn(null);

        StepResult result = handler.handle(textCtx("/start cwbooking_abc123"), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verify(telegramClient, times(2)).sendHtml(eq(100L), any(), any());
        verify(telegramClient, never()).sendHtml(eq(999999L), any(), any());
    }
}
