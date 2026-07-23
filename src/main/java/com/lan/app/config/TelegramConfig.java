package com.lan.app.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Wraps {@link TelegramConfigMapping} in a normal-scoped (ApplicationScoped) bean.
 * Quarkus registers {@code @ConfigMapping} interfaces as Dependent-scoped synthetic beans,
 * which cannot be swapped via {@code @InjectMock} in tests (CDI can only proxy normal scopes).
 */
@ApplicationScoped
public class TelegramConfig {

    @Inject
    TelegramConfigMapping mapping;

    public String botToken() {
        return mapping.botToken();
    }

    public String apiBaseUrl() {
        return mapping.apiBaseUrl();
    }

    public Long adminChatId() {
        return mapping.adminChatId();
    }
}
