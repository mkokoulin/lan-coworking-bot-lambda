package com.lan.app.session;

import com.lan.app.backend.BackendClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class BackendSessionRepositoryTest {

    @Inject
    BackendSessionRepository repository;

    @InjectMock
    BackendClient backendClient;

    @Test
    void findByUserId_delegatesToBackendClient() {
        Session session = Session.newDefault(100L, 200L);
        when(backendClient.getSession(200L)).thenReturn(Optional.of(session));

        Optional<Session> result = repository.findByUserId(200L);

        assertThat(result).contains(session);
    }

    @Test
    void findByUserId_notFound_returnsEmpty() {
        when(backendClient.getSession(999L)).thenReturn(Optional.empty());

        assertThat(repository.findByUserId(999L)).isEmpty();
    }

    @Test
    void save_delegatesToBackendClient() {
        Session session = Session.newDefault(100L, 200L);

        repository.save(session);

        verify(backendClient).saveSession(session);
    }
}
