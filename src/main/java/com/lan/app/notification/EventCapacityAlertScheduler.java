package com.lan.app.notification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.config.TelegramConfig;
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
public class EventCapacityAlertScheduler {

    private static final Logger log = Logger.getLogger(EventCapacityAlertScheduler.class);
    private static final TypeReference<List<EventCapacityAlertDueDto>> LIST_TYPE = new TypeReference<>() {};

    private final TelegramClient telegramClient;
    private final TelegramConfig telegramConfig;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    @ConfigProperty(name = "app.backend-url", defaultValue = "")
    String backendUrl;

    @Inject
    public EventCapacityAlertScheduler(TelegramClient telegramClient, TelegramConfig telegramConfig) {
        this.telegramClient = telegramClient;
        this.telegramConfig = telegramConfig;
    }

    @Scheduled(every = "1m", concurrentExecution = ConcurrentExecution.SKIP)
    void sendFreedCapacityAlerts() {
        if (backendUrl.isBlank()) return;
        Long adminId = telegramConfig.adminChatId();
        if (adminId == null) return;

        List<EventCapacityAlertDueDto> due;
        try {
            due = fetchDue();
        } catch (Exception e) {
            log.warnf("Failed to fetch due event capacity alerts: %s", e.getMessage());
            return;
        }

        if (due.isEmpty()) return;
        log.infof("Processing %d freed-capacity alert(s)", due.size());

        for (var alert : due) {
            try {
                telegramClient.sendHtml(adminId, formatMessage(alert), null);
            } catch (Exception e) {
                log.warnf("Failed to send capacity alert for event '%s': %s", alert.eventName(), e.getMessage());
            }
        }
    }

    private String formatMessage(EventCapacityAlertDueDto alert) {
        int free = alert.maxCapacity() - alert.registeredCount();
        return "🟢 <b>Освободилось место</b>\n\n"
            + "🎪 <b>" + alert.eventName() + "</b>\n"
            + "Свободно: " + free + " из " + alert.maxCapacity() + "\n\n"
            + "Проверьте лист ожидания и свяжитесь с гостями.";
    }

    private List<EventCapacityAlertDueDto> fetchDue() throws Exception {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(backendUrl + "/events/v1/bot/event-capacity-alerts/due"))
            .GET()
            .build();
        var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("Backend returned HTTP " + resp.statusCode());
        }
        return mapper.readValue(resp.body(), LIST_TYPE);
    }
}
