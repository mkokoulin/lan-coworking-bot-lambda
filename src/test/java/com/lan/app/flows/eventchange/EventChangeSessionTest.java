package com.lan.app.flows.eventchange;

import com.lan.app.session.Session;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventChangeSessionTest {

    private static Session newSession() {
        return Session.newDefault(1L, 2L);
    }

    @Test
    void freshSession_hasNoRegId() {
        Session s = newSession();

        assertThat(EventChangeSession.getRegId(s)).isNull();
    }

    @Test
    void setRegId_getRegId_roundTrips() {
        Session s = newSession();

        EventChangeSession.setRegId(s, "reg-42");

        assertThat(EventChangeSession.getRegId(s)).isEqualTo("reg-42");
        assertThat(s.getPayloadJson()).contains("\"event_change.reg_id\":\"reg-42\"");
    }

    @Test
    void clear_removesRegId() {
        Session s = newSession();
        EventChangeSession.setRegId(s, "reg-42");

        EventChangeSession.clear(s);

        assertThat(EventChangeSession.getRegId(s)).isNull();
    }

    @Test
    void malformedPayloadJson_treatedAsEmptyMap_doesNotThrow() {
        Session s = newSession();
        s.setPayloadJson("not valid json");

        assertThat(EventChangeSession.getRegId(s)).isNull();

        EventChangeSession.setRegId(s, "reg-1");
        assertThat(EventChangeSession.getRegId(s)).isEqualTo("reg-1");
    }
}
