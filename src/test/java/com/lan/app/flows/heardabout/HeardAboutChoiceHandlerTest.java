package com.lan.app.flows.heardabout;

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

import static org.junit.jupiter.api.Assertions.*;

/** Covers the guest-facing half of the survey: tapping an Instagram/Google/Friends/Other button. */
class HeardAboutChoiceHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private FakeHttpServer telegramServer;
    private HeardAboutChoiceHandler handler;

    @BeforeEach
    void setUp() {
        telegramServer = new FakeHttpServer();

        TelegramConfig config = new TelegramConfig() {
            public String botToken() { return "TESTTOKEN"; }
            public String apiBaseUrl() { return telegramServer.url(); }
            public Long adminChatId() { return 999L; }
        };
        handler = new HeardAboutChoiceHandler(new TelegramClient(config), new I18n());
    }

    @AfterEach
    void tearDown() {
        telegramServer.close();
    }

    private UpdateContext callback(String data) {
        return new UpdateContext(555L, "private", 555L, null, null, "/" + data, null, true, null, null, null, null);
    }

    @Test
    void instagramTap_storesSourceAndAsksForComment() throws Exception {
        Session session = Session.newDefault(555L, 555L);

        StepResult result = handler.handle(callback("ha_ig_101"), session);

        assertEquals(StepResult.stay(HeardAboutFlowDef.FLOW, HeardAboutFlowDef.STEP_COMMENT), result);
        assertEquals("Instagram", HeardAboutSession.getSource(session));
        assertEquals("101", HeardAboutSession.getGuestRowId(session));

        var sent = telegramServer.requestsTo("/botTESTTOKEN/sendMessage");
        assertEquals(1, sent.size());
        JsonNode body = mapper.readTree(sent.get(0).body());
        assertEquals(555, body.get("chat_id").asLong());
        assertFalse(body.get("text").asText().isBlank());
        assertTrue(body.has("reply_markup"));
    }

    @Test
    void googleTap_storesSourceGoogle() {
        Session session = Session.newDefault(555L, 555L);

        handler.handle(callback("ha_gg_202"), session);

        assertEquals("Google", HeardAboutSession.getSource(session));
        assertEquals("202", HeardAboutSession.getGuestRowId(session));
    }

    @Test
    void friendsTap_storesSourceFriends() {
        Session session = Session.newDefault(555L, 555L);

        handler.handle(callback("ha_fr_303"), session);

        assertEquals("Friends", HeardAboutSession.getSource(session));
        assertEquals("303", HeardAboutSession.getGuestRowId(session));
    }

    @Test
    void otherTap_storesSourceOther() {
        Session session = Session.newDefault(555L, 555L);

        handler.handle(callback("ha_ot_404"), session);

        assertEquals("Other", HeardAboutSession.getSource(session));
        assertEquals("404", HeardAboutSession.getGuestRowId(session));
    }

    @Test
    void malformedPayload_missingGuestRowId_isIgnored() {
        Session session = Session.newDefault(555L, 555L);

        StepResult result = handler.handle(callback("ha_ig_"), session);

        assertEquals(StepResult.finish(), result);
        assertTrue(telegramServer.requests().isEmpty());
    }

    @Test
    void unrelatedCallback_isIgnored() {
        Session session = Session.newDefault(555L, 555L);

        StepResult result = handler.handle(callback("something_else"), session);

        assertEquals(StepResult.finish(), result);
        assertTrue(telegramServer.requests().isEmpty());
    }
}
