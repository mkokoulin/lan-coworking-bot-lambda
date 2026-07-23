package com.lan.app.notification;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
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
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(WireMockBackendResource.class)
class EventNotificationSchedulerTest {

    private static final String DUE_PATH = "/events/v1/bot/event-notifications/due";

    @Inject
    EventNotificationScheduler scheduler;

    @InjectMock
    TelegramClient telegramClient;

    @WireMockInject
    WireMockServer wireMock;

    @BeforeEach
    void reset() {
        wireMock.resetAll();
    }

    // See EventCapacityAlertSchedulerTest for why the blank-URL branch needs a manually
    // constructed (non-CDI-proxied) instance rather than mutating the @Inject'd bean's field.
    @Test
    void blankBackendUrl_skipsWithoutTouchingTelegramClient() {
        TelegramClient tc = mock(TelegramClient.class);
        EventNotificationScheduler s = new EventNotificationScheduler(tc);
        s.backendUrl = "";

        s.sendDueNotifications();

        verifyNoInteractions(tc);
    }

    @Test
    void fetchFailure_logsAndSkips() {
        wireMock.stubFor(get(urlPathEqualTo(DUE_PATH)).willReturn(aResponse().withStatus(500)));

        scheduler.sendDueNotifications();

        verifyNoInteractions(telegramClient);
    }

    @Test
    void emptyDueList_noOp() {
        wireMock.stubFor(get(urlPathEqualTo(DUE_PATH)).willReturn(okJson("[]")));

        scheduler.sendDueNotifications();

        verifyNoInteractions(telegramClient);
    }

    @Test
    void nullRecipients_producesNoResultsAndNoSaveCall() {
        wireMock.stubFor(get(urlPathEqualTo(DUE_PATH)).willReturn(okJson(
            "[{\"id\":1,\"message\":\"hi\",\"eventName\":\"Ev\",\"recipients\":null}]"
        )));

        scheduler.sendDueNotifications();

        verifyNoInteractions(telegramClient);
        wireMock.verify(0, postRequestedFor(urlPathEqualTo("/events/v1/bot/event-notifications/1/results")));
    }

    @Test
    void emptyRecipients_producesNoResultsAndNoSaveCall() {
        wireMock.stubFor(get(urlPathEqualTo(DUE_PATH)).willReturn(okJson(
            "[{\"id\":1,\"message\":\"hi\",\"eventName\":\"Ev\",\"recipients\":[]}]"
        )));

        scheduler.sendDueNotifications();

        verifyNoInteractions(telegramClient);
        wireMock.verify(0, postRequestedFor(urlPathEqualTo("/events/v1/bot/event-notifications/1/results")));
    }

    @Test
    void recipientWithNullChatId_isSkipped_onlyValidRecipientNotified() {
        wireMock.stubFor(get(urlPathEqualTo(DUE_PATH)).willReturn(okJson(
            "[{\"id\":1,\"message\":\"hi\",\"eventName\":\"Ev\",\"recipients\":["
                + "{\"chatId\":null,\"guestRowId\":1,\"registrationRowId\":10},"
                + "{\"chatId\":200,\"guestRowId\":2,\"registrationRowId\":20}]}]"
        )));
        wireMock.stubFor(post(urlPathEqualTo("/events/v1/bot/event-notifications/1/results"))
            .willReturn(aResponse().withStatus(200)));

        scheduler.sendDueNotifications();

        verify(telegramClient, times(1)).sendHtml(eq(200L), any(), any());
        wireMock.verify(postRequestedFor(urlPathEqualTo("/events/v1/bot/event-notifications/1/results"))
            .withRequestBody(containing("\"guestRowId\":2"))
            .withRequestBody(containing("\"status\":\"SENT\"")));
    }

    @Test
    void perRecipientFailureIsolation_producesSentAndFailedResults() {
        wireMock.stubFor(get(urlPathEqualTo(DUE_PATH)).willReturn(okJson(
            "[{\"id\":1,\"message\":\"hi\",\"eventName\":\"Ev\",\"recipients\":["
                + "{\"chatId\":100,\"guestRowId\":1,\"registrationRowId\":10},"
                + "{\"chatId\":200,\"guestRowId\":2,\"registrationRowId\":20}]}]"
        )));
        wireMock.stubFor(post(urlPathEqualTo("/events/v1/bot/event-notifications/1/results"))
            .willReturn(aResponse().withStatus(200)));
        doThrow(new RuntimeException("boom")).when(telegramClient).sendHtml(eq(100L), any(), any());

        scheduler.sendDueNotifications();

        verify(telegramClient, times(2)).sendHtml(anyLong(), any(), any());
        wireMock.verify(postRequestedFor(urlPathEqualTo("/events/v1/bot/event-notifications/1/results"))
            .withRequestBody(containing("\"status\":\"FAILED\""))
            .withRequestBody(containing("\"status\":\"SENT\"")));
    }

    @Test
    void saveResults_nonOkStatus_logsButDoesNotThrow() {
        wireMock.stubFor(get(urlPathEqualTo(DUE_PATH)).willReturn(okJson(
            "[{\"id\":1,\"message\":\"hi\",\"eventName\":\"Ev\",\"recipients\":["
                + "{\"chatId\":100,\"guestRowId\":1,\"registrationRowId\":10}]}]"
        )));
        wireMock.stubFor(post(urlPathEqualTo("/events/v1/bot/event-notifications/1/results"))
            .willReturn(aResponse().withStatus(500)));

        assertThatCode(() -> scheduler.sendDueNotifications()).doesNotThrowAnyException();
    }

    @Test
    void saveResults_connectionFault_logsButDoesNotThrow() {
        wireMock.stubFor(get(urlPathEqualTo(DUE_PATH)).willReturn(okJson(
            "[{\"id\":1,\"message\":\"hi\",\"eventName\":\"Ev\",\"recipients\":["
                + "{\"chatId\":100,\"guestRowId\":1,\"registrationRowId\":10}]}]"
        )));
        wireMock.stubFor(post(urlPathEqualTo("/events/v1/bot/event-notifications/1/results"))
            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThatCode(() -> scheduler.sendDueNotifications()).doesNotThrowAnyException();
    }
}
