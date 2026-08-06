package com.lan.app.flows.eventconfirm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.config.TelegramConfig;
import com.lan.app.domain.IncomingUpdate;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.testsupport.FakeHttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventConfirmHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private FakeHttpServer telegramServer;
    private FakeHttpServer backendServer;
    private EventConfirmHandler handler;

    @BeforeEach
    void setUp() {
        telegramServer = new FakeHttpServer();
        backendServer = new FakeHttpServer();

        TelegramConfig config = new TelegramConfig() {
            public String botToken() { return "TESTTOKEN"; }
            public String apiBaseUrl() { return telegramServer.url(); }
            public Long adminChatId() { return 999L; }
        };
        handler = new EventConfirmHandler(new TelegramClient(config), new I18n());
    }

    @AfterEach
    void tearDown() {
        telegramServer.close();
        backendServer.close();
    }

    private static Session session() {
        return Session.newDefault(777L, 777L);
    }

    private static UpdateContext startWithPayload(String args) {
        IncomingUpdate u = new IncomingUpdate();
        u.setChatId(777L);
        u.setUserId(777L);
        u.setText(args.isEmpty() ? "/start" : "/start " + args);
        return UpdateContext.fromIncomingUpdate(u);
    }

    /** handle() always sends the confirm text first, then the "next" message with the keyboard. */
    private JsonNode confirmMessage() throws Exception {
        var sent = telegramServer.requestsTo("/botTESTTOKEN/sendMessage");
        return mapper.readTree(sent.get(0).body());
    }

    private JsonNode nextMessage() throws Exception {
        var sent = telegramServer.requestsTo("/botTESTTOKEN/sendMessage");
        return mapper.readTree(sent.get(1).body());
    }

    @Test
    void confirmsAgainstJavaBackendWithVersionedPathAndChatId() throws InterruptedException {
        handler.backendUrl = backendServer.url();
        handler.siteUrl = "";

        handler.handle(startWithPayload("reg_11111111-1111-1111-1111-111111111111_ru"), session());

        backendServer.awaitAtLeast(1, 2000);
        var requests = backendServer.requests();
        assertEquals(1, requests.size());
        var req = requests.get(0);
        assertEquals("POST", req.method());
        assertEquals("/events/v1/registrations/11111111-1111-1111-1111-111111111111/confirm", req.path());
        assertEquals("chatId=777", req.query());
    }

    @Test
    void fallsBackToSiteUrlWhenBackendUrlNotConfigured() throws InterruptedException {
        handler.backendUrl = "";
        handler.siteUrl = backendServer.url(); // reuse the fake server as "the site" for this case

        handler.handle(startWithPayload("reg_22222222-2222-2222-2222-222222222222_en"), session());

        backendServer.awaitAtLeast(1, 2000);
        var req = backendServer.requests().get(0);
        assertEquals("/api/registration/22222222-2222-2222-2222-222222222222/confirm", req.path());
        assertEquals("chatId=777", req.query());
    }

    @Test
    void skipsBackendCallWhenNeitherUrlConfigured() throws InterruptedException {
        handler.backendUrl = "";
        handler.siteUrl = "";

        handler.handle(startWithPayload("reg_33333333-3333-3333-3333-333333333333_ru"), session());

        Thread.sleep(200);
        assertTrue(backendServer.requests().isEmpty());
    }

    @Test
    void regIdWithoutLangSuffix_doesNotChangeSessionLang() throws InterruptedException {
        handler.backendUrl = "";
        handler.siteUrl = "";
        Session s = session();

        handler.handle(startWithPayload("reg_abc123"), s);

        assertEquals("ru", s.getLang());
    }

    @Test
    void regIdWithLangSuffix_changesSessionLangBeforeSendingGenericMessage() throws Exception {
        handler.backendUrl = "";
        handler.siteUrl = "";
        Session s = session();

        handler.handle(startWithPayload("reg_abc123_en"), s);

        assertEquals("en", s.getLang());
        String text = confirmMessage().get("text").asText();
        assertTrue(text.contains("Registration confirmed"), text);
    }

    @Test
    void backendConfirmsWithEventName_sendsNamedMessageAndAllButtons() throws Exception {
        handler.backendUrl = backendServer.url();
        handler.siteUrl = "https://example.test";
        backendServer.onPath("/events/v1/registrations", 200, "{\"event_name\":\"Party Night\"}");

        StepResult result = handler.handle(startWithPayload("reg_abc123_en"), session());

        assertEquals(StepResult.finish(), result);
        String text = confirmMessage().get("text").asText();
        assertTrue(text.contains("Party Night"), text);

        JsonNode rows = nextMessage().get("reply_markup").get("inline_keyboard");
        // start button + site button (siteUrl configured) + change button
        assertEquals(3, rows.size());
    }

    @Test
    void backendReturnsMalformedJson_treatedAsNoEventName() throws Exception {
        handler.backendUrl = backendServer.url();
        handler.siteUrl = "";
        backendServer.onPath("/events/v1/registrations", 200, "not json");

        handler.handle(startWithPayload("reg_ghi789_en"), session());

        String text = confirmMessage().get("text").asText();
        assertTrue(text.contains("Registration confirmed"), text);
        assertFalse(text.contains("for \""), text);
    }

    @Test
    void noStartArgs_sendsGenericConfirmMessageOnlyStartButton() throws Exception {
        handler.backendUrl = "";
        handler.siteUrl = "";

        StepResult result = handler.handle(startWithPayload(""), session());

        assertEquals(StepResult.finish(), result);
        assertTrue(confirmMessage().get("text").asText().contains("Регистрация подтверждена"));
        assertEquals(1, nextMessage().get("reply_markup").get("inline_keyboard").size());
    }

    @Test
    void nonRegArgs_treatedAsNoRegId() throws Exception {
        handler.backendUrl = "";
        handler.siteUrl = "";

        handler.handle(startWithPayload("some_other_payload"), session());

        String text = confirmMessage().get("text").asText();
        assertTrue(text.contains("Регистрация подтверждена"), text);
    }
}
