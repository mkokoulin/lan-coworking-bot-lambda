package com.lan.app.flows.printout;

import com.lan.app.session.Session;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrintSessionTest {

    private static Session newSession() {
        return Session.newDefault(1L, 2L);
    }

    @Test
    void freshSession_hasNoDetails() {
        Session s = newSession();

        assertThat(PrintSession.getDetails(s)).isNull();
    }

    @Test
    void setDetails_roundTripsThroughPayloadJson() {
        Session s = newSession();

        PrintSession.setDetails(s, "5 pages, double-sided");

        assertThat(PrintSession.getDetails(s)).isEqualTo("5 pages, double-sided");
    }

    @Test
    void clear_resetsDetails() {
        Session s = newSession();
        PrintSession.setDetails(s, "5 pages");

        PrintSession.clear(s);

        assertThat(PrintSession.getDetails(s)).isNull();
        assertThat(s.getPayloadJson()).isEqualTo("{}");
    }

    @Test
    void malformedPayloadJson_treatedAsEmptyMap_doesNotThrow() {
        Session s = newSession();
        s.setPayloadJson("not valid json");

        assertThat(PrintSession.getDetails(s)).isNull();

        PrintSession.setDetails(s, "5 pages");
        assertThat(PrintSession.getDetails(s)).isEqualTo("5 pages");
    }
}
