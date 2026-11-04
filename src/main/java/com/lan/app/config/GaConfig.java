package com.lan.app.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Wraps {@link GaConfigMapping} in a normal-scoped (ApplicationScoped) bean.
 * Quarkus registers {@code @ConfigMapping} interfaces as Dependent-scoped synthetic beans,
 * which cannot be swapped via {@code @InjectMock} in tests (CDI can only proxy normal scopes).
 */
@ApplicationScoped
public class GaConfig {

    @Inject
    GaConfigMapping mapping;

    public String measurementId() {
        return mapping.measurementId().orElse(null);
    }

    public String apiSecret() {
        return mapping.apiSecret().orElse(null);
    }

    public String endpoint() {
        return mapping.endpoint();
    }

    public boolean enabled() {
        return notBlank(measurementId()) && notBlank(apiSecret());
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
