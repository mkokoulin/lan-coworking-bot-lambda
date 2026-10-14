package com.lan.app.analytics;

import com.lan.app.domain.UpdateContext;
import com.lan.app.session.Session;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks bot usage in Google Analytics (GA4 Measurement Protocol).
 * Each Telegram user is mapped to a stable GA client_id, so GA reports
 * "users" as distinct Telegram accounts rather than raw requests.
 */
@ApplicationScoped
public class AnalyticsService {

    private final GoogleAnalyticsClient client;

    @Inject
    public AnalyticsService(GoogleAnalyticsClient client) {
        this.client = client;
    }

    public void trackUpdate(UpdateContext ctx, Session session, boolean newUser) {
        if (ctx == null || ctx.userId() == null) {
            return;
        }

        String clientId = "tg." + ctx.userId();

        if (newUser) {
            track(clientId, "bot_new_user", Map.of());
        }

        String command = ctx.command();
        if (command != null) {
            track(clientId, "bot_command", Map.of(
                    "command", sanitize(command),
                    "flow", sanitize(session.getFlow())
            ));
        } else if (ctx.hasCallback()) {
            track(clientId, "bot_callback", Map.of(
                    "callback", sanitize(ctx.callbackPayload())
            ));
        } else if (ctx.hasPhoto()) {
            track(clientId, "bot_photo", Map.of());
        } else if (ctx.hasSharedPhone()) {
            track(clientId, "bot_contact_shared", Map.of());
        } else if (ctx.hasText()) {
            track(clientId, "bot_message", Map.of());
        }
    }

    public void trackScreen(Long userId, Session session) {
        if (userId == null || session == null) {
            return;
        }
        String clientId = "tg." + userId;
        track(clientId, "bot_screen_view", Map.of(
                "flow", sanitize(session.getFlow()),
                "step", sanitize(session.getStep()),
                "screen", sanitize(session.getFlow() + "_" + session.getStep())
        ));
    }

    private void track(String clientId, String eventName, Map<String, Object> params) {
        Map<String, Object> withEngagement = new HashMap<>(params);
        // GA4 only counts an event towards active users/realtime if some engagement time is present.
        withEngagement.put("engagement_time_msec", "1");
        client.sendEvent(clientId, eventName, withEngagement);
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "(none)";
        }
        String cleaned = value.toLowerCase().replaceAll("[^a-z0-9_]", "_");
        return cleaned.length() > 100 ? cleaned.substring(0, 100) : cleaned;
    }
}
