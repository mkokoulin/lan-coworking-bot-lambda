package com.lan.app.flows.siteregistration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.config.TelegramConfig;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepResult;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.testsupport.FakeHttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SiteRegistrationRetryHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private FakeHttpServer telegramServer;
    private FakeHttpServer siteServer;
    private SiteRegistrationRetryHandler handler;

    @BeforeEach
    void setUp() {
        telegramServer = new FakeHttpServer();
        siteServer = new FakeHttpServer();

        TelegramConfig config = new TelegramConfig() {
            public String botToken() { return "TESTTOKEN"; }
            public String apiBaseUrl() { return telegramServer.url(); }
            public Long adminChatId() { return 999L; }
        };
        handler = new SiteRegistrationRetryHandler(new TelegramClient(config));
        handler.siteUrl = siteServer.url();
        handler.notifySecret = "test-secret";
    }

    @AfterEach
    void tearDown() {
        telegramServer.close();
        siteServer.close();
    }

    private static UpdateContext callback(String data) {
        return new UpdateContext(555L, "private", 555L, 77, null, data, "q1", true, null, null, null, null);
    }

    @Test
    void successfulRetry_removesKeyboardAndConfirms() throws Exception {
        siteServer.onPath("/api/register/retry", 200, "{\"ok\":true,\"baserowRegId\":\"brow-1\"}");
        Session session = Session.newDefault(555L, 555L);

        StepResult result = handler.handle(callback(SiteRegistrationRetryHandler.CB_PREFIX + "reg-1"), session);

        assertEquals(StepResult.finish(), result);

        var retryRequests = siteServer.requestsTo("/api/register/retry");
        assertEquals(1, retryRequests.size());
        assertEquals("POST", retryRequests.get(0).method());
        JsonNode reqBody = mapper.readTree(retryRequests.get(0).body());
        assertEquals("reg-1", reqBody.get("id").asText());

        assertEquals(1, telegramServer.requestsTo("/botTESTTOKEN/answerCallbackQuery").size());
        assertEquals(1, telegramServer.requestsTo("/botTESTTOKEN/editMessageReplyMarkup").size());
        assertEquals(1, telegramServer.requestsTo("/botTESTTOKEN/sendMessage").size());
    }

    @Test
    void failedRetry_keepsKeyboardAndReportsFailure() {
        siteServer.onPath("/api/register/retry", 502, "{\"error\":\"guest_sync_failed\"}");
        Session session = Session.newDefault(555L, 555L);

        handler.handle(callback(SiteRegistrationRetryHandler.CB_PREFIX + "reg-1"), session);

        assertEquals(1, telegramServer.requestsTo("/botTESTTOKEN/answerCallbackQuery").size());
        assertTrue(telegramServer.requestsTo("/botTESTTOKEN/editMessageReplyMarkup").isEmpty());
        assertEquals(1, telegramServer.requestsTo("/botTESTTOKEN/sendMessage").size());
    }

    @Test
    void registrationNotFound_removesKeyboard() {
        siteServer.onPath("/api/register/retry", 404, "{\"error\":\"not_found\"}");
        Session session = Session.newDefault(555L, 555L);

        handler.handle(callback(SiteRegistrationRetryHandler.CB_PREFIX + "reg-1"), session);

        assertEquals(1, telegramServer.requestsTo("/botTESTTOKEN/editMessageReplyMarkup").size());
    }

    @Test
    void nonMatchingCallback_isIgnored() {
        Session session = Session.newDefault(555L, 555L);

        StepResult result = handler.handle(callback("something_else"), session);

        assertEquals(StepResult.finish(), result);
        assertTrue(telegramServer.requests().isEmpty());
        assertTrue(siteServer.requests().isEmpty());
    }

    @Test
    void nullCallbackData_returnsFinishWithoutSideEffects() {
        Session session = Session.newDefault(555L, 555L);

        StepResult result = handler.handle(callback(null), session);

        assertEquals(StepResult.finish(), result);
        assertTrue(telegramServer.requests().isEmpty());
        assertTrue(siteServer.requests().isEmpty());
    }

    @Test
    void missingSiteConfig_sendsWarningWithoutCallingSite() {
        handler.siteUrl = "";
        Session session = Session.newDefault(555L, 555L);

        handler.handle(callback(SiteRegistrationRetryHandler.CB_PREFIX + "reg-1"), session);

        assertTrue(siteServer.requests().isEmpty());
        assertEquals(1, telegramServer.requestsTo("/botTESTTOKEN/sendMessage").size());
    }
}
