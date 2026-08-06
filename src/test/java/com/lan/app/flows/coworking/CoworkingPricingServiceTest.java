package com.lan.app.flows.coworking;

import com.lan.app.i18n.I18n;
import com.lan.app.testsupport.FakeHttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoworkingPricingServiceTest {

    private FakeHttpServer baserowServer;
    private CoworkingPricingService service;

    @BeforeEach
    void setUp() {
        baserowServer = new FakeHttpServer();
        service = new CoworkingPricingService();
        service.baserowUrl = baserowServer.url();
        service.baserowToken = "secret-token";
        service.i18n = new I18n();
    }

    @AfterEach
    void tearDown() {
        baserowServer.close();
    }

    @Test
    void requestsTariffsEndpoint() {
        baserowServer.onPath("/coworking/v1/tariffs", 200, "[]");

        service.formatPricesBlock("ru");

        var requests = baserowServer.requestsTo("/coworking/v1/tariffs");
        assertEquals(1, requests.size());
        assertEquals("GET", requests.get(0).method());
    }

    @Test
    void rendersDiscountedTariffWithStrikethroughAndRussianDescription() {
        baserowServer.onPath("/coworking/v1/tariffs", 200, """
            [{"name":"30 дней","price":75000,"discount":15000,"discountDescriptionRu":"Скидка на продление","discountDescriptionEn":"Renewal discount"}]
            """);

        String text = service.formatPricesBlock("ru");

        assertTrue(text.contains("30 дней"), text);
        assertTrue(text.contains("<s>75") && text.contains("</s>"), text);
        assertTrue(text.contains("60") && text.contains("֏"), text);
        assertTrue(text.contains("Скидка на продление"), text);
        assertFalse(text.contains("Renewal discount"), text);
    }

    @Test
    void rendersEnglishDescriptionForEnglishSession() {
        baserowServer.onPath("/coworking/v1/tariffs", 200, """
            [{"name":"30 days","price":75000,"discount":15000,"discountDescriptionRu":"Скидка на продление","discountDescriptionEn":"Renewal discount"}]
            """);

        String text = service.formatPricesBlock("en");

        assertTrue(text.contains("Renewal discount"), text);
        assertFalse(text.contains("Скидка на продление"), text);
    }

    @Test
    void rendersPlainPriceWhenNoDiscount() {
        baserowServer.onPath("/coworking/v1/tariffs", 200, """
            [{"name":"1 hour","price":1300,"discount":null}]
            """);

        String text = service.formatPricesBlock("en");

        assertTrue(text.contains("1 hour"), text);
        assertFalse(text.contains("<s>"), text);
    }

    @Test
    void omitsParenthesesWhenDiscountDescriptionMissing() {
        baserowServer.onPath("/coworking/v1/tariffs", 200, """
            [{"name":"30 days","price":75000,"discount":15000}]
            """);

        String text = service.formatPricesBlock("en");

        assertTrue(text.contains("<s>"), text);
        assertFalse(text.contains("("), text);
    }

    @Test
    void ignoresZeroDiscount() {
        baserowServer.onPath("/coworking/v1/tariffs", 200, """
            [{"name":"1 day","price":5000,"discount":0}]
            """);

        String text = service.formatPricesBlock("en");

        assertFalse(text.contains("<s>"), text);
    }

    @Test
    void escapesHtmlInNameAndDescription() {
        baserowServer.onPath("/coworking/v1/tariffs", 200, """
            [{"name":"<b>30</b> & days","price":75000,"discount":15000,"discountDescriptionEn":"<b>Sale</b> & save"}]
            """);

        String text = service.formatPricesBlock("en");

        assertTrue(text.contains("&lt;b&gt;30&lt;/b&gt; &amp; days"), text);
        assertTrue(text.contains("&lt;b&gt;Sale&lt;/b&gt; &amp; save"), text);
        assertFalse(text.contains("<b>"), text);
    }

    @Test
    void fallsBackToStaticTextWhenApiReturnsError() {
        baserowServer.onPath("/coworking/v1/tariffs", 500, "");

        String text = service.formatPricesBlock("ru");

        assertTrue(text.contains("Тарифы коворкинга"), text);
    }

    @Test
    void fallsBackToStaticTextWhenApiReturnsMalformedJson() {
        baserowServer.onPath("/coworking/v1/tariffs", 200, "not json");

        String text = service.formatPricesBlock("en");

        assertTrue(text.contains("Coworking prices"), text);
    }

    @Test
    void fallsBackWithoutHttpCallWhenBaserowUrlBlank() {
        service.baserowUrl = "";

        String text = service.formatPricesBlock("en");

        assertTrue(baserowServer.requests().isEmpty());
        assertTrue(text.contains("Coworking prices"), text);
    }

    @Test
    void fallsBackToStaticTextWhenTariffListIsEmpty() {
        baserowServer.onPath("/coworking/v1/tariffs", 200, "[]");

        String text = service.formatPricesBlock("ru");

        assertTrue(text.contains("Тарифы коворкинга"));
    }
}
