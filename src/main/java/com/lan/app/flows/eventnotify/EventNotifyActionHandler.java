package com.lan.app.flows.eventnotify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.eventnotify.dto.NotificationActionDto;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** Handles the "Всё в силе" / "Не смогу" inline-button taps sent by {@link EventReminderScheduler}. */
@ApplicationScoped
public class EventNotifyActionHandler implements StepHandler {

    private static final Logger log = Logger.getLogger(EventNotifyActionHandler.class);

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @ConfigProperty(name = "app.backend-url", defaultValue = "")
    String backendUrl;

    @ConfigProperty(name = "telegram.admin-chat-id")
    Long adminChatId;

    @Inject
    public EventNotifyActionHandler(TelegramClient telegramClient, I18n i18n) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();
        String raw = ctx.command();

        boolean confirmed;
        String payload;
        if (raw != null && raw.startsWith(EventNotifyFlowDef.PREFIX_YES)) {
            confirmed = true;
            payload = raw.substring(EventNotifyFlowDef.PREFIX_YES.length());
        } else if (raw != null && raw.startsWith(EventNotifyFlowDef.PREFIX_NO)) {
            confirmed = false;
            payload = raw.substring(EventNotifyFlowDef.PREFIX_NO.length());
        } else {
            return StepResult.finish();
        }

        String[] parts = payload.split("_");
        if (parts.length != 3) {
            log.warnf("Malformed event-notify callback payload: %s", raw);
            return StepResult.finish();
        }

        boolean saved;
        int notificationId;
        try {
            notificationId = Integer.parseInt(parts[0]);
            int guestRowId = Integer.parseInt(parts[1]);
            int registrationRowId = Integer.parseInt(parts[2]);
            log.infof("EventNotify callback: chatId=%d notification=%d guest=%d registration=%d confirmed=%s",
                session.getChatId(), notificationId, guestRowId, registrationRowId, confirmed);
            saved = recordAction(notificationId, guestRowId, registrationRowId, confirmed ? "CONFIRMED" : "DECLINED");
            log.infof("EventNotify callback result: chatId=%d notification=%d saved=%s",
                session.getChatId(), notificationId, saved);
        } catch (NumberFormatException e) {
            log.warnf("Malformed event-notify callback payload: %s", raw);
            return StepResult.finish();
        }

        // Only thank the guest if the answer actually made it to the backend — telling them
        // "confirmed" while the write silently failed left the scheduler with no record of it,
        // so the next 5-minute poll re-sent the exact same reminder (what guests reported as spam).
        if (saved) {
            telegramClient.sendHtml(session.getChatId(),
                    i18n.t(lang, confirmed ? "event_notify_thanks_confirmed" : "event_notify_thanks_declined"),
                    null);
        } else {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "event_notify_save_failed"), null);
            if (adminChatId != null) {
                telegramClient.sendHtml(adminChatId,
                        "⚠️ Не удалось сохранить ответ на напоминание (notification=" + notificationId
                                + ", chatId=" + session.getChatId() + ")", null);
            }
        }

        return StepResult.finish();
    }

    private boolean recordAction(int notificationId, int guestRowId, int registrationRowId, String action) {
        if (backendUrl.isBlank()) return false;
        try {
            var dto = new NotificationActionDto(guestRowId, registrationRowId, action);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(backendUrl + "/events/v1/bot/event-notifications/" + notificationId + "/action"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(dto)))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 300) {
                log.warnf("action endpoint returned %d for notification %d", resp.statusCode(), notificationId);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warnf("Failed to record guest action for notification %d: %s", notificationId, e.getMessage());
            return false;
        }
    }
}
