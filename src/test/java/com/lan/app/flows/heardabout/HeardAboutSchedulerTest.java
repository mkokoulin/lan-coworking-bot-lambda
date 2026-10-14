package com.lan.app.flows.heardabout;

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

/** Covers the sending half of the "how did you hear about us?" survey — polling /due and
 * delivering the source-choice message with its four buttons over Telegram. */
class HeardAboutSchedulerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private FakeHttpServer telegramServer;
    private FakeHttpServer backendServer;
    private BackendSessionRepository sessionRepository;
    private HeardAboutScheduler scheduler;

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
        scheduler = new HeardAboutScheduler(new TelegramClient(config), sessionRepository, new I18n());
        scheduler.backendUrl = backendServer.url();
    }

    @AfterEach
    void tearDown() {
        telegramServer.close();
        backendServer.close();
    }

    @Test
    void sendsSurveyWithFourSourceButtonsInGuestLanguage() throws Exception {
        Session enSession = Session.newDefault(201L, 201L);
        enSession.setLang("en");
        sessionRepository.save(enSession);

        String dueJson = """
            [{"chatId":201,"guestRowId":101},{"chatId":202,"guestRowId":102}]
            """;
        backendServer.onPath("/events/v1/bot/heard-about-source/due", 200, dueJson);

        scheduler.pollAndSend();

        var sent = telegramServer.requestsTo("/botTESTTOKEN/sendMessage");
        assertEquals(2, sent.size());

        JsonNode first = mapper.readTree(sent.get(0).body());
        JsonNode second = mapper.readTree(sent.get(1).body());
        JsonNode toEnGuest = first.get("chat_id").asLong() == 201 ? first : second;

        JsonNode rows = toEnGuest.get("reply_markup").get("inline_keyboard");
        assertEquals(2, rows.size());
        assertEquals("/ha_ig_101", rows.get(0).get(0).get("callback_data").asText());
        assertEquals("/ha_gg_101", rows.get(0).get(1).get("callback_data").asText());
        assertEquals("/ha_fr_101", rows.get(1).get(0).get("callback_data").asText());
        assertEquals("/ha_ot_101", rows.get(1).get(1).get("callback_data").asText());
    }

    @Test
    void noDueRecipientsSendsNothing() {
        backendServer.onPath("/events/v1/bot/heard-about-source/due", 200, "[]");

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
    void deliveryFailureForOneRecipientDoesNotStopOthers() throws Exception {
        String dueJson = """
            [{"chatId":301,"guestRowId":1},{"chatId":302,"guestRowId":2}]
            """;
        backendServer.onPath("/events/v1/bot/heard-about-source/due", 200, dueJson);
        telegramServer.onPath("/botTESTTOKEN/sendMessage", 500, "{}");

        scheduler.pollAndSend();

        // Both attempted (fire-and-forget per recipient); failures are just logged.
        assertEquals(2, telegramServer.requestsTo("/botTESTTOKEN/sendMessage").size());
    }
}
