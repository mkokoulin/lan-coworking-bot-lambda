package com.lan.app.flows.tariffs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.config.TelegramConfig;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.coworking.CoworkingPricingService;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.testsupport.FakeHttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** CoworkingPricingService's own fetch/format/discount rendering is covered by
 *  CoworkingPricingServiceTest; this class only checks what's specific to the handler wiring. */
class TariffsHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private FakeHttpServer telegramServer;
    private TariffsHandler handler;

    @BeforeEach
    void setUp() {
        telegramServer = new FakeHttpServer();

        TelegramConfig config = new TelegramConfig() {
            public String botToken() { return "TESTTOKEN"; }
            public String apiBaseUrl() { return telegramServer.url(); }
            public Long adminChatId() { return 999L; }
        };

        // baserowUrl left blank on purpose: formatPricesBlock() falls back to the static
        // "coworking_prices" i18n text, which is enough to prove the handler wires the service in.
        handler = new TariffsHandler(new TelegramClient(config), new I18n(), new CoworkingPricingService(new I18n()));
    }

    @AfterEach
    void tearDown() {
        telegramServer.close();
    }

    private UpdateContext tariffsCommand() {
        return new UpdateContext(555L, "private", 555L, null, "/tariffs", null, false, null, null);
    }

    @Test
    void sendsPricesFromPricingService() throws Exception {
        Session session = Session.newDefault(555L, 555L);
        session.setLang("ru");
        handler.handle(tariffsCommand(), session);

        JsonNode body = mapper.readTree(telegramServer.requestsTo("/botTESTTOKEN/sendMessage").get(0).body());
        assertTrue(body.get("text").asText().contains("Тарифы коворкинга"));
    }

    @Test
    void backButtonReturnsToCoworkingHome() throws Exception {
        Session session = Session.newDefault(555L, 555L);
        session.setLang("ru");
        handler.handle(tariffsCommand(), session);

        JsonNode body = mapper.readTree(telegramServer.requestsTo("/botTESTTOKEN/sendMessage").get(0).body());
        JsonNode backBtn = body.get("reply_markup").get("inline_keyboard").get(0).get(0);
        assertEquals("/coworking", backBtn.get("callback_data").asText());
    }

    @Test
    void staysOnTariffsListStep() {
        Session session = Session.newDefault(555L, 555L);
        StepResult result = handler.handle(tariffsCommand(), session);

        assertEquals(TariffsFlowDef.FLOW, result.nextFlow());
        assertEquals(TariffsFlowDef.STEP_LIST, result.nextStep());
    }
}
