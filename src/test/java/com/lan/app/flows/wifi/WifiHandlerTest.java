package com.lan.app.flows.wifi;

import com.lan.app.config.TelegramConfig;
import com.lan.app.domain.IncomingUpdate;
import com.lan.app.domain.UpdateContext;
import com.lan.app.flows.registration.RegistrationSession;
import com.lan.app.i18n.I18n;
import com.lan.app.service.GuestService;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.testsupport.FakeHttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WifiHandlerTest {

    private FakeHttpServer telegramServer;
    private WifiHandler handler;

    @BeforeEach
    void setUp() {
        telegramServer = new FakeHttpServer();

        TelegramConfig config = new TelegramConfig() {
            public String botToken() { return "TESTTOKEN"; }
            public String apiBaseUrl() { return telegramServer.url(); }
            public Long adminChatId() { return 999L; }
        };
        handler = new WifiHandler(new TelegramClient(config), new I18n(), new GuestService());
        handler.guestSsid = "";
        handler.guestPassword = "";
        handler.privateSsid = "";
        handler.privatePassword = "";
    }

    @AfterEach
    void tearDown() {
        telegramServer.close();
    }

    private UpdateContext wifiCommand() {
        IncomingUpdate u = new IncomingUpdate();
        u.setChatId(555L);
        u.setUserId(555L);
        u.setText("/wifi");
        return UpdateContext.fromIncomingUpdate(u);
    }

    private static Session authenticatedSession() {
        Session session = Session.newDefault(555L, 555L);
        session.setLang("en");
        RegistrationSession.markRegistered(session);
        return session;
    }

    private static Session unauthenticatedSession() {
        Session session = Session.newDefault(555L, 555L);
        session.setLang("en");
        // short-circuits isAuthenticated() to false without hitting GuestService
        RegistrationSession.setManualLogout(session);
        return session;
    }

    @Test
    void authenticatedGuest_sendsOneQrPhotoPerConfiguredNetworkThenFallbackText() {
        handler.guestSsid = "LAN Guest";
        handler.guestPassword = "guestpass";
        handler.privateSsid = "LAN Residents";
        handler.privatePassword = "s3cr3t!";

        handler.handle(wifiCommand(), authenticatedSession());

        var photoReqs = telegramServer.requestsTo("/botTESTTOKEN/sendPhoto");
        assertEquals(2, photoReqs.size(), "should send one QR per configured network");
        assertTrue(photoReqs.get(0).body().contains("LAN Guest"));
        assertTrue(photoReqs.get(1).body().contains("LAN Residents"));

        var textReqs = telegramServer.requestsTo("/botTESTTOKEN/sendMessage");
        assertEquals(1, textReqs.size());
        String text = textReqs.get(0).body();
        assertTrue(text.contains("LAN Guest") && text.contains("guestpass"), text);
        assertTrue(text.contains("LAN Residents") && text.contains("s3cr3t!"), text);
    }

    @Test
    void unauthenticatedGuest_onlySeesGuestNetworkEvenWhenPrivateIsConfigured() {
        handler.guestSsid = "LAN Guest";
        handler.guestPassword = "guestpass";
        handler.privateSsid = "LAN Residents";
        handler.privatePassword = "s3cr3t!";

        handler.handle(wifiCommand(), unauthenticatedSession());

        var photoReqs = telegramServer.requestsTo("/botTESTTOKEN/sendPhoto");
        assertEquals(1, photoReqs.size());
        assertTrue(photoReqs.get(0).body().contains("LAN Guest"));

        var textReqs = telegramServer.requestsTo("/botTESTTOKEN/sendMessage");
        assertFalse(textReqs.get(0).body().contains("LAN Residents"));
        assertFalse(textReqs.get(0).body().contains("s3cr3t!"));
    }

    @Test
    void sendsOnlyTheConfiguredNetworkWhenOtherIsMissing() {
        handler.guestSsid = "LAN Guest";
        handler.guestPassword = "guestpass";

        handler.handle(wifiCommand(), authenticatedSession());

        var photoReqs = telegramServer.requestsTo("/botTESTTOKEN/sendPhoto");
        assertEquals(1, photoReqs.size());
        assertTrue(photoReqs.get(0).body().contains("LAN Guest"));

        var textReqs = telegramServer.requestsTo("/botTESTTOKEN/sendMessage");
        assertFalse(textReqs.get(0).body().contains("Residents"));
    }

    @Test
    void escapesHtmlInSsidAndPassword() {
        handler.guestSsid = "<b>LAN</b>";
        handler.guestPassword = "a&b";

        handler.handle(wifiCommand(), authenticatedSession());

        var photoReqs = telegramServer.requestsTo("/botTESTTOKEN/sendPhoto");
        assertTrue(photoReqs.get(0).body().contains("&lt;b&gt;LAN&lt;/b&gt;"));

        var textReqs = telegramServer.requestsTo("/botTESTTOKEN/sendMessage");
        assertTrue(textReqs.get(0).body().contains("a&amp;b"));
    }

    @Test
    void showsNotConfiguredMessageWithoutSendingPhotoWhenNoNetworksSet() {
        Session session = authenticatedSession();
        session.setLang("ru");
        handler.handle(wifiCommand(), session);

        assertTrue(telegramServer.requestsTo("/botTESTTOKEN/sendPhoto").isEmpty());

        var textReqs = telegramServer.requestsTo("/botTESTTOKEN/sendMessage");
        assertEquals(1, textReqs.size());
        assertTrue(textReqs.get(0).body().contains("не настроены"));
    }
}
