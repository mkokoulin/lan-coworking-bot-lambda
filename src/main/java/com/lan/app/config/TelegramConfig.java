package com.lan.app.config;

import io.smallrye.config.ConfigMapping;
import jakarta.enterprise.context.ApplicationScoped;

@ConfigMapping(prefix = "telegram")
@ApplicationScoped
public interface TelegramConfig {
    String botToken();
    String apiBaseUrl();
    Long adminChatId();
}
