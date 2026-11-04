package com.lan.app.flows.start;

import com.lan.app.client.baserow.model.CoworkingGuestTariffResponse;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.registration.RegistrationSession;
import com.lan.app.i18n.I18n;
import com.lan.app.service.TariffService;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class StartDeductDoHandlerTest {

    @Inject
    StartDeductDoHandler handler;

    @InjectMock
    TelegramClient telegramClient;

    @InjectMock
    I18n i18n;

    @InjectMock
    TariffService tariffService;

    @BeforeEach
    void stubTranslations() {
        when(i18n.t(any(), any())).thenReturn("translated");
    }

    private static Session session() {
        return Session.newDefault(100L, 200L);
    }

    private static UpdateContext ctx() {
        return new UpdateContext(100L, "private", 200L, 55, null, "deduct_do", "q1", true, "bob", null, null, null);
    }

    @Test
    void noTariffInSession_sendsNoTariffAndRoutesToStartShow() {
        Session s = session();

        StepResult result = handler.handle(ctx(), s);

        assertThat(result).isEqualTo(StepResult.finish());
        assertThat(s.getFlow()).isEqualTo(StartFlowDef.FLOW);
        assertThat(s.getStep()).isEqualTo(StartFlowDef.STEP_SHOW);
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }

    @Test
    void deductFails_sendsErrorAndRoutesToProfile() {
        Session s = session();
        UUID tariffId = UUID.randomUUID();
        RegistrationSession.setDeductTariffId(s, tariffId.toString());
        when(tariffService.deductDay(tariffId)).thenReturn(Optional.empty());

        StepResult result = handler.handle(ctx(), s);

        assertThat(result).isEqualTo(StepResult.finish());
        assertThat(s.getFlow()).isEqualTo(StartFlowDef.FLOW);
        assertThat(s.getStep()).isEqualTo(StartFlowDef.STEP_PROFILE);
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }

    @Test
    void deductSucceeds_sendsSuccessMessageAndRoutesToProfile() {
        Session s = session();
        UUID tariffId = UUID.randomUUID();
        RegistrationSession.setDeductTariffId(s, tariffId.toString());
        CoworkingGuestTariffResponse updated = mock(CoworkingGuestTariffResponse.class);
        when(updated.getDaysUsed()).thenReturn(3);
        when(tariffService.deductDay(tariffId)).thenReturn(Optional.of(updated));

        StepResult result = handler.handle(ctx(), s);

        assertThat(result).isEqualTo(StepResult.finish());
        assertThat(s.getFlow()).isEqualTo(StartFlowDef.FLOW);
        assertThat(s.getStep()).isEqualTo(StartFlowDef.STEP_PROFILE);
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }
}
