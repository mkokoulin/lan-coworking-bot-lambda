package com.lan.app.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

@ConfigMapping(prefix = "ga")
public interface GaConfigMapping {
    Optional<String> measurementId();
    Optional<String> apiSecret();

    @WithDefault("https://www.google-analytics.com/mp/collect")
    String endpoint();
}
