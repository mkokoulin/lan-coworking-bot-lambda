package com.lan.app.flows.siteregistration;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;

/**
 * Handles the "🔄 Повторить отправку в CRM" button on admin notifications for site
 * registrations that failed to sync to Baserow (see NotifyResource). Re-triggers the sync
 * on lan-site, which holds the registration data — the bot itself never stored it.
 */
@ApplicationScoped
public class SiteRegistrationRetryHandler implements StepHandler {

    public static final String CB_PREFIX = "site_reg_retry_";

    private static final Logger log = Logger.getLogger(SiteRegistrationRetryHandler.class);

    private final TelegramClient telegramClient;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @ConfigProperty(name = "app.site-url", defaultValue = "")
    String siteUrl;

    @ConfigProperty(name = "notify.secret", defaultValue = "")
    String notifySecret;

    @Inject
    public SiteRegistrationRetryHandler(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String cb = ctx.callbackData();
        if (cb == null || !cb.startsWith(CB_PREFIX)) return StepResult.finish();

        String regId = cb.substring(CB_PREFIX.length());

        telegramClient.answerCallbackQuery(ctx.callbackQueryId());

        if (siteUrl.isBlank() || notifySecret.isBlank()) {
            log.warn("app.site-url or notify.secret not configured, cannot retry site registration sync");
            telegramClient.sendHtml(ctx.chatId(), "⚠️ Не настроен адрес сайта — повтор недоступен.", null);
            return StepResult.finish();
        }

        RetryOutcome outcome = callRetry(regId);
        switch (outcome) {
            case SUCCESS -> {
                telegramClient.editMessageRemoveKeyboard(ctx.chatId(), ctx.messageId());
                telegramClient.sendHtml(ctx.chatId(), "✅ Регистрация добавлена в CRM (Baserow).", null);
            }
            case NOT_FOUND -> {
                telegramClient.editMessageRemoveKeyboard(ctx.chatId(), ctx.messageId());
                telegramClient.sendHtml(ctx.chatId(), "⚠️ Регистрация не найдена на сайте — возможно, уже обработана.", null);
            }
            case FAILED -> telegramClient.sendHtml(ctx.chatId(),
                    "❌ Не удалось добавить в CRM. Попробуйте ещё раз позже.", null);
        }

        return StepResult.finish();
    }

    private enum RetryOutcome { SUCCESS, NOT_FOUND, FAILED }

    private RetryOutcome callRetry(String regId) {
        try {
            String url = normalizeUrl(siteUrl) + "/api/register/retry";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + notifySecret)
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(Map.of("id", regId))))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                return RetryOutcome.SUCCESS;
            }
            if (resp.statusCode() == 404) {
                return RetryOutcome.NOT_FOUND;
            }
            log.warnf("site registration retry failed: HTTP %d regId=%s body=%s", resp.statusCode(), regId, resp.body());
            return RetryOutcome.FAILED;
        } catch (Exception e) {
            log.warnf(e, "site registration retry error regId=%s", regId);
            return RetryOutcome.FAILED;
        }
    }

    /** Mirrors EventConfirmHandler#normalizeUrl: collapses a doubled scheme and strips a trailing slash. */
    private static String normalizeUrl(String raw) {
        String url = raw.trim().replaceFirst("^(https?://)(https?://)+", "$1");
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
