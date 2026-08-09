package com.lan.app.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.config.GaConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Sends events to GA4 via the Measurement Protocol.
 * https://developers.google.com/analytics/devguides/collection/protocol/ga4
 *
 * Fire-and-forget: analytics must never slow down or break bot responses,
 * so failures are only logged at debug level.
 */
@ApplicationScoped
public class GoogleAnalyticsClient {

    private static final Logger log = Logger.getLogger(GoogleAnalyticsClient.class);

    private final GaConfig config;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Inject
    public GoogleAnalyticsClient(GaConfig config) {
        this.config = config;
    }

    public void sendEvent(String clientId, String eventName, Map<String, Object> params) {
        if (!config.enabled()) {
            log.debugf("GA disabled (missing measurement_id/api_secret), skipping event '%s'", eventName);
            return;
        }
        if (clientId == null || clientId.isBlank() || eventName == null || eventName.isBlank()) {
            return;
        }
        try {
            Map<String, Object> event = Map.of(
                    "name", eventName,
                    "params", params != null ? params : Map.of()
            );
            Map<String, Object> body = Map.of(
                    "client_id", clientId,
                    "events", List.of(event)
            );

            String url = config.endpoint()
                    + "?measurement_id=" + config.measurementId()
                    + "&api_secret=" + config.apiSecret();

            var req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            log.debugf("GA sending event '%s' clientId=%s", eventName, clientId);
            http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                    .whenComplete((resp, err) -> {
                        if (err != null) {
                            log.debugf("GA event '%s' failed to send: %s", eventName, err.getMessage());
                        } else if (resp.statusCode() >= 300) {
                            log.debugf("GA event '%s' rejected with status %d: %s", eventName, resp.statusCode(), resp.body());
                        } else {
                            log.debugf("GA event '%s' accepted with status %d", eventName, resp.statusCode());
                        }
                    });
        } catch (Exception e) {
            log.debugf(e, "GA event '%s' error: %s", eventName, e.getMessage());
        }
    }
}
