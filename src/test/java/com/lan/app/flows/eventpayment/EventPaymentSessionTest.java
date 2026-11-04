package com.lan.app.flows.eventpayment;

import com.lan.app.session.Session;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventPaymentSessionTest {

    private static Session newSession() {
        return Session.newDefault(1L, 2L);
    }

    @Test
    void freshSession_hasNoRegIdOrPrice() {
        Session s = newSession();

        assertThat(EventPaymentSession.getRegId(s)).isNull();
        assertThat(EventPaymentSession.getPrice(s)).isNull();
    }

    @Test
    void setters_roundTripThroughPayloadJson() {
        Session s = newSession();

        EventPaymentSession.setRegId(s, "reg-1");
        EventPaymentSession.setPrice(s, "1500");

        assertThat(EventPaymentSession.getRegId(s)).isEqualTo("reg-1");
        assertThat(EventPaymentSession.getPrice(s)).isEqualTo("1500");
        assertThat(s.getPayloadJson()).contains("\"pay_reg_id\":\"reg-1\"");
        assertThat(s.getPayloadJson()).contains("\"pay_price\":\"1500\"");
    }

    @Test
    void malformedPayloadJson_treatedAsEmptyMap_doesNotThrow() {
        Session s = newSession();
        s.setPayloadJson("not valid json");

        assertThat(EventPaymentSession.getRegId(s)).isNull();

        EventPaymentSession.setRegId(s, "reg-1");
        assertThat(EventPaymentSession.getRegId(s)).isEqualTo("reg-1");
    }
}
