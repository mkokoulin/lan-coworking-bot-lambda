package com.lan.app.flows.weeklydigest;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
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

/** Handles the "Unsubscribe" button tap on a message sent by {@link WeeklyDigestScheduler}. */
@ApplicationScoped
public class WeeklyDigestUnsubscribeHandler implements StepHandler {

    private static final String PREFIX = "digest_unsub_";

    private static final Logger log = Logger.getLogger(WeeklyDigestUnsubscribeHandler.class);

    @ConfigProperty(name = "app.backend-url", defaultValue = "")
    String backendUrl;

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Inject
    public WeeklyDigestUnsubscribeHandler(TelegramClient telegramClient, I18n i18n) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String cb = ctx.callbackData();
        if (cb == null || !cb.startsWith(PREFIX)) return StepResult.finish();

        int guestRowId;
        try {
            guestRowId = Integer.parseInt(cb.substring(PREFIX.length()));
        } catch (NumberFormatException e) {
            return StepResult.finish();
        }

        String lang = session.getLang() != null ? session.getLang() : "ru";

        telegramClient.answerCallbackQuery(ctx.callbackQueryId());
        telegramClient.editMessageRemoveKeyboard(ctx.chatId(), ctx.messageId());

        callUnsubscribe(guestRowId);

        telegramClient.sendHtml(ctx.chatId(), i18n.t(lang, "weekly_digest_unsubscribed"), null);

        return StepResult.finish();
    }

    private void callUnsubscribe(int guestRowId) {
        if (backendUrl.isBlank()) return;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(backendUrl + "/events/v1/bot/weekly-digest/" + guestRowId + "/unsubscribe"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 300) {
                log.warnf("weekly-digest unsubscribe endpoint returned %d for guestRowId=%d", resp.statusCode(), guestRowId);
            }
        } catch (Exception e) {
            log.warnf("Failed to unsubscribe guestRowId=%d from weekly digest: %s", guestRowId, e.getMessage());
        }
    }
}
