package com.lan.app.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "telegram")
public interface TelegramConfig {
    String botToken();
    String apiBaseUrl();
    Long adminChatId();
}
