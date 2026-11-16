package com.lan.app.flows.start;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.registration.RegistrationSession;
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
class StartDeductConfirmHandlerTest {

    @Inject
    StartDeductConfirmHandler handler;

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
        return new UpdateContext(100L, "private", 200L, null, "/deduct", null, null, false, "bob", null, null, null);
    }

    @Test
    void noTariffInSession_sendsNoTariffAndRoutesToStartShow() {
        Session s = session();

        StepResult result = handler.handle(ctx(), s);

        assertThat(result).isEqualTo(StepResult.finish());
        assertThat(s.getFlow()).isEqualTo(StartFlowDef.FLOW);
        assertThat(s.getStep()).isEqualTo(StartFlowDef.STEP_SHOW);
        verify(telegramClient).sendHtml(eq(100L), any(), eq(null));
    }

    @Test
    void tariffInSession_sendsConfirmPromptAndStaysOnDeductConfirm() {
        Session s = session();
        RegistrationSession.setDeductTariffId(s, "tariff-1");
        RegistrationSession.setDeductTariffName(s, "Day pass");

        StepResult result = handler.handle(ctx(), s);

        assertThat(result).isEqualTo(StepResult.stay(StartFlowDef.FLOW, StartFlowDef.STEP_DEDUCT_CONFIRM));
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }

    @Test
    void tariffNameBlank_stillSendsPromptWithFallbackDash() {
        Session s = session();
        RegistrationSession.setDeductTariffId(s, "tariff-1");
        RegistrationSession.setDeductTariffName(s, "");

        StepResult result = handler.handle(ctx(), s);

        assertThat(result).isEqualTo(StepResult.stay(StartFlowDef.FLOW, StartFlowDef.STEP_DEDUCT_CONFIRM));
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }
}
