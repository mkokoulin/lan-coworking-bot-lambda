package com.lan.app.flows.eventsurvey;

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

class EventSurveySchedulerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private FakeHttpServer telegramServer;
    private FakeHttpServer backendServer;
    private BackendSessionRepository sessionRepository;
    private EventSurveyScheduler scheduler;

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
        scheduler = new EventSurveyScheduler(new TelegramClient(config), sessionRepository, new I18n());
        scheduler.backendUrl = backendServer.url();
    }

    @AfterEach
    void tearDown() {
        telegramServer.close();
        backendServer.close();
    }

    @Test
    void sendsRatingPromptWithCallbackDataEncodingIds() throws Exception {
        Session enSession = Session.newDefault(201L, 201L);
        enSession.setLang("en");
        sessionRepository.save(enSession);
        // guest 202 has never talked to the bot -> falls back to Russian

        backendServer.onPath("/events/v1/bot/event-surveys/due", 200, """
            [{"chatId":201,"eventRowId":42,"eventName":"Movie Night","guestRowId":11,"registrationRowId":100},
             {"chatId":202,"eventRowId":42,"eventName":"Movie Night","guestRowId":12,"registrationRowId":101}]
            """);

        scheduler.pollAndSend();

        var sent = telegramServer.requestsTo("/botTESTTOKEN/sendMessage");
        assertEquals(2, sent.size());

        JsonNode first = mapper.readTree(sent.get(0).body());
        JsonNode second = mapper.readTree(sent.get(1).body());
        JsonNode toEnGuest = first.get("chat_id").asLong() == 201 ? first : second;
        JsonNode toRuGuest = first.get("chat_id").asLong() == 202 ? first : second;

        assertTrue(toEnGuest.get("text").asText().contains("Movie Night"));

        JsonNode enButtons = toEnGuest.get("reply_markup").get("inline_keyboard").get(0);
        assertEquals(5, enButtons.size());
        assertEquals("survey_rate_1_42_11_100", enButtons.get(0).get("callback_data").asText());
        assertEquals("survey_rate_5_42_11_100", enButtons.get(4).get("callback_data").asText());

        JsonNode ruButtons = toRuGuest.get("reply_markup").get("inline_keyboard").get(0);
        assertEquals("survey_rate_3_42_12_101", ruButtons.get(2).get("callback_data").asText());
    }

    @Test
    void noDueSurveysSendsNothing() {
        backendServer.onPath("/events/v1/bot/event-surveys/due", 200, "[]");

        scheduler.pollAndSend();

        assertTrue(telegramServer.requests().isEmpty());
    }

    @Test
    void blankBackendUrlSkipsPollEntirely() {
        scheduler.backendUrl = "";

        scheduler.pollAndSend();

        assertTrue(telegramServer.requests().isEmpty());
        assertTrue(backendServer.requests().isEmpty());
    }

    @Test
    void backendErrorIsLoggedAndSwallowed() {
        backendServer.onPath("/events/v1/bot/event-surveys/due", 500, "{}");

        scheduler.pollAndSend();

        assertTrue(telegramServer.requests().isEmpty());
    }
}
