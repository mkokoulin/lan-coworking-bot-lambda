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

/** Covers the optional free-text comment step following a source-button tap. */
class HeardAboutCommentHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private FakeHttpServer telegramServer;
    private FakeHttpServer backendServer;
    private HeardAboutCommentHandler handler;

    @BeforeEach
    void setUp() {
        telegramServer = new FakeHttpServer();
        backendServer = new FakeHttpServer();

        TelegramConfig config = new TelegramConfig() {
            public String botToken() { return "TESTTOKEN"; }
            public String apiBaseUrl() { return telegramServer.url(); }
            public Long adminChatId() { return 999L; }
        };
        handler = new HeardAboutCommentHandler(new TelegramClient(config), new I18n());
        handler.backendUrl = backendServer.url();
    }

    @AfterEach
    void tearDown() {
        telegramServer.close();
        backendServer.close();
    }

    private static Session sessionWithChoice(String source, String guestRowId) {
        Session s = Session.newDefault(555L, 555L);
        HeardAboutSession.setSource(s, source);
        HeardAboutSession.setGuestRowId(s, guestRowId);
        return s;
    }

    private static UpdateContext textCtx(String text) {
        return new UpdateContext(555L, "private", 555L, null, text, null, null, false, "bob", null, null, null);
    }

    private static UpdateContext callbackCtx(String data) {
        return new UpdateContext(555L, "private", 555L, 55, null, "/" + data, "q1", true, "bob", null, null, null);
    }

    @Test
    void blankText_repromptsAndStaysOnComment() throws Exception {
        Session s = sessionWithChoice("Instagram", "101");

        StepResult result = handler.handle(textCtx("   "), s);

        assertEquals(StepResult.stay(HeardAboutFlowDef.FLOW, HeardAboutFlowDef.STEP_COMMENT), result);
        assertTrue(backendServer.requests().isEmpty());
        assertEquals(1, telegramServer.requestsTo("/botTESTTOKEN/sendMessage").size());
    }

    @Test
    void validText_savesAnswerAndFinishesClearingSession() throws Exception {
        Session s = sessionWithChoice("Google", "202");

        StepResult result = handler.handle(textCtx("saw an ad"), s);

        assertEquals(StepResult.finish(), result);
        assertEquals("", s.getFlow());
        assertEquals("", s.getStep());
        assertNull(HeardAboutSession.getSource(s));
        assertNull(HeardAboutSession.getGuestRowId(s));

        var answers = backendServer.requestsTo("/events/v1/bot/heard-about-source/202/answer");
        assertEquals(1, answers.size());
        JsonNode body = mapper.readTree(answers.get(0).body());
        assertEquals("Google", body.get("source").asText());
        assertEquals("saw an ad", body.get("comment").asText());
    }

    @Test
    void skipCallback_savesAnswerWithNullComment() throws Exception {
        Session s = sessionWithChoice("Friends", "303");

        StepResult result = handler.handle(callbackCtx(HeardAboutFlowDef.CB_SKIP), s);

        assertEquals(StepResult.finish(), result);
        var answers = backendServer.requestsTo("/events/v1/bot/heard-about-source/303/answer");
        assertEquals(1, answers.size());
        JsonNode body = mapper.readTree(answers.get(0).body());
        assertEquals("Friends", body.get("source").asText());
        assertTrue(body.get("comment").isNull());
    }

    @Test
    void guestIsStillThankedEvenWhenBackendUrlIsBlank() {
        handler.backendUrl = "";
        Session s = sessionWithChoice("Other", "404");

        StepResult result = handler.handle(textCtx("hello"), s);

        assertEquals(StepResult.finish(), result);
        assertEquals(1, telegramServer.requestsTo("/botTESTTOKEN/sendMessage").size());
        assertTrue(backendServer.requests().isEmpty());
    }
}
