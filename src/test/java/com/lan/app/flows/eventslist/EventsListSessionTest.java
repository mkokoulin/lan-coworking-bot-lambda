package com.lan.app.flows.eventslist;

import com.lan.app.session.Session;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventsListSessionTest {

    private static Session newSession() {
        return Session.newDefault(1L, 2L);
    }

    @Test
    void freshSession_hasNoParentFestivalId() {
        Session s = newSession();

        assertThat(EventsListSession.getParentFestivalId(s)).isNull();
    }

    @Test
    void setParentFestivalId_getParentFestivalId_roundTrips() {
        Session s = newSession();

        EventsListSession.setParentFestivalId(s, "festival-1");

        assertThat(EventsListSession.getParentFestivalId(s)).isEqualTo("festival-1");
    }

    @Test
    void clearParentFestivalId_removesIt() {
        Session s = newSession();
        EventsListSession.setParentFestivalId(s, "festival-1");

        EventsListSession.clearParentFestivalId(s);

        assertThat(EventsListSession.getParentFestivalId(s)).isNull();
    }

    @Test
    void malformedPayloadJson_treatedAsEmptyMap_doesNotThrow() {
        Session s = newSession();
        s.setPayloadJson("not valid json");

        assertThat(EventsListSession.getParentFestivalId(s)).isNull();

        EventsListSession.setParentFestivalId(s, "festival-1");
        assertThat(EventsListSession.getParentFestivalId(s)).isEqualTo("festival-1");
    }
}
