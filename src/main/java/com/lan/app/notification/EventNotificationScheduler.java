package com.lan.app.notification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.telegram.TelegramClient;
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
            boolean allSent = sendToAllChats(notification);
            markResult(notification.id, allSent);
        }
    }

    private boolean sendToAllChats(EventNotificationDueDto notification) {
        if (notification.chatIds == null || notification.chatIds.isEmpty()) return true;
        boolean allSent = true;
        for (Long chatId : notification.chatIds) {
            try {
                telegramClient.sendHtml(chatId, notification.message, null);
            } catch (Exception e) {
                log.warnf("Failed to send notification id=%d to chatId=%d: %s",
                    notification.id, chatId, e.getMessage());
                allSent = false;
            }
        }
        return allSent;
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

    private void markResult(int id, boolean success) {
        String path = success ? "/mark-sent" : "/mark-failed";
        try {
            var req = HttpRequest.newBuilder()
                .uri(URI.create(backendUrl + "/events/v1/bot/event-notifications/" + id + path))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
            http.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                .thenAccept(resp -> {
                    if (resp.statusCode() != 200) {
                        log.warnf("mark%s id=%d returned HTTP %d",
                            success ? "Sent" : "Failed", id, resp.statusCode());
                    }
                });
        } catch (Exception e) {
            log.warnf("Failed to call %s for notification id=%d: %s", path, id, e.getMessage());
        }
    }
}
