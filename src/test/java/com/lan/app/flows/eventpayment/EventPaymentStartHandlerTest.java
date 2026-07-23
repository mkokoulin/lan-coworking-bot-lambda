package com.lan.app.flows.eventpayment;

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
class EventPaymentStartHandlerTest {

    @Inject
    EventPaymentStartHandler handler;

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

    private static UpdateContext ctx() {
        return new UpdateContext(100L, "private", 200L, null, "/pay", null, null, false, "bob", null, null, null);
    }

    @Test
    void handle_advancesToWaitPhotoStep() {
        Session s = session();

        StepResult result = handler.handle(ctx(), s);

        assertThat(result).isEqualTo(StepResult.stay(EventPaymentFlowDef.FLOW, EventPaymentFlowDef.STEP_WAIT_PHOTO));
        assertThat(s.getStep()).isEqualTo(EventPaymentFlowDef.STEP_WAIT_PHOTO);
        verify(telegramClient).sendHtml(eq(100L), any(), eq(null));
    }

    @Test
    void handle_withPriceSet_formatsInstructionsWithPriceAndCardNumber() {
        Session s = session();
        EventPaymentSession.setPrice(s, "1500");
        when(i18n.t("ru", "event_payment_instructions")).thenReturn("Price:%s Card:%s");

        handler.handle(ctx(), s);

        verify(telegramClient).sendHtml(eq(100L), eq("Price:1500 Card:0000 0000 0000 0000"), eq(null));
    }

    @Test
    void handle_withoutPrice_defaultsToQuestionMark() {
        Session s = session();
        when(i18n.t("ru", "event_payment_instructions")).thenReturn("Price:%s Card:%s");

        handler.handle(ctx(), s);

        verify(telegramClient).sendHtml(eq(100L), eq("Price:? Card:0000 0000 0000 0000"), eq(null));
    }
}
