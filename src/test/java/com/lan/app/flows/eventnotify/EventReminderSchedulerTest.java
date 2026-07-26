package com.lan.app.flows.eventnotify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.backend.BackendClient;
import com.lan.app.config.TelegramConfig;
import com.lan.app.i18n.I18n;
import com.lan.app.session.BackendSessionRepository;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.testsupport.FakeHttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers the sending half of the reminder feature — the piece that was entirely missing before:
 * nothing polled the backend's due-notifications feed or delivered anything over Telegram.
 */
class EventReminderSchedulerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private FakeHttpServer telegramServer;
    private FakeHttpServer backendServer;
    private BackendSessionRepository sessionRepository;
    private EventReminderScheduler scheduler;

    @BeforeEach
    void setUp() {
        telegramServer = new FakeHttpServer();
        backendServer = new FakeHttpServer();
        sessionRepository = new BackendSessionRepository(new BackendClient());

        TelegramConfig config = new TelegramConfig() {
            public String botToken() { return "TESTTOKEN"; }
            public String apiBaseUrl() { return telegramServer.url(); }
            public Long adminChatId() { return 999L; }
        };
        scheduler = new EventReminderScheduler(new TelegramClient(config), sessionRepository, new I18n());
        scheduler.backendUrl = backendServer.url();
    }

    @AfterEach
    void tearDown() {
        telegramServer.close();
        backendServer.close();
    }

    @Test
    void sendsReminderInGuestLanguageWithButtonsAndReportsSentResults() throws Exception {
        // Guest 201 has an existing bot session in English; guest 202 has never talked to the
        // bot, so the scheduler must fall back to Russian (the app-wide default).
        Session enSession = Session.newDefault(201L, 201L);
        enSession.setLang("en");
        sessionRepository.save(enSession);

        String dueJson = """
            [{"id":10,"messageEn":"Reminder EN","messageRu":"Напоминание RU","eventName":"Movie Night",
              "recipients":[
                {"chatId":201,"guestRowId":1,"registrationRowId":11},
                {"chatId":202,"guestRowId":2,"registrationRowId":12}
              ]}]
            """;
        backendServer.onPath("/events/v1/bot/event-notifications/due", 200, dueJson);

        scheduler.pollAndSend();

        var sent = telegramServer.requestsTo("/botTESTTOKEN/sendMessage");
        assertEquals(2, sent.size());

        JsonNode first = mapper.readTree(sent.get(0).body());
        JsonNode second = mapper.readTree(sent.get(1).body());
        JsonNode toEnGuest = first.get("chat_id").asLong() == 201 ? first : second;
        JsonNode toRuGuest = first.get("chat_id").asLong() == 202 ? first : second;

        assertEquals("Reminder EN", toEnGuest.get("text").asText());
        assertEquals("Напоминание RU", toRuGuest.get("text").asText());

        JsonNode enButtons = toEnGuest.get("reply_markup").get("inline_keyboard").get(0);
        assertEquals("/en_y_10_1_11", enButtons.get(0).get("callback_data").asText());
        assertEquals("/en_n_10_1_11", enButtons.get(1).get("callback_data").asText());

        var results = backendServer.requestsTo("/events/v1/bot/event-notifications/10/results");
        assertEquals(1, results.size());
        JsonNode resultBody = mapper.readTree(results.get(0).body());
        assertEquals(2, resultBody.size());
        for (JsonNode r : resultBody) {
            assertEquals("SENT", r.get("status").asText());
        }
    }

    @Test
    void marksRecipientAsFailedWhenTelegramDeliveryFails() throws Exception {
        String dueJson = """
            [{"id":20,"messageEn":"E","messageRu":"R","eventName":"Ev",
              "recipients":[{"chatId":301,"guestRowId":5,"registrationRowId":15}]}]
            """;
        backendServer.onPath("/events/v1/bot/event-notifications/due", 200, dueJson);
        telegramServer.onPath("/botTESTTOKEN/sendMessage", 500, "{}");

        scheduler.pollAndSend();

        var results = backendServer.requestsTo("/events/v1/bot/event-notifications/20/results");
        assertEquals(1, results.size());
        JsonNode resultBody = mapper.readTree(results.get(0).body()).get(0);
        assertEquals("FAILED", resultBody.get("status").asText());
        assertNotNull(resultBody.get("failureReason"));
    }

    @Test
    void noDueNotificationsSendsNothing() {
        backendServer.onPath("/events/v1/bot/event-notifications/due", 200, "[]");

        scheduler.pollAndSend();

        assertTrue(telegramServer.requests().isEmpty());
        assertTrue(backendServer.requestsTo("/events/v1/bot/event-notifications").stream()
                .noneMatch(r -> r.path().endsWith("/results")));
    }

    @Test
    void blankBackendUrlSkipsPollEntirely() {
        scheduler.backendUrl = "";

        scheduler.pollAndSend();

        assertTrue(telegramServer.requests().isEmpty());
        assertTrue(backendServer.requests().isEmpty());
    }
}
