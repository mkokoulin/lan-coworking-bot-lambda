package com.lan.app.flows.meetingroom;

import com.lan.app.session.Session;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MeetingSessionTest {

    private static Session newSession() {
        return Session.newDefault(1L, 2L);
    }

    @Test
    void freshSession_hasNoFields() {
        Session s = newSession();

        assertThat(MeetingSession.getDate(s)).isNull();
        assertThat(MeetingSession.getStart(s)).isNull();
        assertThat(MeetingSession.getEnd(s)).isNull();
    }

    @Test
    void setters_roundTripThroughPayloadJson() {
        Session s = newSession();

        MeetingSession.setDate(s, "2026-04-12");
        MeetingSession.setStart(s, "10:00");
        MeetingSession.setEnd(s, "11:00");

        assertThat(MeetingSession.getDate(s)).isEqualTo("2026-04-12");
        assertThat(MeetingSession.getStart(s)).isEqualTo("10:00");
        assertThat(MeetingSession.getEnd(s)).isEqualTo("11:00");
    }

    @Test
    void clear_removesAllMeetingFields() {
        Session s = newSession();
        MeetingSession.setDate(s, "2026-04-12");
        MeetingSession.setStart(s, "10:00");
        MeetingSession.setEnd(s, "11:00");

        MeetingSession.clear(s);

        assertThat(MeetingSession.getDate(s)).isNull();
        assertThat(MeetingSession.getStart(s)).isNull();
        assertThat(MeetingSession.getEnd(s)).isNull();
    }

    @Test
    void malformedPayloadJson_treatedAsEmptyMap_doesNotThrow() {
        Session s = newSession();
        s.setPayloadJson("not valid json");

        assertThat(MeetingSession.getDate(s)).isNull();

        MeetingSession.setDate(s, "2026-04-12");
        assertThat(MeetingSession.getDate(s)).isEqualTo("2026-04-12");
    }
}
