package com.lan.app.notification;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.lan.app.config.TelegramConfig;
import com.lan.app.support.WireMockBackendResource;
import com.lan.app.support.WireMockInject;
import com.lan.app.telegram.TelegramClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(WireMockBackendResource.class)
class EventCapacityAlertSchedulerTest {

    private static final String DUE_PATH = "/events/v1/bot/event-capacity-alerts/due";

    @Inject
    EventCapacityAlertScheduler scheduler;

    @InjectMock
    TelegramClient telegramClient;

    @InjectMock
    TelegramConfig telegramConfig;

    @WireMockInject
    WireMockServer wireMock;

    @BeforeEach
    void reset() {
        wireMock.resetAll();
    }

    // Note: `app.backend-url` is forced non-blank (WireMock's base URL) by WireMockBackendResource
    // for the whole test class, so the blank-URL skip branch is exercised on a manually built,
    // non-CDI-managed instance instead (CDI client proxies for @ApplicationScoped beans don't
    // share field storage with the real delegate, so mutating the injected `scheduler`'s
    // package-private @ConfigProperty field wouldn't affect the bean actually invoked).
    @Test
    void blankBackendUrl_skipsWithoutTouchingTelegramConfigOrClient() {
        TelegramClient tc = mock(TelegramClient.class);
        TelegramConfig cfg = mock(TelegramConfig.class);
        EventCapacityAlertScheduler s = new EventCapacityAlertScheduler(tc, cfg);
        s.backendUrl = "";

        s.sendFreedCapacityAlerts();

        verifyNoInteractions(tc);
        verifyNoInteractions(cfg);
    }

    @Test
    void nullAdminChatId_skipsFetchAndSend() {
        when(telegramConfig.adminChatId()).thenReturn(null);

        scheduler.sendFreedCapacityAlerts();

        verifyNoInteractions(telegramClient);
        wireMock.verify(0, getRequestedFor(urlPathEqualTo(DUE_PATH)));
    }

    @Test
    void fetchFailure_logsAndSkipsSend() {
        when(telegramConfig.adminChatId()).thenReturn(999999L);
        wireMock.stubFor(get(urlPathEqualTo(DUE_PATH)).willReturn(aResponse().withStatus(500)));

        scheduler.sendFreedCapacityAlerts();

        verifyNoInteractions(telegramClient);
    }

    @Test
    void emptyDueList_noSend() {
        when(telegramConfig.adminChatId()).thenReturn(999999L);
        wireMock.stubFor(get(urlPathEqualTo(DUE_PATH)).willReturn(okJson("[]")));

        scheduler.sendFreedCapacityAlerts();

        verifyNoInteractions(telegramClient);
    }

    @Test
    void successfulFetch_sendsFormattedMessageToAdmin() {
        when(telegramConfig.adminChatId()).thenReturn(999999L);
        wireMock.stubFor(get(urlPathEqualTo(DUE_PATH)).willReturn(okJson(
            "[{\"eventName\":\"Jam Night\",\"registeredCount\":8,\"maxCapacity\":10}]"
        )));

        scheduler.sendFreedCapacityAlerts();

        verify(telegramClient).sendHtml(eq(999999L), contains("Jam Night"), isNull());
        verify(telegramClient).sendHtml(eq(999999L), contains("Свободно: 2 из 10"), isNull());
    }

    @Test
    void perAlertFailureIsolation_continuesToRemainingAlerts() {
        when(telegramConfig.adminChatId()).thenReturn(999999L);
        wireMock.stubFor(get(urlPathEqualTo(DUE_PATH)).willReturn(okJson(
            "[{\"eventName\":\"Event1\",\"registeredCount\":1,\"maxCapacity\":5},"
                + "{\"eventName\":\"Event2\",\"registeredCount\":2,\"maxCapacity\":5}]"
        )));
        doThrow(new RuntimeException("boom")).when(telegramClient).sendHtml(eq(999999L), contains("Event1"), any());

        scheduler.sendFreedCapacityAlerts();

        verify(telegramClient, times(2)).sendHtml(eq(999999L), any(), any());
        verify(telegramClient).sendHtml(eq(999999L), contains("Event2"), isNull());
    }
}
