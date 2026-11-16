package com.lan.app.flows.eventnotify;

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

/** Covers the guest-facing half of the reminder feature: tapping a Yes/No button. */
class EventNotifyActionHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private FakeHttpServer telegramServer;
    private FakeHttpServer backendServer;
    private EventNotifyActionHandler handler;

    @BeforeEach
    void setUp() {
        telegramServer = new FakeHttpServer();
        backendServer = new FakeHttpServer();

        TelegramConfig config = new TelegramConfig() {
            public String botToken() { return "TESTTOKEN"; }
            public String apiBaseUrl() { return telegramServer.url(); }
            public Long adminChatId() { return 999L; }
        };
        handler = new EventNotifyActionHandler(new TelegramClient(config), new I18n());
        handler.backendUrl = backendServer.url();
        handler.adminChatId = 999L;
    }

    @AfterEach
    void tearDown() {
        telegramServer.close();
        backendServer.close();
    }

    private UpdateContext callback(String data) {
        return new UpdateContext(555L, "private", 555L, null, null, "/" + data, null, true, null, null, null, null);
    }

    @Test
    void yesTapRecordsConfirmedActionAndThanksGuest() throws Exception {
        Session session = Session.newDefault(555L, 555L);
        session.setLang("ru");

        handler.handle(callback("en_y_10_20_30"), session);

        var actionRequests = backendServer.requestsTo("/events/v1/bot/event-notifications/10/action");
        assertEquals(1, actionRequests.size());
        JsonNode body = mapper.readTree(actionRequests.get(0).body());
        assertEquals(20, body.get("guestRowId").asInt());
        assertEquals(30, body.get("registrationRowId").asInt());
        assertEquals("CONFIRMED", body.get("action").asText());

        var telegramRequests = telegramServer.requestsTo("/botTESTTOKEN/sendMessage");
        assertEquals(1, telegramRequests.size());
        JsonNode tgBody = mapper.readTree(telegramRequests.get(0).body());
        assertEquals(555, tgBody.get("chat_id").asLong());
        assertFalse(tgBody.get("text").asText().isBlank());
    }

    @Test
    void noTapRecordsDeclinedAction() throws Exception {
        Session session = Session.newDefault(555L, 555L);
        session.setLang("en");

        handler.handle(callback("en_n_11_21_31"), session);

        var actionRequests = backendServer.requestsTo("/events/v1/bot/event-notifications/11/action");
        assertEquals(1, actionRequests.size());
        JsonNode body = mapper.readTree(actionRequests.get(0).body());
        assertEquals("DECLINED", body.get("action").asText());
    }

    @Test
    void malformedPayloadIsIgnoredWithoutCallingBackendOrTelegram() {
        Session session = Session.newDefault(555L, 555L);

        StepResult result = handler.handle(callback("en_y_not-a-number_2_3"), session);

        assertEquals(StepResult.finish(), result);
        assertTrue(backendServer.requests().isEmpty());
        assertTrue(telegramServer.requests().isEmpty());
    }

    @Test
    void nonEventNotifyCallbackIsIgnored() {
        Session session = Session.newDefault(555L, 555L);
        StepResult result = handler.handle(callback("something_else"), session);
        assertEquals(StepResult.finish(), result);
        assertTrue(backendServer.requests().isEmpty());
    }

    @Test
    void guestIsToldAboutFailureWhenBackendUrlIsBlank() throws Exception {
        // Previously the guest was thanked regardless of whether the answer was ever recorded —
        // so a misconfigured/unreachable backend looked identical to a real confirmation, and the
        // scheduler kept re-sending the same reminder since nothing was ever persisted.
        handler.backendUrl = "";
        Session session = Session.newDefault(555L, 555L);
        session.setLang("ru");

        handler.handle(callback("en_y_10_20_30"), session);

        var telegramRequests = telegramServer.requestsTo("/botTESTTOKEN/sendMessage");
        JsonNode toGuest = mapper.readTree(telegramRequests.get(0).body());
        assertEquals(555, toGuest.get("chat_id").asLong());
        assertTrue(toGuest.get("text").asText().toLowerCase().contains("не получилось"));
        assertTrue(backendServer.requests().isEmpty());
    }

    @Test
    void guestIsToldAboutFailureAndAdminIsAlertedWhenBackendRejectsTheAction() throws Exception {
        backendServer.onPath("/events/v1/bot/event-notifications/10/action", 500, "{}");
        Session session = Session.newDefault(555L, 555L);
        session.setLang("ru");

        handler.handle(callback("en_y_10_20_30"), session);

        var guestMessages = telegramServer.requestsTo("/botTESTTOKEN/sendMessage");
        assertEquals(2, guestMessages.size());

        JsonNode toGuest = mapper.readTree(guestMessages.get(0).body());
        assertEquals(555, toGuest.get("chat_id").asLong());
        assertTrue(toGuest.get("text").asText().toLowerCase().contains("не получилось"));

        JsonNode toAdmin = mapper.readTree(guestMessages.get(1).body());
        assertEquals(999, toAdmin.get("chat_id").asLong());
    }
}
