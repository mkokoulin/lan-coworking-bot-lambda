package com.lan.app.flows.eventpayment;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class EventPaymentWaitPhotoHandlerTest {

    @Inject
    EventPaymentWaitPhotoHandler handler;

    @InjectMock
    TelegramClient telegramClient;

    @InjectMock
    I18n i18n;

    @InjectMock
    PaymentPendingStore pendingStore;

    @BeforeEach
    void stubTranslations() {
        when(i18n.t(any(), any())).thenReturn("translated");
    }

    private static Session session() {
        return Session.newDefault(100L, 200L);
    }

    private static UpdateContext ctxWithoutPhoto() {
        IncomingUpdate u = new IncomingUpdate();
        u.setChatId(100L);
        u.setUserId(200L);
        u.setText("hi");
        return UpdateContext.fromIncomingUpdate(u);
    }

    private static UpdateContext ctxWithPhoto(String fileId, String username) {
        IncomingUpdate u = new IncomingUpdate();
        u.setChatId(100L);
        u.setUserId(200L);
        u.setFileId(fileId);
        u.setUsername(username);
        return UpdateContext.fromIncomingUpdate(u);
    }

    @Test
    void noPhoto_sendsErrorAndStaysOnStep() {
        Session s = session();

        StepResult result = handler.handle(ctxWithoutPhoto(), s);

        assertThat(result).isEqualTo(StepResult.stay(EventPaymentFlowDef.FLOW, EventPaymentFlowDef.STEP_WAIT_PHOTO));
        verify(telegramClient).sendHtml(eq(100L), any(), isNull());
        verify(pendingStore, never()).store(any(), any());
    }

    @Test
    void withPhoto_storesPendingPayment_notifiesAdminAndUser_clearsFlow() {
        Session s = session();
        EventPaymentSession.setRegId(s, "reg-1");
        EventPaymentSession.setPrice(s, "1500");
        s.setFlow(EventPaymentFlowDef.FLOW);
        s.setStep(EventPaymentFlowDef.STEP_WAIT_PHOTO);

        StepResult result = handler.handle(ctxWithPhoto("file-1", "bob"), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verify(pendingStore).store("reg-1", 100L);
        verify(telegramClient).sendPhotoByFileId(eq(999999L), eq("file-1"), any(), any());
        verify(telegramClient).sendHtml(eq(100L), any(), isNull());
        assertThat(s.getFlow()).isEmpty();
        assertThat(s.getStep()).isEmpty();
    }

    @Test
    void withPhoto_captionUsesUsernameAndPriceAndRegId() {
        Session s = session();
        EventPaymentSession.setRegId(s, "reg-9");
        EventPaymentSession.setPrice(s, "2000");
        when(i18n.t("ru", "event_payment_admin_caption")).thenReturn("%s|%s|%s");

        handler.handle(ctxWithPhoto("file-1", "bob"), s);

        verify(telegramClient).sendPhotoByFileId(eq(999999L), eq("file-1"), eq("@bob|2000|reg-9"), any());
    }

    @Test
    void withPhoto_noUsername_captionFallsBackToChatId() {
        Session s = session();
        EventPaymentSession.setRegId(s, "reg-9");
        EventPaymentSession.setPrice(s, "2000");
        when(i18n.t("ru", "event_payment_admin_caption")).thenReturn("%s|%s|%s");

        handler.handle(ctxWithPhoto("file-1", null), s);

        verify(telegramClient).sendPhotoByFileId(eq(999999L), eq("file-1"), eq("100|2000|reg-9"), any());
    }

    @Test
    void withPhoto_noPrice_defaultsToQuestionMarkInCaption() {
        Session s = session();
        EventPaymentSession.setRegId(s, "reg-9");
        when(i18n.t("ru", "event_payment_admin_caption")).thenReturn("%s|%s|%s");

        handler.handle(ctxWithPhoto("file-1", "bob"), s);

        verify(telegramClient).sendPhotoByFileId(eq(999999L), eq("file-1"), eq("@bob|?|reg-9"), any());
    }
}
