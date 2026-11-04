package com.lan.app.flows.weeklydigest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.config.TelegramConfig;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.testsupport.FakeHttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeeklyDigestUnsubscribeHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private FakeHttpServer telegramServer;
    private FakeHttpServer backendServer;
    private WeeklyDigestUnsubscribeHandler handler;

    @BeforeEach
    void setUp() {
        telegramServer = new FakeHttpServer();
        backendServer = new FakeHttpServer();

        TelegramConfig config = new TelegramConfig() {
            public String botToken() { return "TESTTOKEN"; }
            public String apiBaseUrl() { return telegramServer.url(); }
            public Long adminChatId() { return 999L; }
        };
        handler = new WeeklyDigestUnsubscribeHandler(new TelegramClient(config), new I18n());
        handler.backendUrl = backendServer.url();
    }

    @AfterEach
    void tearDown() {
        telegramServer.close();
        backendServer.close();
    }

    private static UpdateContext callback(String data) {
        return new UpdateContext(555L, "private", 555L, 77, null, data, "q1", true, null, null, null, null);
    }

    @Test
    void validCallback_unsubscribesOnBackendAndConfirmsToGuest() throws Exception {
        Session session = Session.newDefault(555L, 555L);
        session.setLang("ru");

        StepResult result = handler.handle(callback("digest_unsub_661"), session);

        assertEquals(StepResult.finish(), result);

        var unsubRequests = backendServer.requestsTo("/events/v1/bot/weekly-digest/661/unsubscribe");
        assertEquals(1, unsubRequests.size());
        assertEquals("POST", unsubRequests.get(0).method());

        assertEquals(1, telegramServer.requestsTo("/botTESTTOKEN/answerCallbackQuery").size());
        assertEquals(1, telegramServer.requestsTo("/botTESTTOKEN/editMessageReplyMarkup").size());

        var sent = telegramServer.requestsTo("/botTESTTOKEN/sendMessage");
        assertEquals(1, sent.size());
        JsonNode body = mapper.readTree(sent.get(0).body());
        assertEquals(555, body.get("chat_id").asLong());
        assertFalse(body.get("text").asText().isBlank());
    }

    @Test
    void malformedGuestRowId_returnsFinishWithoutSideEffects() {
        Session session = Session.newDefault(555L, 555L);

        StepResult result = handler.handle(callback("digest_unsub_not-a-number"), session);

        assertEquals(StepResult.finish(), result);
        assertTrue(telegramServer.requests().isEmpty());
        assertTrue(backendServer.requests().isEmpty());
    }

    @Test
    void nonMatchingCallback_isIgnored() {
        Session session = Session.newDefault(555L, 555L);

        StepResult result = handler.handle(callback("something_else"), session);

        assertEquals(StepResult.finish(), result);
        assertTrue(telegramServer.requests().isEmpty());
        assertTrue(backendServer.requests().isEmpty());
    }

    @Test
    void nullCallbackData_returnsFinishWithoutSideEffects() {
        Session session = Session.newDefault(555L, 555L);

        StepResult result = handler.handle(callback(null), session);

        assertEquals(StepResult.finish(), result);
        assertTrue(telegramServer.requests().isEmpty());
        assertTrue(backendServer.requests().isEmpty());
    }

    @Test
    void guestIsStillConfirmedEvenWhenBackendUrlIsBlank() {
        handler.backendUrl = "";
        Session session = Session.newDefault(555L, 555L);

        handler.handle(callback("digest_unsub_661"), session);

        assertEquals(1, telegramServer.requestsTo("/botTESTTOKEN/sendMessage").size());
        assertTrue(backendServer.requests().isEmpty());
    }
}
