package com.lan.app.flows.coworking;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.session.Session;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class CoworkingHomeHandlerTest {

    @Inject
    CoworkingHomeHandler handler;

    @InjectMock
    TelegramClient telegramClient;

    @InjectMock
    I18n i18n;

    @InjectMock
    CoworkingPricingService pricingService;

    @BeforeEach
    void stubTranslations() {
        when(i18n.t(any(), any())).thenReturn("translated");
    }

    private static Session session() {
        return Session.newDefault(100L, 200L);
    }

    private static UpdateContext ctx() {
        return new UpdateContext(100L, "private", 200L, null, "/coworking", null, null, false, "bob", null, null, null);
    }

    @Test
    void noTariffs_fallsBackToStaticPricesText() {
        when(pricingService.formatPricesBlock(anyString())).thenReturn("translated");
        Session s = session();

        StepResult result = handler.handle(ctx(), s);

        assertThat(result).isEqualTo(StepResult.stay(CoworkingFlowDef.FLOW, CoworkingFlowDef.STEP_HOME));
        verify(telegramClient).sendHtml(eq(100L), anyString(), any());
    }

    @Test
    void withTariffs_buildsFormattedPriceListInText() {
        when(pricingService.formatPricesBlock(anyString()))
                .thenReturn("💳 Coworking prices:\n• Full Day — 12 345֏");
        Session s = session();

        handler.handle(ctx(), s);

        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(telegramClient).sendHtml(eq(100L), captor.capture(), any());
        assertThat(captor.getValue())
                .contains("Full Day")
                .contains("12 345");
    }
}
