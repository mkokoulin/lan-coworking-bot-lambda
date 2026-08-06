package com.lan.app.flows.eventnotify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.flows.eventnotify.dto.EventNotificationDueDto;
import com.lan.app.flows.eventnotify.dto.EventNotificationRecipientDto;
import com.lan.app.flows.eventnotify.dto.NotificationResultDto;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.session.SessionRepository;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.ui.KeyboardBuilder;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Polls the backend for due event-reminder notifications (see BotResource#dueEventNotifications
 * on lan-baserow-api-lambda) and delivers them over Telegram with "Всё в силе" / "Не смогу"
 * inline buttons. This was the missing half of the reminder feature — the backend has always
 * computed "due" notifications, but nothing was consuming that endpoint or actually sending
 * anything, so guests never received a reminder and there was nothing for them to answer.
 */
@ApplicationScoped
public class EventReminderScheduler {

    private static final Logger log = Logger.getLogger(EventReminderScheduler.class);

    @ConfigProperty(name = "app.backend-url", defaultValue = "")
    String backendUrl;

    private final TelegramClient telegramClient;
    private final SessionRepository sessionRepository;
    private final I18n i18n;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Inject
    public EventReminderScheduler(TelegramClient telegramClient, SessionRepository sessionRepository, I18n i18n) {
        this.telegramClient = telegramClient;
        this.sessionRepository = sessionRepository;
        this.i18n = i18n;
    }

    @Scheduled(every = "5m")
    void pollAndSend() {
        if (backendUrl.isBlank()) {
            log.debug("app.backend-url not set, skipping event reminder poll");
            return;
        }

        List<EventNotificationDueDto> due;
        try {
            due = fetchDue();
        } catch (Exception e) {
            log.warnf("Failed to fetch due event notifications: %s", e.getMessage());
            return;
        }
        if (due.isEmpty()) return;

        log.infof("Sending %d due event notification(s)", due.size());
        for (EventNotificationDueDto notification : due) {
            sendOne(notification);
        }
    }

    private void sendOne(EventNotificationDueDto notification) {
        if (notification.recipients == null || notification.recipients.isEmpty()) return;

        List<NotificationResultDto> results = new ArrayList<>();
        for (EventNotificationRecipientDto recipient : notification.recipients) {
            if (recipient.chatId == null) continue;

            String lang = sessionRepository.findByUserId(recipient.chatId)
                    .map(Session::getLang)
                    .orElse("ru");
            // Mirrors I18n#t: only "ru" gets Russian copy, everything else (incl. "hy", which has
            // no dedicated reminder text) falls back to English, same as the rest of the bot's UI.
            String body = "ru".equals(lang) ? notification.messageRu : notification.messageEn;

            String suffix = notification.id + "_" + recipient.guestRowId + "_" + recipient.registrationRowId;
            var keyboard = KeyboardBuilder.inline(List.of(KeyboardBuilder.row(
                    KeyboardBuilder.cbCmd(i18n.t(lang, "event_notify_btn_yes"), EventNotifyFlowDef.PREFIX_YES + suffix),
                    KeyboardBuilder.cbCmd(i18n.t(lang, "event_notify_btn_no"), EventNotifyFlowDef.PREFIX_NO + suffix)
            )));

            try {
                telegramClient.sendHtml(recipient.chatId, body, keyboard);
                results.add(new NotificationResultDto(recipient.guestRowId, recipient.registrationRowId, "SENT", null));
            } catch (Exception e) {
                log.warnf("Failed to send reminder to chatId=%d: %s", recipient.chatId, e.getMessage());
                results.add(new NotificationResultDto(recipient.guestRowId, recipient.registrationRowId, "FAILED", e.getMessage()));
            }
        }

        try {
            saveResults(notification.id, results);
        } catch (Exception e) {
            log.warnf("Failed to save results for notification %d: %s", notification.id, e.getMessage());
        }
    }

    private List<EventNotificationDueDto> fetchDue() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(backendUrl + "/events/v1/bot/event-notifications/due"))
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("due endpoint returned " + resp.statusCode());
        }
        EventNotificationDueDto[] arr = mapper.readValue(resp.body(), EventNotificationDueDto[].class);
        return List.of(arr);
    }

    private void saveResults(int notificationId, List<NotificationResultDto> results) throws Exception {
        if (results.isEmpty()) return;
        String body = mapper.writeValueAsString(results);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(backendUrl + "/events/v1/bot/event-notifications/" + notificationId + "/results"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 300) {
            log.warnf("results endpoint returned %d for notification %d", resp.statusCode(), notificationId);
        }
    }
}
