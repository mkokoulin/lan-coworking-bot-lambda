package com.lan.app.notification;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
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

/**
 * Handles the "Всё в силе, буду!" / "Планы изменились, не смогу" buttons on a
 * day-of event reminder (see EventNotificationScheduler). Bypasses the flow
 * system the same way CwLoginConfirmHandler does for cw_confirm_/cw_reject_.
 */
@ApplicationScoped
public class EventAttendanceHandler implements StepHandler {

    private static final Logger log = Logger.getLogger(EventAttendanceHandler.class);
    private static final String PREFIX_YES = "evt_att_yes_";
    private static final String PREFIX_NO = "evt_att_no_";

    private final TelegramClient telegramClient;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @ConfigProperty(name = "app.backend-url", defaultValue = "")
    String backendUrl;

    @Inject
    public EventAttendanceHandler(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String cb = ctx.callbackData();
        if (cb == null) return StepResult.finish();

        boolean confirmed = cb.startsWith(PREFIX_YES);
        String payload = confirmed ? cb.substring(PREFIX_YES.length()) : cb.substring(PREFIX_NO.length());
        String[] parts = payload.split("_", 2);
        if (parts.length != 2) return StepResult.finish();

        int notificationId;
        int guestRowId;
        try {
            notificationId = Integer.parseInt(parts[0]);
            guestRowId = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return StepResult.finish();
        }

        telegramClient.answerCallbackQuery(ctx.callbackQueryId());
        telegramClient.editMessageRemoveKeyboard(ctx.chatId(), ctx.messageId());

        recordAction(notificationId, guestRowId, confirmed ? "CONFIRMED" : "DECLINED");

        String reply = confirmed
            ? "Отлично, ждём вас! ✅"
            : "Жаль, что не получится в этот раз. Спасибо, что предупредили! ❌";
        telegramClient.sendHtml(ctx.chatId(), reply, null);

        return StepResult.finish();
    }

    private void recordAction(int notificationId, int guestRowId, String action) {
        if (backendUrl.isBlank()) return;
        try {
            String body = "{\"guestRowId\":" + guestRowId + ",\"action\":\"" + action + "\"}";
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(backendUrl + "/events/v1/bot/event-notifications/" + notificationId + "/action"))
                .header("Content-Type", "application/json")
                .timeout(java.time.Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            // Sent synchronously and joined before handle() returns — this runs inside a
            // @Scheduled poll tick that may freeze (Lambda) right after returning, which
            // silently drops any still-in-flight fire-and-forget request.
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                log.warnf("recordAction notificationId=%d guestRowId=%d returned HTTP %d",
                    notificationId, guestRowId, res.statusCode());
            }
        } catch (Exception e) {
            log.warnf("Failed to record attendance action notificationId=%d guestRowId=%d: %s",
                notificationId, guestRowId, e.getMessage());
        }
    }
}
