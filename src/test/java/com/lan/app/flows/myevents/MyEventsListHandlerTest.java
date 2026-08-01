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

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MyEventsListHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private FakeHttpServer telegramServer;
    private FakeHttpServer backendServer;
    private MyEventsListHandler handler;

    @BeforeEach
    void setUp() {
        telegramServer = new FakeHttpServer();
        backendServer = new FakeHttpServer();

        TelegramConfig config = new TelegramConfig() {
            public String botToken() { return "TESTTOKEN"; }
            public String apiBaseUrl() { return telegramServer.url(); }
            public Long adminChatId() { return 999L; }
        };
        handler = new MyEventsListHandler(new TelegramClient(config), new I18n());
        handler.backendUrl = backendServer.url();
    }

    @AfterEach
    void tearDown() {
        telegramServer.close();
        backendServer.close();
    }

    private UpdateContext eventsCommand() {
        return new UpdateContext(555L, "private", 555L, null, "/events", null, false, null, null);
    }

    private JsonNode lastMessage() throws Exception {
        var sent = telegramServer.requestsTo("/botTESTTOKEN/sendMessage");
        assertEquals(1, sent.size());
        return mapper.readTree(sent.get(0).body());
    }

    @Test
    void staysInListStep() {
        backendServer.onPath("/events/v1/bot/my-registrations", 200, "[]");
        Session session = Session.newDefault(555L, 555L);

        StepResult result = handler.handle(eventsCommand(), session);

        assertEquals(MyEventsFlowDef.FLOW, result.nextFlow());
        assertEquals(MyEventsFlowDef.STEP_LIST, result.nextStep());
    }

    @Test
    void requestsMyRegistrationsWithChatIdQueryParam() {
        backendServer.onPath("/events/v1/bot/my-registrations", 200, "[]");
        Session session = Session.newDefault(555L, 555L);

        handler.handle(eventsCommand(), session);

        var requests = backendServer.requestsTo("/events/v1/bot/my-registrations");
        assertEquals(1, requests.size());
        assertEquals("chatId=555", requests.get(0).query());
    }

    @Test
    void emptyRegistrationsShowsEmptyMessageWithOnlyStartButton() throws Exception {
        backendServer.onPath("/events/v1/bot/my-registrations", 200, "[]");
        Session session = Session.newDefault(555L, 555L);
        session.setLang("ru");

        handler.handle(eventsCommand(), session);

        JsonNode body = lastMessage();
        assertTrue(body.get("text").asText().contains("У вас пока нет регистраций"));
        JsonNode rows = body.get("reply_markup").get("inline_keyboard");
        assertEquals(1, rows.size());
    }

    @Test
    void separatesUpcomingFromPastAndCancelledIntoHistory() throws Exception {
        Instant future = Instant.now().plusSeconds(7 * 24 * 3600);
        Instant past = Instant.now().minusSeconds(7 * 24 * 3600);

        String json = mapper.writeValueAsString(List.of(
                registration("upcoming-active", "Upcoming Party", future, 2, false),
                registration("already-happened", "Old Party", past, 3, false),
                registration("upcoming-but-cancelled", "Cancelled Party", future, 1, true)
        ));
        backendServer.onPath("/events/v1/bot/my-registrations", 200, json);
        Session session = Session.newDefault(555L, 555L);
        session.setLang("ru");

        handler.handle(eventsCommand(), session);

        JsonNode body = lastMessage();
        String text = body.get("text").asText();
        assertTrue(text.contains("Upcoming Party"), text);
        assertTrue(text.contains("Old Party"), text);
        assertTrue(text.contains("Cancelled Party"), text);

        // Only the genuinely upcoming, non-cancelled registration gets action buttons.
        JsonNode rows = body.get("reply_markup").get("inline_keyboard");
        assertEquals(2, rows.size()); // one active row + the trailing "start" row
        JsonNode activeRow = rows.get(0);
        assertEquals("/me_c_upcoming-active", activeRow.get(0).get("callback_data").asText());
        assertEquals("/me_g_upcoming-active", activeRow.get(1).get("callback_data").asText());
    }

    @Test
    void historyIsLimitedToFifteenMostRecent() throws Exception {
        var items = new java.util.ArrayList<Map<String, Object>>();
        for (int i = 0; i < 20; i++) {
            Instant date = Instant.now().minusSeconds((i + 1) * 3600L);
            items.add(rawRegistration("reg-" + i, "Ev" + i, date, 1, false));
        }
        backendServer.onPath("/events/v1/bot/my-registrations", 200, mapper.writeValueAsString(items));
        Session session = Session.newDefault(555L, 555L);
        session.setLang("ru");

        handler.handle(eventsCommand(), session);

        String text = lastMessage().get("text").asText();
        for (int i = 0; i < 15; i++) {
            assertTrue(text.contains("Ev" + i), "expected Ev" + i + " in:\n" + text);
        }
        for (int i = 15; i < 20; i++) {
            assertFalse(text.contains("Ev" + i), "did not expect Ev" + i + " in:\n" + text);
        }
    }

    @Test
    void non200BackendResponseIsTreatedAsNoRegistrations() throws Exception {
        backendServer.onPath("/events/v1/bot/my-registrations", 500, "{}");
        Session session = Session.newDefault(555L, 555L);
        session.setLang("ru");

        handler.handle(eventsCommand(), session);

        assertTrue(lastMessage().get("text").asText().contains("У вас пока нет регистраций"));
    }

    @Test
    void blankBackendUrlSkipsHttpCallEntirely() throws Exception {
        handler.backendUrl = "";
        Session session = Session.newDefault(555L, 555L);
        session.setLang("ru");

        handler.handle(eventsCommand(), session);

        assertTrue(backendServer.requests().isEmpty());
        assertTrue(lastMessage().get("text").asText().contains("У вас пока нет регистраций"));
    }

    private Map<String, Object> registration(String id, String eventName, Instant dateStart, int guestCount, boolean cancelled) {
        return rawRegistration(id, eventName, dateStart, guestCount, cancelled);
    }

    private Map<String, Object> rawRegistration(String id, String eventName, Instant dateStart, int guestCount, boolean cancelled) {
        return Map.of(
                "registration_id", id,
                "event_name", eventName,
                "date_start", dateStart.toString(),
                "guest_count", guestCount,
                "is_cancelled", cancelled
        );
    }
}
