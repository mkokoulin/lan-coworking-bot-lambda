package com.lan.app.flows.myevents;

import com.lan.app.session.Session;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MyEventsSessionTest {

    private static Session newSession() {
        return Session.newDefault(1L, 2L);
    }

    @Test
    void freshSession_hasNoPendingRegId() {
        Session s = newSession();

        assertThat(MyEventsSession.getPendingRegId(s)).isNull();
    }

    @Test
    void setPendingRegId_getPendingRegId_roundTrips() {
        Session s = newSession();

        MyEventsSession.setPendingRegId(s, "reg-42");

        assertThat(MyEventsSession.getPendingRegId(s)).isEqualTo("reg-42");
        assertThat(s.getPayloadJson()).contains("\"myevents.pending_reg_id\":\"reg-42\"");
    }

    @Test
    void clear_removesPendingRegId() {
        Session s = newSession();
        MyEventsSession.setPendingRegId(s, "reg-42");

        MyEventsSession.clear(s);

        assertThat(MyEventsSession.getPendingRegId(s)).isNull();
    }

    @Test
    void malformedPayloadJson_treatedAsEmptyMap_doesNotThrow() {
        Session s = newSession();
        s.setPayloadJson("not valid json");

        assertThat(MyEventsSession.getPendingRegId(s)).isNull();

        MyEventsSession.setPendingRegId(s, "reg-1");
        assertThat(MyEventsSession.getPendingRegId(s)).isEqualTo("reg-1");
    }
}
