package com.lan.app.flows.news;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.config.TelegramConfig;
import com.lan.app.domain.UpdateContext;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.testsupport.FakeHttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression coverage for the bug that shipped to production: the bot's news client called the
 * old /coworking/v1/news path (renamed to /coworking/v1/blog on the backend), so every "News" tap
 * 404'd. These tests pin the request shape (path, auth header) and the RU/EN rendering so a future
 * path/DTO drift fails a test instead of a guest.
 */
class NewsHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private FakeHttpServer telegramServer;
    private FakeHttpServer baserowServer;
    private NewsHandler handler;

    @BeforeEach
    void setUp() {
        telegramServer = new FakeHttpServer();
        baserowServer = new FakeHttpServer();

        TelegramConfig config = new TelegramConfig() {
            public String botToken() { return "TESTTOKEN"; }
            public String apiBaseUrl() { return telegramServer.url(); }
            public Long adminChatId() { return 999L; }
        };
        handler = new NewsHandler(new TelegramClient(config), new I18n());
        handler.baserowUrl = baserowServer.url();
        handler.baserowToken = "secret-token";
    }

    @AfterEach
    void tearDown() {
        telegramServer.close();
        baserowServer.close();
    }

    private UpdateContext newsCommand() {
        return new UpdateContext(555L, "private", 555L, null, "/news", null, false, null, null);
    }

    @Test
    void requestsCorrectPathWithBearerAuth() {
        baserowServer.onPath("/coworking/v1/blog", 200, "[]");

        Session session = Session.newDefault(555L, 555L);
        handler.handle(newsCommand(), session);

        var requests = baserowServer.requestsTo("/coworking/v1/blog");
        assertEquals(1, requests.size());
        assertEquals("GET", requests.get(0).method());
    }

    @Test
    void rendersRussianTitleAndBodyWithHomeButton() throws Exception {
        baserowServer.onPath("/coworking/v1/blog", 200, """
            [{"titleEn":"Movie night","titleRu":"Кино вечер","bodyEn":"English body","bodyRu":"Русский текст","link":"https://t.me/lan_yerevan"}]
            """);

        Session session = Session.newDefault(555L, 555L);
        session.setLang("ru");
        handler.handle(newsCommand(), session);

        var sent = telegramServer.requestsTo("/botTESTTOKEN/sendMessage");
        assertEquals(1, sent.size());
        JsonNode body = mapper.readTree(sent.get(0).body());
        String text = body.get("text").asText();
        assertTrue(text.contains("Кино вечер"), text);
        assertTrue(text.contains("Русский текст"), text);
        assertFalse(text.contains("English body"), text);

        JsonNode homeBtn = body.get("reply_markup").get("inline_keyboard").get(0).get(0);
        assertEquals("/start", homeBtn.get("callback_data").asText());
    }

    @Test
    void rendersEnglishTitleAndBodyForEnglishSession() throws Exception {
        baserowServer.onPath("/coworking/v1/blog", 200, """
            [{"titleEn":"Movie night","titleRu":"Кино вечер","bodyEn":"English body","bodyRu":"Русский текст"}]
            """);

        Session session = Session.newDefault(555L, 555L);
        session.setLang("en");
        handler.handle(newsCommand(), session);

        JsonNode body = mapper.readTree(telegramServer.requestsTo("/botTESTTOKEN/sendMessage").get(0).body());
        String text = body.get("text").asText();
        assertTrue(text.contains("Movie night"), text);
        assertTrue(text.contains("English body"), text);
        assertFalse(text.contains("Кино вечер"), text);
    }

    @Test
    void escapesHtmlInTitleAndBody() throws Exception {
        baserowServer.onPath("/coworking/v1/blog", 200, """
            [{"titleEn":"<b>Bold</b> & fun","titleRu":"т","bodyEn":"a < b & c > d","bodyRu":"т"}]
            """);

        Session session = Session.newDefault(555L, 555L);
        session.setLang("en");
        handler.handle(newsCommand(), session);

        JsonNode body = mapper.readTree(telegramServer.requestsTo("/botTESTTOKEN/sendMessage").get(0).body());
        String text = body.get("text").asText();
        assertTrue(text.contains("&lt;b&gt;Bold&lt;/b&gt; &amp; fun"), text);
        assertTrue(text.contains("a &lt; b &amp; c &gt; d"), text);
    }

    @Test
    void emptyListShowsEmptyMessage() throws Exception {
        baserowServer.onPath("/coworking/v1/blog", 200, "[]");

        Session session = Session.newDefault(555L, 555L);
        session.setLang("ru");
        handler.handle(newsCommand(), session);

        JsonNode body = mapper.readTree(telegramServer.requestsTo("/botTESTTOKEN/sendMessage").get(0).body());
        assertTrue(body.get("text").asText().contains("Новостей пока нет"));
    }

    @Test
    void non200ResponseShowsErrorMessageInsteadOfCrashing() throws Exception {
        baserowServer.onPath("/coworking/v1/blog", 404, """
            {"code":"ROUTE_NOT_FOUND","message":"Requested resource was not found.","details":{}}
            """);

        Session session = Session.newDefault(555L, 555L);
        session.setLang("ru");
        handler.handle(newsCommand(), session);

        JsonNode body = mapper.readTree(telegramServer.requestsTo("/botTESTTOKEN/sendMessage").get(0).body());
        assertTrue(body.get("text").asText().contains("Не удалось загрузить новости"));
    }

    @Test
    void malformedJsonShowsErrorMessageInsteadOfCrashing() throws Exception {
        baserowServer.onPath("/coworking/v1/blog", 200, "");

        Session session = Session.newDefault(555L, 555L);
        session.setLang("ru");
        handler.handle(newsCommand(), session);

        JsonNode body = mapper.readTree(telegramServer.requestsTo("/botTESTTOKEN/sendMessage").get(0).body());
        assertTrue(body.get("text").asText().contains("Не удалось загрузить новости"));
    }

    @Test
    void blankBaserowUrlShowsErrorWithoutMakingHttpCall() throws Exception {
        handler.baserowUrl = "";

        Session session = Session.newDefault(555L, 555L);
        session.setLang("ru");
        handler.handle(newsCommand(), session);

        assertTrue(baserowServer.requests().isEmpty());
        JsonNode body = mapper.readTree(telegramServer.requestsTo("/botTESTTOKEN/sendMessage").get(0).body());
        assertTrue(body.get("text").asText().contains("Не удалось загрузить новости"));
    }

    @Test
    void truncatesLongBodyTo300CharsWithEllipsis() throws Exception {
        String longBody = "x".repeat(400);
        baserowServer.onPath("/coworking/v1/blog", 200, mapper.writeValueAsString(java.util.List.of(
                java.util.Map.of("titleEn", "T", "titleRu", "T", "bodyEn", longBody, "bodyRu", longBody)
        )));

        Session session = Session.newDefault(555L, 555L);
        session.setLang("en");
        handler.handle(newsCommand(), session);

        JsonNode body = mapper.readTree(telegramServer.requestsTo("/botTESTTOKEN/sendMessage").get(0).body());
        String text = body.get("text").asText();
        assertTrue(text.contains("x".repeat(300) + "…"), text);
        assertFalse(text.contains("x".repeat(301)), text);
    }

    @Test
    void limitsToTenMostRecentItems() throws Exception {
        var items = new java.util.ArrayList<java.util.Map<String, String>>();
        for (int i = 0; i < 12; i++) {
            items.add(java.util.Map.of("titleEn", "Item" + i, "titleRu", "Item" + i, "bodyEn", "", "bodyRu", ""));
        }
        baserowServer.onPath("/coworking/v1/blog", 200, mapper.writeValueAsString(items));

        Session session = Session.newDefault(555L, 555L);
        session.setLang("en");
        handler.handle(newsCommand(), session);

        JsonNode body = mapper.readTree(telegramServer.requestsTo("/botTESTTOKEN/sendMessage").get(0).body());
        String text = body.get("text").asText();
        for (int i = 0; i < 10; i++) {
            assertTrue(text.contains("Item" + i), text);
        }
        assertFalse(text.contains("Item10"), text);
        assertFalse(text.contains("Item11"), text);
    }
}
