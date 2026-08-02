package com.lan.app.flows.eventconfirm;

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

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(WireMockBackendResource.class)
class EventConfirmHandlerTest {

    @Inject
    EventConfirmHandler handler;

    @InjectMock
    TelegramClient telegramClient;

    @InjectMock
    I18n i18n;

    @WireMockInject
    WireMockServer wireMock;

    @BeforeEach
    void stubTranslations() {
        when(i18n.t(any(), any())).thenReturn("translated");
    }

    private static Session session() {
        return Session.newDefault(100L, 200L);
    }

    private static UpdateContext textCtx(String text) {
        IncomingUpdate u = new IncomingUpdate();
        u.setChatId(100L);
        u.setUserId(200L);
        u.setText(text);
        return UpdateContext.fromIncomingUpdate(u);
    }

    @SuppressWarnings("unchecked")
    private static List<?> keyboardRows(Object replyMarkup) {
        return (List<?>) ((Map<String, Object>) replyMarkup).get("inline_keyboard");
    }

    @Test
    void noStartArgs_sendsGenericConfirmMessage_onlyStartButton() {
        Session s = session();

        StepResult result = handler.handle(textCtx("/start"), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verify(i18n).t(eq("ru"), eq("event_confirm_message"));
        verify(i18n, times(0)).t(eq("ru"), eq("event_confirm_message_named"));

        var captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(telegramClient, times(2)).sendHtml(eq(100L), any(), captor.capture());
        Object secondMarkup = captor.getAllValues().get(1);
        assertThat(keyboardRows(secondMarkup)).hasSize(1);
    }

    @Test
    void regIdWithoutLangSuffix_doesNotChangeSessionLang() {
        Session s = session();
        wireMock.stubFor(post(urlPathEqualTo("/events/v1/registrations/abc123/confirm"))
                .willReturn(aResponse().withStatus(404)));

        handler.handle(textCtx("/start reg_abc123"), s);

        assertThat(s.getLang()).isEqualTo("ru");
    }

    @Test
    void regIdWithLangSuffix_changesSessionLangBeforeSendingMessages() {
        Session s = session();
        wireMock.stubFor(post(urlPathEqualTo("/events/v1/registrations/abc123/confirm"))
                .willReturn(aResponse().withStatus(404)));

        handler.handle(textCtx("/start reg_abc123_en"), s);

        assertThat(s.getLang()).isEqualTo("en");
        verify(i18n).t(eq("en"), eq("event_confirm_message"));
    }

    @Test
    void regId_backendConfirmsWithEventName_sendsNamedMessageAndAllButtons() {
        Session s = session();
        wireMock.stubFor(post(urlPathEqualTo("/events/v1/registrations/abc123/confirm"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"event_name\":\"Party Night\"}")));

        StepResult result = handler.handle(textCtx("/start reg_abc123_en"), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verify(i18n).t(eq("en"), eq("event_confirm_message_named"));

        var captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(telegramClient, times(2)).sendHtml(eq(100L), any(), captor.capture());
        Object secondMarkup = captor.getAllValues().get(1);
        // start button + site button (APP_SITE_URL=https://example.test in test env) + change button
        assertThat(keyboardRows(secondMarkup)).hasSize(3);
    }

    @Test
    void regId_backendReturnsNon200_fallsBackToGenericMessage() {
        Session s = session();
        wireMock.stubFor(post(urlPathEqualTo("/events/v1/registrations/def456/confirm"))
                .willReturn(aResponse().withStatus(500)));

        handler.handle(textCtx("/start reg_def456_en"), s);

        verify(i18n).t(eq("en"), eq("event_confirm_message"));
    }

    @Test
    void regId_backendReturnsMalformedJson_treatedAsNoEventName() {
        Session s = session();
        wireMock.stubFor(post(urlPathEqualTo("/events/v1/registrations/ghi789/confirm"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("not json")));

        StepResult result = handler.handle(textCtx("/start reg_ghi789_en"), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verify(i18n).t(eq("en"), eq("event_confirm_message"));
    }

    @Test
    void keyboardSendFails_fallsBackToSafeButtonsOnly() {
        Session s = session();
        wireMock.stubFor(post(urlPathEqualTo("/events/v1/registrations/abc123/confirm"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"event_name\":\"Party Night\"}")));
        // 1st call: plain confirm text (no keyboard) — succeeds.
        // 2nd call: full keyboard (start + site + change) — Telegram rejects it (e.g. bad site URL).
        // 3rd call: fallback keyboard (start + change only) — succeeds.
        doNothing()
                .doThrow(new RuntimeException("Telegram rejected the keyboard"))
                .doNothing()
                .when(telegramClient).sendHtml(eq(100L), any(), any());

        StepResult result = handler.handle(textCtx("/start reg_abc123_en"), s);

        assertThat(result).isEqualTo(StepResult.finish());
        var captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(telegramClient, times(3)).sendHtml(eq(100L), any(), captor.capture());
        Object fallbackMarkup = captor.getAllValues().get(2);
        // start button + change button only — no site button in the fallback
        assertThat(keyboardRows(fallbackMarkup)).hasSize(2);
    }

    @Test
    void nonRegArgs_treatedAsNoRegId() {
        Session s = session();

        handler.handle(textCtx("/start some_other_payload"), s);

        verify(i18n).t(eq("ru"), eq("event_confirm_message"));
    }
}
