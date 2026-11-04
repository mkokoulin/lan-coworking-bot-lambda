package com.lan.app.flows.myevents;

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

class MyEventsGuestsPromptHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private FakeHttpServer telegramServer;
    private MyEventsGuestsPromptHandler handler;

    @BeforeEach
    void setUp() {
        telegramServer = new FakeHttpServer();
        TelegramConfig config = new TelegramConfig() {
            public String botToken() { return "TESTTOKEN"; }
            public String apiBaseUrl() { return telegramServer.url(); }
            public Long adminChatId() { return 999L; }
        };
        handler = new MyEventsGuestsPromptHandler(new TelegramClient(config), new I18n());
    }

    @AfterEach
    void tearDown() {
        telegramServer.close();
    }

    private UpdateContext callback(String data) {
        return new UpdateContext(555L, "private", 555L, null, null, "/" + data, null, true, null, null, null, null);
    }

    @Test
    void storesPendingRegIdAndPromptsForCountWithBackButton() throws Exception {
        Session session = Session.newDefault(555L, 555L);
        session.setLang("ru");

        StepResult result = handler.handle(callback(MyEventsFlowDef.CB_GUESTS_PFX + "reg-42"), session);

        assertEquals(MyEventsFlowDef.FLOW, result.nextFlow());
        assertEquals(MyEventsFlowDef.STEP_GUESTS_WAIT, result.nextStep());
        assertEquals("reg-42", MyEventsSession.getPendingRegId(session));

        var sent = telegramServer.requestsTo("/botTESTTOKEN/sendMessage");
        assertEquals(1, sent.size());
        JsonNode body = mapper.readTree(sent.get(0).body());
        assertTrue(body.get("text").asText().contains("Введите новое количество"));
        JsonNode backBtn = body.get("reply_markup").get("inline_keyboard").get(0).get(0);
        assertEquals("/myevents", backBtn.get("callback_data").asText());
    }

    @Test
    void nullCommandFinishesWithoutSendingAnything() {
        Session session = Session.newDefault(555L, 555L);
        UpdateContext ctx = new UpdateContext(555L, "private", 555L, null, null, null, null, false, null, null, null, null);

        StepResult result = handler.handle(ctx, session);

        assertNull(result.nextFlow());
        assertTrue(telegramServer.requests().isEmpty());
    }

    @Test
    void nonMatchingPrefixFinishesWithoutSendingAnything() {
        Session session = Session.newDefault(555L, 555L);

        StepResult result = handler.handle(callback(MyEventsFlowDef.CB_CANCEL_PFX + "reg-1"), session);

        assertNull(result.nextFlow());
        assertTrue(telegramServer.requests().isEmpty());
    }
}
