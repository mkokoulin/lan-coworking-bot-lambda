package com.lan.app.flows.weeklydigest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.client.baserow.model.CoworkingGuestResponse;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.service.GuestService;
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
import java.util.LinkedHashMap;
import java.util.Optional;

/** Handles the "Подписаться на дайджест" button tap from the events list. */
@ApplicationScoped
public class WeeklyDigestSubscribeHandler implements StepHandler {

    private static final Logger log = Logger.getLogger(WeeklyDigestSubscribeHandler.class);

    @ConfigProperty(name = "app.backend-url", defaultValue = "")
    String backendUrl;

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final GuestService guestService;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Inject
    public WeeklyDigestSubscribeHandler(TelegramClient telegramClient, I18n i18n, GuestService guestService) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.guestService = guestService;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang() != null ? session.getLang() : "ru";

        telegramClient.answerCallbackQuery(ctx.callbackQueryId());

        boolean ok = callSubscribe(session.getChatId());
        telegramClient.sendHtml(session.getChatId(),
            i18n.t(lang, ok ? "weekly_digest_subscribed" : "events_list_error"), null);

        return StepResult.finish();
    }

    // Upserts a guest by chatId/phone (same semantics as event registration) and opts them into
    // the digest — reuses whatever coworking-guest profile already exists for nicer prefill, and
    // falls back to placeholder values (same pattern as EventRegisterHandler) for brand-new chats.
    private boolean callSubscribe(Long chatId) {
        if (backendUrl.isBlank()) return false;

        String firstName = "User";
        String phone = "tg:" + chatId;
        String telegram = null;

        Optional<CoworkingGuestResponse> cwGuest = guestService.findByChatId(chatId);
        if (cwGuest.isPresent()) {
            var g = cwGuest.get();
            if (g.getFirstName() != null && !g.getFirstName().isBlank()) firstName = g.getFirstName();
            if (g.getPhone() != null && !g.getPhone().isBlank()) phone = g.getPhone();
            if (g.getTelegram() != null && !g.getTelegram().isBlank()) telegram = "@" + g.getTelegram();
        }

        try {
            var body = new LinkedHashMap<String, Object>();
            body.put("first_name", firstName);
            body.put("phone", phone);
            body.put("telegram", telegram);
            body.put("source", "telegram-bot");
            body.put("chat_id", chatId);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(backendUrl + "/events/v1/bot/weekly-digest/subscribe"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 300) {
                log.warnf("weekly-digest subscribe endpoint returned %d for chatId=%d", resp.statusCode(), chatId);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warnf(e, "Failed to subscribe chatId=%d to weekly digest", chatId);
            return false;
        }
    }
}
