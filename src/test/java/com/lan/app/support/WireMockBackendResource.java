package com.lan.app.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Map;

/**
 * Starts one shared WireMock instance and points both {@code app.backend-url}
 * and {@code telegram.api-base-url} at it, for classes that make raw HttpClient
 * calls (TelegramClient, the notification schedulers, EventAttendanceHandler)
 * instead of going through an injectable/mockable REST client.
 */
public class WireMockBackendResource implements QuarkusTestResourceLifecycleManager {

    private WireMockServer server;

    @Override
    public Map<String, String> start() {
        server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();
        String baseUrl = server.baseUrl();
        return Map.of(
            "app.backend-url", baseUrl,
            "telegram.api-base-url", baseUrl
        );
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop();
        }
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(
            server,
            new TestInjector.AnnotatedAndMatchesType(WireMockInject.class, WireMockServer.class)
        );
    }
}
