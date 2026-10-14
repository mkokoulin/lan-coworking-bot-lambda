package com.lan.app.flows.myevents;

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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MyEventsGuestsWaitHandlerTest {

    private static final long ADMIN_CHAT_ID = 999L;

    private final ObjectMapper mapper = new ObjectMapper();
    private FakeHttpServer telegramServer;
    private FakeHttpServer backendServer;
    private MyEventsGuestsWaitHandler handler;

    @BeforeEach
    void setUp() {
        telegramServer = new FakeHttpServer();
        backendServer = new FakeHttpServer();
        backendServer.onPath("/events/v1/bot/my-registrations", 200, "[]");

        TelegramConfig config = new TelegramConfig() {
            public String botToken() { return "TESTTOKEN"; }
            public String apiBaseUrl() { return telegramServer.url(); }
            public Long adminChatId() { return ADMIN_CHAT_ID; }
        };
        TelegramClient telegramClient = new TelegramClient(config);
        MyEventsListHandler listHandler = new MyEventsListHandler(telegramClient, new I18n());
        listHandler.backendUrl = backendServer.url();

        handler = new MyEventsGuestsWaitHandler(telegramClient, new I18n(), listHandler);
        handler.backendUrl = backendServer.url();
        handler.adminChatId = ADMIN_CHAT_ID;
    }

    @AfterEach
    void tearDown() {
        telegramServer.close();
        backendServer.close();
    }

    private UpdateContext text(String messageText) {
        return new UpdateContext(555L, "private", 555L, null, messageText, null, null, false, null, null, null, null);
    }

    private UpdateContext callback(String data) {
        return new UpdateContext(555L, "private", 555L, null, null, "/" + data, null, true, null, null, null, null);
    }

    private List<JsonNode> sentMessages() throws Exception {
        var out = new ArrayList<JsonNode>();
        for (var r : telegramServer.requestsTo("/botTESTTOKEN/sendMessage")) {
            out.add(mapper.readTree(r.body()));
        }
        return out;
    }

    @Test
    void ignoresCallbackTapsAndStaysWaiting() throws Exception {
        Session session = Session.newDefault(555L, 555L);
        MyEventsSession.setPendingRegId(session, "reg-1");

        StepResult result = handler.handle(callback("something"), session);

        assertEquals(MyEventsFlowDef.STEP_GUESTS_WAIT, result.nextStep());
        assertTrue(sentMessages().isEmpty());
        assertTrue(backendServer.requestsTo("/events/v1/bot/registrations").isEmpty());
    }

    @Test
    void noPendingRegIdDelegatesStraightToList() throws Exception {
        Session session = Session.newDefault(555L, 555L);

        handler.handle(text("3"), session);

        assertEquals(1, backendServer.requestsTo("/events/v1/bot/my-registrations").size());
        assertEquals(1, sentMessages().size());
    }

    @Test
    void nonNumericTextShowsInvalidMessageAndStaysWaiting() throws Exception {
        Session session = Session.newDefault(555L, 555L);
        session.setLang("ru");
        MyEventsSession.setPendingRegId(session, "reg-1");

        StepResult result = handler.handle(text("abc"), session);

        assertEquals(MyEventsFlowDef.STEP_GUESTS_WAIT, result.nextStep());
        assertEquals("reg-1", MyEventsSession.getPendingRegId(session));
        var sent = sentMessages();
        assertEquals(1, sent.size());
        assertTrue(sent.get(0).get("text").asText().contains("положительное целое число"));
        assertTrue(backendServer.requestsTo("/events/v1/bot/registrations/reg-1/guest-count").isEmpty());
    }

    @Test
    void zeroOrNegativeCountShowsInvalidMessage() throws Exception {
        Session session = Session.newDefault(555L, 555L);
        session.setLang("ru");
        MyEventsSession.setPendingRegId(session, "reg-1");

        handler.handle(text("0"), session);

        assertTrue(sentMessages().get(0).get("text").asText().contains("положительное целое число"));
    }

    @Test
    void blankBackendUrlClearsPendingAndDelegatesToList() throws Exception {
        handler.backendUrl = "";
        Session session = Session.newDefault(555L, 555L);
        MyEventsSession.setPendingRegId(session, "reg-1");

        handler.handle(text("3"), session);

        assertNull(MyEventsSession.getPendingRegId(session));
        assertEquals(1, sentMessages().size());
        assertTrue(backendServer.requestsTo("/events/v1/bot/registrations").isEmpty());
    }

    @Test
    void successfulUpdateNotifiesGuestAndAdminClearsPendingAndRefreshesList() throws Exception {
        backendServer.onPath("/events/v1/bot/registrations/reg-1/guest-count", 200, """
            {"event_name":"Movie Night","date_start":"2026-03-01T18:00:00Z",
             "previous_guest_count":2,"guest_count":5,
             "guest_first_name":"Anna","guest_last_name":"Smith"}
            """);
        Session session = Session.newDefault(555L, 555L);
        session.setLang("ru");
        MyEventsSession.setPendingRegId(session, "reg-1");

        handler.handle(text("5"), session);

        var updateRequests = backendServer.requestsTo("/events/v1/bot/registrations/reg-1/guest-count");
        assertEquals(1, updateRequests.size());
        assertEquals("PATCH", updateRequests.get(0).method());
        JsonNode sentBody = mapper.readTree(updateRequests.get(0).body());
        assertEquals(5, sentBody.get("guest_count").asInt());

        assertNull(MyEventsSession.getPendingRegId(session));
        var sent = sentMessages();
        assertEquals(3, sent.size());
        assertEquals(555, sent.get(0).get("chat_id").asLong());
        assertTrue(sent.get(0).get("text").asText().contains("Movie Night"));
        assertEquals(ADMIN_CHAT_ID, sent.get(1).get("chat_id").asLong());
        assertEquals(555, sent.get(2).get("chat_id").asLong());
    }

    @Test
    void capacityConflictShowsMessageAndKeepsWaitingWithPendingRegId() throws Exception {
        backendServer.onPath("/events/v1/bot/registrations/reg-1/guest-count", 409, "{}");
        Session session = Session.newDefault(555L, 555L);
        session.setLang("ru");
        MyEventsSession.setPendingRegId(session, "reg-1");

        StepResult result = handler.handle(text("20"), session);

        assertEquals(MyEventsFlowDef.STEP_GUESTS_WAIT, result.nextStep());
        assertEquals("reg-1", MyEventsSession.getPendingRegId(session));
        var sent = sentMessages();
        assertEquals(1, sent.size());
        assertTrue(sent.get(0).get("text").asText().contains("Недостаточно свободных мест"));
        assertTrue(backendServer.requestsTo("/events/v1/bot/my-registrations").isEmpty());
    }

    @Test
    void serverErrorShowsGenericErrorClearsPendingAndRefreshesList() throws Exception {
        backendServer.onPath("/events/v1/bot/registrations/reg-1/guest-count", 500, "{}");
        Session session = Session.newDefault(555L, 555L);
        session.setLang("ru");
        MyEventsSession.setPendingRegId(session, "reg-1");

        handler.handle(text("3"), session);

        assertNull(MyEventsSession.getPendingRegId(session));
        var sent = sentMessages();
        assertEquals(2, sent.size());
        assertTrue(sent.get(0).get("text").asText().contains("Что-то пошло не так"));
    }
}
