package com.lan.app.flows.eventconfirm;

import com.lan.app.config.TelegramConfig;
import com.lan.app.domain.UpdateContext;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.testsupport.FakeHttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression coverage for two bugs fixed in EventConfirmHandler#notifyBackend /
 * #resolveBackendUrl: (1) the guest's Telegram chatId was never forwarded to the backend, so
 * EventRegistrationResource#confirm never had anything to persist for future reminders, and
 * (2) the direct-backend URL was missing the "/v1" segment the real resource is mounted under,
 * so the call 404'd whenever BACKEND_URL was configured.
 */
class EventConfirmHandlerTest {

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

    private UpdateContext startWithPayload(String args) {
        return new UpdateContext(777L, "private", 777L, null, "/start " + args, null, false, null, null);
    }

    @Test
    void confirmsAgainstJavaBackendWithVersionedPathAndChatId() throws InterruptedException {
        handler.backendUrl = backendServer.url();
        handler.siteUrl = "";

        Session session = Session.newDefault(777L, 777L);
        handler.handle(startWithPayload("reg_11111111-1111-1111-1111-111111111111_ru"), session);

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

        Session session = Session.newDefault(777L, 777L);
        handler.handle(startWithPayload("reg_22222222-2222-2222-2222-222222222222_en"), session);

        backendServer.awaitAtLeast(1, 2000);
        var req = backendServer.requests().get(0);
        assertEquals("/api/registration/22222222-2222-2222-2222-222222222222/confirm", req.path());
        assertEquals("chatId=777", req.query());
    }

    @Test
    void skipsBackendCallWhenNeitherUrlConfigured() throws InterruptedException {
        handler.backendUrl = "";
        handler.siteUrl = "";

        Session session = Session.newDefault(777L, 777L);
        handler.handle(startWithPayload("reg_33333333-3333-3333-3333-333333333333_ru"), session);

        Thread.sleep(200);
        assertTrue(backendServer.requests().isEmpty());
    }
}
