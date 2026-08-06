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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MyEventsCancelActionHandlerTest {

    private static final long ADMIN_CHAT_ID = 999L;

    private final ObjectMapper mapper = new ObjectMapper();
    private FakeHttpServer telegramServer;
    private FakeHttpServer backendServer;
    private MyEventsCancelActionHandler handler;

    @BeforeEach
    void setUp() {
        telegramServer = new FakeHttpServer();
        backendServer = new FakeHttpServer();
        backendServer.onPath("/events/v1/bot/my-registrations", 200, "[]");

        TelegramConfig config = new TelegramConfig() {
            public String botToken() { return "TESTTOKEN"; }
            public String apiBaseUrl() { return telegramServer.url(); }
            public Long adminChatId() { return ADMIN_CHAT_ID; }
        };
        TelegramClient telegramClient = new TelegramClient(config);
        MyEventsListHandler listHandler = new MyEventsListHandler(telegramClient, new I18n());
        listHandler.backendUrl = backendServer.url();

        handler = new MyEventsCancelActionHandler(telegramClient, new I18n(), listHandler);
        handler.backendUrl = backendServer.url();
        handler.adminChatId = ADMIN_CHAT_ID;
    }

    @AfterEach
    void tearDown() {
        telegramServer.close();
        backendServer.close();
    }

    private UpdateContext callback(String data) {
        return new UpdateContext(555L, "private", 555L, null, null, "/" + data, null, true, null, null, null, null);
    }

    private List<JsonNode> sentMessages() throws Exception {
        var out = new java.util.ArrayList<JsonNode>();
        for (var r : telegramServer.requestsTo("/botTESTTOKEN/sendMessage")) {
            out.add(mapper.readTree(r.body()));
        }
        return out;
    }

    @Test
    void tapOnCancelButtonShowsYesNoConfirmationAndStays() throws Exception {
        Session session = Session.newDefault(555L, 555L);
        session.setLang("ru");

        StepResult result = handler.handle(callback(MyEventsFlowDef.CB_CANCEL_PFX + "reg-1"), session);

        assertEquals(MyEventsFlowDef.STEP_CANCEL_ACTION, result.nextStep());
        assertTrue(backendServer.requestsTo("/events/v1/bot/registrations").isEmpty());

        var sent = sentMessages();
        assertEquals(1, sent.size());
        assertTrue(sent.get(0).get("text").asText().contains("Точно хотите отменить"));
        JsonNode buttons = sent.get(0).get("reply_markup").get("inline_keyboard").get(0);
        assertEquals("/me_y_reg-1", buttons.get(0).get("callback_data").asText());
        assertEquals("/me_n_reg-1", buttons.get(1).get("callback_data").asText());
    }

    @Test
    void tapOnNoAbortsAndShowsListAgain() throws Exception {
        Session session = Session.newDefault(555L, 555L);
        session.setLang("ru");

        handler.handle(callback(MyEventsFlowDef.CB_CANCEL_NO_PFX + "reg-1"), session);

        assertTrue(backendServer.requestsTo("/events/v1/bot/registrations/reg-1/cancel").isEmpty());
        var sent = sentMessages();
        assertEquals(2, sent.size());
        assertTrue(sent.get(0).get("text").asText().contains("остаётся в силе"));
    }

    @Test
    void tapOnYesCancelsAgainstBackendAndNotifiesGuestAndAdmin() throws Exception {
        backendServer.onPath("/events/v1/bot/registrations/reg-1/cancel", 200, """
            {"event_name":"Movie Night","date_start":"2026-03-01T18:00:00Z",
             "previous_guest_count":2,"guest_count":2,
             "guest_first_name":"Anna","guest_last_name":"Smith",
             "guest_phone":"+37491123456","guest_telegram":"@anna"}
            """);
        Session session = Session.newDefault(555L, 555L);
        session.setLang("ru");

        handler.handle(callback(MyEventsFlowDef.CB_CANCEL_YES_PFX + "reg-1"), session);

        var cancelRequests = backendServer.requestsTo("/events/v1/bot/registrations/reg-1/cancel");
        assertEquals(1, cancelRequests.size());
        assertEquals("POST", cancelRequests.get(0).method());

        var sent = sentMessages();
        // guest success message, admin notification, then the refreshed list — in that order
        assertEquals(3, sent.size());
        assertEquals(555, sent.get(0).get("chat_id").asLong());
        assertTrue(sent.get(0).get("text").asText().contains("Movie Night"));
        assertEquals(ADMIN_CHAT_ID, sent.get(1).get("chat_id").asLong());
        assertTrue(sent.get(1).get("text").asText().contains("Anna"));
        assertEquals(555, sent.get(2).get("chat_id").asLong());
    }

    @Test
    void conflictResponseShowsConflictMessageAndStillRefreshesList() throws Exception {
        backendServer.onPath("/events/v1/bot/registrations/reg-1/cancel", 409, "{}");
        Session session = Session.newDefault(555L, 555L);
        session.setLang("ru");

        handler.handle(callback(MyEventsFlowDef.CB_CANCEL_YES_PFX + "reg-1"), session);

        var sent = sentMessages();
        assertEquals(2, sent.size());
        assertTrue(sent.get(0).get("text").asText().contains("Не удалось отменить"));
    }

    @Test
    void serverErrorShowsGenericErrorAndStillRefreshesList() throws Exception {
        backendServer.onPath("/events/v1/bot/registrations/reg-1/cancel", 500, "{}");
        Session session = Session.newDefault(555L, 555L);
        session.setLang("ru");

        handler.handle(callback(MyEventsFlowDef.CB_CANCEL_YES_PFX + "reg-1"), session);

        var sent = sentMessages();
        assertEquals(2, sent.size());
        assertTrue(sent.get(0).get("text").asText().contains("Что-то пошло не так"));
    }

    @Test
    void blankBackendUrlSkipsCancelCallButStillRefreshesList() throws Exception {
        handler.backendUrl = "";
        Session session = Session.newDefault(555L, 555L);
        session.setLang("ru");

        handler.handle(callback(MyEventsFlowDef.CB_CANCEL_YES_PFX + "reg-1"), session);

        assertTrue(backendServer.requestsTo("/events/v1/bot/registrations").isEmpty());
        assertEquals(1, sentMessages().size());
    }

    @Test
    void nullCommandFinishesWithoutSendingAnything() throws Exception {
        Session session = Session.newDefault(555L, 555L);
        UpdateContext ctx = new UpdateContext(555L, "private", 555L, null, null, null, null, false, null, null, null, null);

        StepResult result = handler.handle(ctx, session);

        assertNull(result.nextFlow());
        assertTrue(sentMessages().isEmpty());
    }
}
