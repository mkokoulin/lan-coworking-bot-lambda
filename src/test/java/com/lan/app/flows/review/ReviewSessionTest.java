package com.lan.app.flows.review;

import com.lan.app.session.Session;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewSessionTest {

    private static Session newSession() {
        return Session.newDefault(1L, 2L);
    }

    @Test
    void freshSession_hasNoRating() {
        Session s = newSession();

        assertThat(ReviewSession.getRating(s)).isNull();
    }

    @Test
    void setRating_roundTripsThroughPayloadJson() {
        Session s = newSession();

        ReviewSession.setRating(s, "5");

        assertThat(ReviewSession.getRating(s)).isEqualTo("5");
    }

    @Test
    void clear_removesRating() {
        Session s = newSession();
        ReviewSession.setRating(s, "5");

        ReviewSession.clear(s);

        assertThat(ReviewSession.getRating(s)).isNull();
    }

    @Test
    void malformedPayloadJson_treatedAsEmptyMap_doesNotThrow() {
        Session s = newSession();
        s.setPayloadJson("not valid json");

        assertThat(ReviewSession.getRating(s)).isNull();

        ReviewSession.setRating(s, "3");
        assertThat(ReviewSession.getRating(s)).isEqualTo("3");
    }
}
