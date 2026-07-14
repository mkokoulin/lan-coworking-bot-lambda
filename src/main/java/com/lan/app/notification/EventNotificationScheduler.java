package com.lan.app.notification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.ui.KeyboardBuilder;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class EventNotificationScheduler {

    private static final Logger log = Logger.getLogger(EventNotificationScheduler.class);
    private static final TypeReference<List<EventNotificationDueDto>> LIST_TYPE = new TypeReference<>() {};

    private final TelegramClient telegramClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    @ConfigProperty(name = "app.backend-url", defaultValue = "")
    String backendUrl;

    @Inject
    public EventNotificationScheduler(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    @Scheduled(every = "1m", concurrentExecution = ConcurrentExecution.SKIP)
    void sendDueNotifications() {
        if (backendUrl.isBlank()) return;

        List<EventNotificationDueDto> due;
        try {
            due = fetchDue();
        } catch (Exception e) {
            log.warnf("Failed to fetch due event notifications: %s", e.getMessage());
            return;
        }

        if (due.isEmpty()) return;
        log.infof("Processing %d due event notification(s)", due.size());

        for (var notification : due) {
            List<NotificationResultDto> results = sendToRecipients(notification);
            saveResults(notification.id(), results);
        }
    }

    private List<NotificationResultDto> sendToRecipients(EventNotificationDueDto notification) {
        var results = new java.util.ArrayList<NotificationResultDto>();
        if (notification.recipients() == null || notification.recipients().isEmpty()) return results;
        for (var recipient : notification.recipients()) {
            if (recipient.chatId() == null) continue;
            try {
                var keyboard = attendanceKeyboard(notification.id(), recipient.guestRowId());
                telegramClient.sendHtml(recipient.chatId(), notification.message(), keyboard);
                results.add(new NotificationResultDto(recipient.guestRowId(), "SENT", null));
            } catch (Exception e) {
                String reason = e.getMessage();
                log.warnf("Failed to send notification id=%d to guestRowId=%d chatId=%d: %s",
                    notification.id(), recipient.guestRowId(), recipient.chatId(), reason);
                results.add(new NotificationResultDto(recipient.guestRowId(), "FAILED", reason));
            }
        }
        return results;
    }

    private Object attendanceKeyboard(int notificationId, int guestRowId) {
        String suffix = notificationId + "_" + guestRowId;
        return KeyboardBuilder.inline(List.of(
            KeyboardBuilder.row(
                Map.of("text", "✅ Всё в силе, буду!", "callback_data", "evt_att_yes_" + suffix),
                Map.of("text", "❌ Планы изменились, не смогу", "callback_data", "evt_att_no_" + suffix)
            )
        ));
    }

    private List<EventNotificationDueDto> fetchDue() throws Exception {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(backendUrl + "/events/v1/bot/event-notifications/due"))
            .GET()
            .build();
        var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("Backend returned HTTP " + resp.statusCode());
        }
        return mapper.readValue(resp.body(), LIST_TYPE);
    }

    private void saveResults(int id, List<NotificationResultDto> results) {
        if (results.isEmpty()) return;
        try {
            String body = mapper.writeValueAsString(results);
            var req = HttpRequest.newBuilder()
                .uri(URI.create(backendUrl + "/events/v1/bot/event-notifications/" + id + "/results"))
                .header("Content-Type", "application/json")
                .timeout(java.time.Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            // Sent synchronously — this runs inside a @Scheduled tick that may freeze
            // (Lambda) right after returning, which silently drops fire-and-forget requests.
            var resp = http.send(req, HttpResponse.BodyHandlers.discarding());
            if (resp.statusCode() != 200) {
                log.warnf("saveResults id=%d returned HTTP %d", id, resp.statusCode());
            }
        } catch (Exception e) {
            log.warnf("Failed to save results for notification id=%d: %s", id, e.getMessage());
        }
    }
}
