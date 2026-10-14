package com.lan.app.flows.eventpayment;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.lan.app.domain.IncomingUpdate;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.support.WireMockBackendResource;
import com.lan.app.support.WireMockInject;
import com.lan.app.telegram.TelegramClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(WireMockBackendResource.class)
class EventPaymentAdminHandlerTest {

    @Inject
    EventPaymentAdminHandler handler;

    @InjectMock
    TelegramClient telegramClient;

    @InjectMock
    I18n i18n;

    @InjectMock
    PaymentPendingStore pendingStore;

    @WireMockInject
    WireMockServer wireMock;

    @BeforeEach
    void stubTranslations() {
        when(i18n.t(any(), any())).thenReturn("translated");
        when(pendingStore.getUserChatId(any())).thenReturn(Optional.empty());
    }

    private static Session session() {
        return Session.newDefault(100L, 200L);
    }

    private static UpdateContext callbackCtx(String data) {
        IncomingUpdate u = new IncomingUpdate();
        u.setChatId(100L);
        u.setUserId(200L);
        u.setCallbackData(data);
        return UpdateContext.fromIncomingUpdate(u);
    }

    @Test
    void nullCallbackData_returnsFinishWithoutSideEffects() {
        Session s = session();
        UpdateContext ctx = new UpdateContext(100L, "private", 200L, null, null, null, null, false, null, null, null, null);

        StepResult result = handler.handle(ctx, s);

        assertThat(result).isEqualTo(StepResult.finish());
        verifyNoInteractions(telegramClient);
    }

    @Test
    void approve_backendReturnsChatId_notifiesUserAndAdminAndRemovesPending() {
        Session s = session();
        wireMock.stubFor(post(urlPathEqualTo("/events/v1/payments/42/approve"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"chatId\":555}")));

        StepResult result = handler.handle(callbackCtx("pay_approve_42"), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verify(telegramClient).sendHtml(eq(555L), any(), eq(null));
        verify(telegramClient).sendHtml(eq(100L), any(), eq(null));
        verify(pendingStore).remove("42");
        assertThat(s.getFlow()).isEmpty();
        assertThat(s.getStep()).isEmpty();
    }

    @Test
    void approve_paymentEndpointHasNoChatId_fallsBackToMarkPaidEndpoint() {
        Session s = session();
        wireMock.stubFor(post(urlPathEqualTo("/events/v1/payments/42/approve"))
                .willReturn(aResponse().withStatus(404)));
        wireMock.stubFor(post(urlPathEqualTo("/events/v1/registrations/42/mark-paid"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"chatId\":777}")));

        handler.handle(callbackCtx("pay_approve_42"), s);

        verify(telegramClient).sendHtml(eq(777L), any(), eq(null));
    }

    @Test
    void approve_bothBackendCallsFail_fallsBackToPendingStore() {
        Session s = session();
        wireMock.stubFor(post(urlPathEqualTo("/events/v1/payments/42/approve"))
                .willReturn(aResponse().withStatus(500)));
        wireMock.stubFor(post(urlPathEqualTo("/events/v1/registrations/42/mark-paid"))
                .willReturn(aResponse().withStatus(500)));
        when(pendingStore.getUserChatId("42")).thenReturn(Optional.of(321L));

        handler.handle(callbackCtx("pay_approve_42"), s);

        verify(telegramClient).sendHtml(eq(321L), any(), eq(null));
    }

    @Test
    void approve_noChatIdResolvedAnywhere_onlyNotifiesAdmin() {
        Session s = session();
        wireMock.stubFor(post(urlPathEqualTo("/events/v1/payments/42/approve"))
                .willReturn(aResponse().withStatus(500)));
        wireMock.stubFor(post(urlPathEqualTo("/events/v1/registrations/42/mark-paid"))
                .willReturn(aResponse().withStatus(500)));

        handler.handle(callbackCtx("pay_approve_42"), s);

        verify(telegramClient, never()).sendHtml(eq(555L), any(), any());
        verify(telegramClient).sendHtml(eq(100L), any(), eq(null));
    }

    @Test
    void reject_backendReturnsChatId_notifiesUserAndAdmin() {
        Session s = session();
        wireMock.stubFor(post(urlPathEqualTo("/events/v1/payments/99/reject"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"chatId\":333}")));

        StepResult result = handler.handle(callbackCtx("pay_reject_99"), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verify(telegramClient).sendHtml(eq(333L), any(), eq(null));
        verify(telegramClient).sendHtml(eq(100L), any(), eq(null));
        verify(pendingStore).remove("99");
    }

    @Test
    void reject_backendFails_fallsBackToPendingStore() {
        Session s = session();
        wireMock.stubFor(post(urlPathEqualTo("/events/v1/payments/99/reject"))
                .willReturn(aResponse().withStatus(500)));
        when(pendingStore.getUserChatId("99")).thenReturn(Optional.of(654L));

        handler.handle(callbackCtx("pay_reject_99"), s);

        verify(telegramClient).sendHtml(eq(654L), any(), eq(null));
    }
}
