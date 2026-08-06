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

        try {
            int notificationId = Integer.parseInt(parts[0]);
            int guestRowId = Integer.parseInt(parts[1]);
            int registrationRowId = Integer.parseInt(parts[2]);
            recordAction(notificationId, guestRowId, registrationRowId, confirmed ? "CONFIRMED" : "DECLINED");
        } catch (NumberFormatException e) {
            log.warnf("Malformed event-notify callback payload: %s", raw);
            return StepResult.finish();
        }

        telegramClient.sendHtml(session.getChatId(),
                i18n.t(lang, confirmed ? "event_notify_thanks_confirmed" : "event_notify_thanks_declined"),
                null);

        return StepResult.finish();
    }

    private void recordAction(int notificationId, int guestRowId, int registrationRowId, String action) {
        if (backendUrl.isBlank()) return;
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
            }
        } catch (Exception e) {
            log.warnf("Failed to record guest action for notification %d: %s", notificationId, e.getMessage());
        }
    }
}
