package com.lan.app.telegram;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.lan.app.support.WireMockBackendResource;
import com.lan.app.support.WireMockInject;
import com.lan.app.telegram.dto.TelegramUpdate;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
@QuarkusTestResource(WireMockBackendResource.class)
class TelegramClientTest {

    @Inject
    TelegramClient telegramClient;

    @WireMockInject
    WireMockServer wireMock;

    @BeforeEach
    void reset() {
        wireMock.resetAll();
    }

    @Test
    void sendHtml_success_postsMessageWithExpectedBody() {
        wireMock.stubFor(post(urlPathMatching(".*/sendMessage")).willReturn(okJson("{\"ok\":true}")));

        telegramClient.sendHtml(100L, "hello", null);

        wireMock.verify(postRequestedFor(urlPathMatching(".*/sendMessage"))
            .withRequestBody(containing("\"chat_id\":100"))
            .withRequestBody(containing("\"text\":\"hello\"")));
    }

    @Test
    void sendHtml_nonOkStatus_throwsTelegramClientException() {
        wireMock.stubFor(post(urlPathMatching(".*/sendMessage")).willReturn(aResponse().withStatus(400)));

        assertThatThrownBy(() -> telegramClient.sendHtml(100L, "hi", null))
            .isInstanceOf(TelegramClientException.class);
    }

    @Test
    void answerCallbackQuery_serverError_doesNotThrow() {
        wireMock.stubFor(post(urlPathMatching(".*/answerCallbackQuery")).willReturn(aResponse().withStatus(500)));

        assertThatCode(() -> telegramClient.answerCallbackQuery("q1")).doesNotThrowAnyException();
    }

    @Test
    void answerCallbackQuery_nullId_sendsNoRequest() {
        telegramClient.answerCallbackQuery(null);

        wireMock.verify(0, postRequestedFor(urlPathMatching(".*/answerCallbackQuery")));
    }

    @Test
    void editMessageRemoveKeyboard_serverError_doesNotThrow() {
        wireMock.stubFor(post(urlPathMatching(".*/editMessageReplyMarkup")).willReturn(aResponse().withStatus(500)));

        assertThatCode(() -> telegramClient.editMessageRemoveKeyboard(100L, 5)).doesNotThrowAnyException();
    }

    @Test
    void editMessageRemoveKeyboard_nullChatId_sendsNoRequest() {
        telegramClient.editMessageRemoveKeyboard(null, 5);

        wireMock.verify(0, postRequestedFor(urlPathMatching(".*/editMessageReplyMarkup")));
    }

    @Test
    void editMessageRemoveKeyboard_nullMessageId_sendsNoRequest() {
        telegramClient.editMessageRemoveKeyboard(100L, null);

        wireMock.verify(0, postRequestedFor(urlPathMatching(".*/editMessageReplyMarkup")));
    }

    @Test
    void getUpdates_happyPath_parsesResultList() {
        wireMock.stubFor(get(urlPathMatching(".*/getUpdates")).willReturn(okJson(
            "{\"ok\":true,\"result\":[{\"update_id\":42}]}"
        )));

        List<TelegramUpdate> result = telegramClient.getUpdates(0, 0);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).update_id).isEqualTo(42L);
    }

    @Test
    void getUpdates_notOk_returnsEmptyList() {
        wireMock.stubFor(get(urlPathMatching(".*/getUpdates")).willReturn(okJson("{\"ok\":false}")));

        assertThat(telegramClient.getUpdates(0, 0)).isEmpty();
    }

    @Test
    void getUpdates_non200_returnsEmptyList() {
        wireMock.stubFor(get(urlPathMatching(".*/getUpdates")).willReturn(aResponse().withStatus(500)));

        assertThat(telegramClient.getUpdates(0, 0)).isEmpty();
    }
}
