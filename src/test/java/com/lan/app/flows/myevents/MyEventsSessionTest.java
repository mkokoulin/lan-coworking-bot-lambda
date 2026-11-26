package com.lan.app.flows.myevents;

import com.lan.app.session.Session;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class MyEventsSessionTest {

    @Test
    void pendingRegIdRoundTripsThroughSessionPayload() {
        Session session = Session.newDefault(1L, 1L);
        assertNull(MyEventsSession.getPendingRegId(session));

        MyEventsSession.setPendingRegId(session, "abc-123");
        assertEquals("abc-123", MyEventsSession.getPendingRegId(session));

        MyEventsSession.clear(session);
        assertNull(MyEventsSession.getPendingRegId(session));
    }

    @Test
    void pendingRegIdSurvivesAlreadyPresentPayload() {
        Session session = Session.newDefault(1L, 1L);
        session.setPayloadJson("{}");

        MyEventsSession.setPendingRegId(session, "xyz");
        assertEquals("xyz", MyEventsSession.getPendingRegId(session));
    }

    @Test
    void malformedPayloadJson_treatedAsEmptyMap_doesNotThrow() {
        Session session = Session.newDefault(1L, 1L);
        session.setPayloadJson("not valid json");

        assertNull(MyEventsSession.getPendingRegId(session));

        MyEventsSession.setPendingRegId(session, "reg-1");
        assertEquals("reg-1", MyEventsSession.getPendingRegId(session));
    }

    @Test
    void parseInstantReturnsNullForBlankOrInvalidInput() {
        assertNull(MyEventsSession.parseInstant(null));
        assertNull(MyEventsSession.parseInstant(""));
        assertNull(MyEventsSession.parseInstant("not-a-date"));
    }

    @Test
    void parseInstantParsesIso8601() {
        assertEquals(Instant.parse("2026-01-15T10:00:00Z"),
                MyEventsSession.parseInstant("2026-01-15T10:00:00Z"));
    }

    @Test
    void formatDateReturnsDashForUnparsableInput() {
        assertEquals("—", MyEventsSession.formatDate(null));
        assertEquals("—", MyEventsSession.formatDate("garbage"));
    }

    @Test
    void guestsLabelUsesRussianPluralRules() {
        assertEquals("гость", MyEventsSession.guestsLabel(1));
        assertEquals("гость", MyEventsSession.guestsLabel(21));
        assertEquals("гостя", MyEventsSession.guestsLabel(2));
        assertEquals("гостя", MyEventsSession.guestsLabel(4));
        assertEquals("гостя", MyEventsSession.guestsLabel(22));
        assertEquals("гостей", MyEventsSession.guestsLabel(0));
        assertEquals("гостей", MyEventsSession.guestsLabel(5));
        assertEquals("гостей", MyEventsSession.guestsLabel(11));
        assertEquals("гостей", MyEventsSession.guestsLabel(12));
        assertEquals("гостей", MyEventsSession.guestsLabel(14));
        assertEquals("гостей", MyEventsSession.guestsLabel(111));
    }
}
