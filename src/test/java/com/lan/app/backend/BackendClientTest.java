package com.lan.app.backend;

import com.lan.app.session.Session;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class BackendClientTest {

    private static Session session(Long userId, Long chatId) {
        return Session.newDefault(chatId, userId);
    }

    @Test
    void getSession_unknownUserId_returnsEmpty() {
        BackendClient client = new BackendClient();

        assertThat(client.getSession(999L)).isEmpty();
    }

    @Test
    void saveThenGetSession_roundTrips() {
        BackendClient client = new BackendClient();
        Session s = session(200L, 100L);

        client.saveSession(s);

        Optional<Session> result = client.getSession(200L);
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(200L);
        assertThat(result.get().getChatId()).isEqualTo(100L);
    }

    @Test
    void saveSession_updatesUpdatedAtTimestamp() {
        BackendClient client = new BackendClient();
        Session s = session(200L, 100L);
        s.setUpdatedAt(OffsetDateTime.now().minusDays(1));
        OffsetDateTime before = s.getUpdatedAt();

        Session saved = client.saveSession(s);

        assertThat(saved.getUpdatedAt()).isAfter(before);
    }

    @Test
    void saveSession_returnsSameSessionInstance() {
        BackendClient client = new BackendClient();
        Session s = session(200L, 100L);

        Session saved = client.saveSession(s);

        assertThat(saved).isSameAs(s);
    }

    @Test
    void saveSession_overwritesPreviousSessionForSameUserId() {
        BackendClient client = new BackendClient();
        Session first = session(200L, 100L);
        client.saveSession(first);

        Session second = session(200L, 555L);
        client.saveSession(second);

        Optional<Session> result = client.getSession(200L);
        assertThat(result).isPresent();
        assertThat(result.get().getChatId()).isEqualTo(555L);
    }
}
