package com.lan.app.session;

import com.lan.app.backend.BackendClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

@ApplicationScoped
public class BackendSessionRepository implements SessionRepository {

    private final BackendClient backendClient;

    @Inject
    public BackendSessionRepository(
        BackendClient backendClient
    ) {
        this.backendClient = backendClient;
    }

    @Override
    public Optional<Session> findByUserId(Long userId) {
        return backendClient.getSession(userId);
    }

    @Override
    public void save(Session session) {
        backendClient.saveSession(session);
    }
}
