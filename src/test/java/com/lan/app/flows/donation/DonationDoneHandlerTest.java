package com.lan.app.flows.donation;

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
class DonationDoneHandlerTest {

    @Inject
    DonationDoneHandler handler;

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
        return new UpdateContext(100L, "private", 200L, null, null, "donation:done", null, true, "bob", null, null, null);
    }

    @Test
    void handle_sendsAlwaysMessageAndClearsFlowAndStep() {
        Session s = session();
        s.setFlow(DonationFlowDef.FLOW);
        s.setStep(DonationFlowDef.STEP_HOME);

        StepResult result = handler.handle(ctx(), s);

        assertThat(result).isEqualTo(StepResult.finish());
        assertThat(s.getFlow()).isEmpty();
        assertThat(s.getStep()).isEmpty();
        verify(telegramClient).sendHtml(eq(100L), any(), eq(null));
    }
}
