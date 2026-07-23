package com.lan.app.notification;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepResult;
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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@QuarkusTest
@QuarkusTestResource(WireMockBackendResource.class)
class EventAttendanceHandlerTest {

    private static final String ACTION_PATH = "/events/v1/bot/event-notifications/1/action";

    @Inject
    EventAttendanceHandler handler;

    @InjectMock
    TelegramClient telegramClient;

    @WireMockInject
    WireMockServer wireMock;

    @BeforeEach
    void reset() {
        wireMock.resetAll();
    }

    private static Session session() {
        return Session.newDefault(100L, 200L);
    }

    private static UpdateContext ctx(String callbackData) {
        return new UpdateContext(100L, "private", 200L, 55, null, callbackData, "q1", true, "bob", null, null, null);
    }

    @Test
    void nullCallbackData_returnsFinishWithoutTelegramInteraction() {
        StepResult result = handler.handle(ctx(null), session());

        assertThat(result).isEqualTo(StepResult.finish());
        verifyNoInteractions(telegramClient);
    }

    @Test
    void malformedPayload_wrongPartCount_returnsFinishWithoutInteraction() {
        StepResult result = handler.handle(ctx("evt_att_yes_1_2"), session());

        assertThat(result).isEqualTo(StepResult.finish());
        verifyNoInteractions(telegramClient);
    }

    @Test
    void nonNumericParts_returnsFinishWithoutInteraction() {
        StepResult result = handler.handle(ctx("evt_att_yes_abc_2_3"), session());

        assertThat(result).isEqualTo(StepResult.finish());
        verifyNoInteractions(telegramClient);
    }

    @Test
    void validYesCallback_answersEditsRecordsAndSendsConfirmation() {
        wireMock.stubFor(post(urlPathEqualTo(ACTION_PATH)).willReturn(aResponse().withStatus(200)));

        StepResult result = handler.handle(ctx("evt_att_yes_1_2_3"), session());

        assertThat(result).isEqualTo(StepResult.finish());
        verify(telegramClient).answerCallbackQuery("q1");
        verify(telegramClient).editMessageRemoveKeyboard(100L, 55);
        verify(telegramClient).sendHtml(eq(100L), contains("Отлично"), isNull());
        wireMock.verify(postRequestedFor(urlPathEqualTo(ACTION_PATH))
            .withRequestBody(containing("\"action\":\"CONFIRMED\""))
            .withRequestBody(containing("\"guestRowId\":2"))
            .withRequestBody(containing("\"registrationRowId\":3")));
    }

    @Test
    void validNoCallback_answersEditsRecordsAndSendsDeclineConfirmation() {
        wireMock.stubFor(post(urlPathEqualTo(ACTION_PATH)).willReturn(aResponse().withStatus(200)));

        StepResult result = handler.handle(ctx("evt_att_no_1_2_3"), session());

        assertThat(result).isEqualTo(StepResult.finish());
        verify(telegramClient).answerCallbackQuery("q1");
        verify(telegramClient).editMessageRemoveKeyboard(100L, 55);
        verify(telegramClient).sendHtml(eq(100L), contains("Жаль"), isNull());
        wireMock.verify(postRequestedFor(urlPathEqualTo(ACTION_PATH))
            .withRequestBody(containing("\"action\":\"DECLINED\"")));
    }

    @Test
    void recordAction_nonOkStatus_logsAndStillSendsConfirmation() {
        wireMock.stubFor(post(urlPathEqualTo(ACTION_PATH)).willReturn(aResponse().withStatus(500)));

        assertThatCode(() -> {
            StepResult result = handler.handle(ctx("evt_att_yes_1_2_3"), session());
            assertThat(result).isEqualTo(StepResult.finish());
        }).doesNotThrowAnyException();

        verify(telegramClient).sendHtml(eq(100L), contains("Отлично"), isNull());
    }

    @Test
    void recordAction_connectionFault_logsAndStillSendsConfirmation() {
        wireMock.stubFor(post(urlPathEqualTo(ACTION_PATH))
            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThatCode(() -> handler.handle(ctx("evt_att_yes_1_2_3"), session())).doesNotThrowAnyException();

        verify(telegramClient).sendHtml(eq(100L), contains("Отлично"), isNull());
    }
}
