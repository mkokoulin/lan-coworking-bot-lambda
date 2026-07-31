package com.lan.app.flows.myevents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.myevents.dto.RegistrationActionDto;
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
import java.util.Map;

/** Free-text step: waits for the new guest count typed after MyEventsGuestsPromptHandler. */
@ApplicationScoped
public class MyEventsGuestsWaitHandler implements StepHandler {

    private static final Logger log = Logger.getLogger(MyEventsGuestsWaitHandler.class);

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final MyEventsListHandler listHandler;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @ConfigProperty(name = "app.backend-url", defaultValue = "")
    String backendUrl;

    @ConfigProperty(name = "telegram.admin-chat-id")
    Long adminChatId;

    @Inject
    public MyEventsGuestsWaitHandler(TelegramClient telegramClient, I18n i18n, MyEventsListHandler listHandler) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.listHandler = listHandler;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        // callback-нажатие (кроме "/events", которое роутится в STEP_LIST раньше) игнорируем — ждём текст
        if (ctx.hasCallback()) {
            return StepResult.stay(MyEventsFlowDef.FLOW, MyEventsFlowDef.STEP_GUESTS_WAIT);
        }

        String regId = MyEventsSession.getPendingRegId(session);
        if (regId == null) {
            return listHandler.handle(ctx, session);
        }

        String text = ctx.messageText() != null ? ctx.messageText().trim() : "";
        int newCount;
        try {
            newCount = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            newCount = -1;
        }
        if (newCount < 1) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "my_events_guests_invalid"), null);
            return StepResult.stay(MyEventsFlowDef.FLOW, MyEventsFlowDef.STEP_GUESTS_WAIT);
        }

        return applyGuestCount(ctx, session, regId, newCount);
    }

    private StepResult applyGuestCount(UpdateContext ctx, Session session, String regId, int newCount) {
        String lang = session.getLang();
        if (backendUrl.isBlank()) {
            MyEventsSession.clear(session);
            return listHandler.handle(ctx, session);
        }
        try {
            String body = mapper.writeValueAsString(Map.of("guest_count", newCount));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(backendUrl + "/events/v1/bot/registrations/" + regId + "/guest-count"))
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                var result = mapper.readValue(resp.body(), RegistrationActionDto.class);
                telegramClient.sendHtml(session.getChatId(),
                        i18n.t(lang, "my_events_guests_success")
                                .formatted(result.eventName, result.previousGuestCount, result.guestCount),
                        null);
                telegramClient.sendHtml(adminChatId, buildAdminMessage(result), null);
                MyEventsSession.clear(session);
                return listHandler.handle(ctx, session);
            } else if (resp.statusCode() == 409) {
                telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "my_events_guests_capacity"), null);
                return StepResult.stay(MyEventsFlowDef.FLOW, MyEventsFlowDef.STEP_GUESTS_WAIT);
            } else {
                log.warnf("guest-count endpoint returned %d for regId=%s", resp.statusCode(), regId);
                telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "my_events_error"), null);
                MyEventsSession.clear(session);
                return listHandler.handle(ctx, session);
            }
        } catch (Exception e) {
            log.warnf("Failed to update guest count for registration %s: %s", regId, e.getMessage());
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "my_events_error"), null);
            MyEventsSession.clear(session);
            return listHandler.handle(ctx, session);
        }
    }

    private String buildAdminMessage(RegistrationActionDto r) {
        StringBuilder sb = new StringBuilder();
        sb.append("✏️ <b>Изменено количество гостей</b>\n\n");
        sb.append("🎪 ").append(r.eventName).append("\n");
        sb.append("📅 ").append(MyEventsSession.formatDate(r.dateStart)).append("\n");
        sb.append("👤 ").append(safe(r.guestFirstName)).append(" ").append(safe(r.guestLastName)).append("\n");
        if (r.guestPhone != null && !r.guestPhone.isBlank()) {
            sb.append("📞 ").append(r.guestPhone).append("\n");
        }
        if (r.guestTelegram != null && !r.guestTelegram.isBlank()) {
            sb.append("✈️ ").append(r.guestTelegram).append("\n");
        }
        sb.append("👥 было ").append(r.previousGuestCount).append(" → стало ").append(r.guestCount);
        return sb.toString();
    }

    private String safe(String s) {
        return s != null ? s : "";
    }
}
