package com.lan.app.flows.weeklydigest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.backend.BackendClient;
import com.lan.app.client.baserow.api.EventResourceApi;
import com.lan.app.client.baserow.api.FestivalsApi;
import com.lan.app.client.baserow.model.EventResponse;
import com.lan.app.config.TelegramConfig;
import com.lan.app.i18n.I18n;
import com.lan.app.session.BackendSessionRepository;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.testsupport.FakeHttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WeeklyDigestSchedulerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private FakeHttpServer telegramServer;
    private FakeHttpServer backendServer;
    private BackendSessionRepository sessionRepository;
    private EventResourceApi eventApi;
    private FestivalsApi festivalsApi;
    private WeeklyDigestScheduler scheduler;

    @BeforeEach
    void setUp() {
        telegramServer = new FakeHttpServer();
        backendServer = new FakeHttpServer();
        sessionRepository = new BackendSessionRepository(new BackendClient());
        eventApi = mock(EventResourceApi.class);
        festivalsApi = mock(FestivalsApi.class);
        when(festivalsApi.listFestivals()).thenReturn(List.of());

        TelegramConfig config = new TelegramConfig() {
            public String botToken() { return "TESTTOKEN"; }
            public String apiBaseUrl() { return telegramServer.url(); }
            public Long adminChatId() { return 999L; }
        };
        scheduler = new WeeklyDigestScheduler(new TelegramClient(config), sessionRepository, new I18n(), eventApi, festivalsApi);
        scheduler.backendUrl = backendServer.url();
    }

    @AfterEach
    void tearDown() {
        telegramServer.close();
        backendServer.close();
    }

    private static EventResponse event(String name, OffsetDateTime dateStart) {
        EventResponse e = new EventResponse();
        e.setId(UUID.randomUUID());
        e.setName(name);
        e.setDateStart(dateStart);
        return e;
    }

    @Test
    void sendsDigestInGuestLanguageWithUnsubscribeButton() throws Exception {
        when(eventApi.eventsV1Get()).thenReturn(List.of(
            event("Movie Night", OffsetDateTime.now().plusDays(2))
        ));
        backendServer.onPath("/events/v1/bot/weekly-digest/subscribers", 200, """
            [{"chatId":201,"guestRowId":11},{"chatId":202,"guestRowId":12}]
            """);

        Session enSession = Session.newDefault(201L, 201L);
        enSession.setLang("en");
        sessionRepository.save(enSession);
        // guest 202 has never talked to the bot -> falls back to Russian

        scheduler.sendDigest();

        var sent = telegramServer.requestsTo("/botTESTTOKEN/sendMessage");
        assertEquals(2, sent.size());

        JsonNode first = mapper.readTree(sent.get(0).body());
        JsonNode second = mapper.readTree(sent.get(1).body());
        JsonNode toEnGuest = first.get("chat_id").asLong() == 201 ? first : second;
        JsonNode toRuGuest = first.get("chat_id").asLong() == 202 ? first : second;

        assertTrue(toEnGuest.get("text").asText().contains("Movie Night"));
        assertTrue(toEnGuest.get("text").asText().contains("This week's events"));
        assertTrue(toRuGuest.get("text").asText().contains("Movie Night"));
        assertTrue(toRuGuest.get("text").asText().contains("События этой недели"));

        JsonNode enButton = toEnGuest.get("reply_markup").get("inline_keyboard").get(0).get(0);
        assertEquals("digest_unsub_11", enButton.get("callback_data").asText());
        JsonNode ruButton = toRuGuest.get("reply_markup").get("inline_keyboard").get(0).get(0);
        assertEquals("digest_unsub_12", ruButton.get("callback_data").asText());
    }

    @Test
    void eventOutsideWeekWindowIsExcluded() throws Exception {
        when(eventApi.eventsV1Get()).thenReturn(List.of(
            event("Next month", OffsetDateTime.now().plusDays(20))
        ));

        scheduler.sendDigest();

        assertTrue(telegramServer.requests().isEmpty());
        assertTrue(backendServer.requests().isEmpty());
    }

    @Test
    void pastEventIsExcluded() throws Exception {
        when(eventApi.eventsV1Get()).thenReturn(List.of(
            event("Yesterday", OffsetDateTime.now().minusDays(1))
        ));

        scheduler.sendDigest();

        assertTrue(telegramServer.requests().isEmpty());
        assertTrue(backendServer.requests().isEmpty());
    }

    @Test
    void noEventsThisWeek_skipsSendEntirelyWithoutCallingSubscribersEndpoint() {
        when(eventApi.eventsV1Get()).thenReturn(List.of());

        scheduler.sendDigest();

        assertTrue(telegramServer.requests().isEmpty());
        assertTrue(backendServer.requests().isEmpty());
    }

    @Test
    void eventsExistButNoSubscribers_sendsNothing() {
        when(eventApi.eventsV1Get()).thenReturn(List.of(
            event("Movie Night", OffsetDateTime.now().plusDays(2))
        ));
        backendServer.onPath("/events/v1/bot/weekly-digest/subscribers", 200, "[]");

        scheduler.sendDigest();

        assertTrue(telegramServer.requests().isEmpty());
    }

    @Test
    void blankBackendUrlSkipsEntirely() {
        scheduler.backendUrl = "";

        scheduler.sendDigest();

        assertTrue(telegramServer.requests().isEmpty());
        assertTrue(backendServer.requests().isEmpty());
    }
}
