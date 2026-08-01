package com.lan.app.flows.myevents;

import com.lan.app.client.baserow.api.BotApi;
import com.lan.app.client.baserow.model.BotRegistrationActionResponse;
import com.lan.app.client.baserow.model.GuestCountUpdateRequest;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * Handles "👥 Change guest count" on myevents: the first call is the me_g_&lt;regId&gt; callback tap
 * (prompts for a number), the second call is the free-text reply with the new count — same two-turn
 * shape as EventChangeHandler.
 */
@ApplicationScoped
public class MyEventsGuestCountHandler implements StepHandler {

    private static final Logger log = Logger.getLogger(MyEventsGuestCountHandler.class);

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final BotApi botApi;
    private final MyEventsHandler myEventsHandler;

    @ConfigProperty(name = "telegram.admin-chat-id")
    Long adminChatId;

    @Inject
    public MyEventsGuestCountHandler(TelegramClient telegramClient, I18n i18n,
                                      @RestClient BotApi botApi, MyEventsHandler myEventsHandler) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.botApi = botApi;
        this.myEventsHandler = myEventsHandler;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        if (ctx.hasCallback()) {
            String cb = ctx.callbackData();
            String regId = cb != null && cb.startsWith(MyEventsFlowDef.CB_GUEST_COUNT_PFX)
                    ? cb.substring(MyEventsFlowDef.CB_GUEST_COUNT_PFX.length())
                    : null;
            MyEventsSession.setPendingRegId(session, regId);
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "myevents_guests_prompt"), null);
            return StepResult.stay(MyEventsFlowDef.FLOW, MyEventsFlowDef.STEP_GUEST_COUNT_WAIT);
        }

        String text = ctx.messageText() != null ? ctx.messageText().trim() : "";
        int newCount;
        try {
            newCount = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            newCount = -1;
        }
        if (newCount < 1) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "myevents_guests_invalid"), null);
            return StepResult.stay(MyEventsFlowDef.FLOW, MyEventsFlowDef.STEP_GUEST_COUNT_WAIT);
        }

        String regId = MyEventsSession.getPendingRegId(session);
        if (regId == null) {
            return myEventsHandler.handle(ctx, session);
        }

        return applyGuestCount(ctx, session, regId, newCount);
    }

    private StepResult applyGuestCount(UpdateContext ctx, Session session, String regId, int newCount) {
        String lang = session.getLang();
        try {
            var request = new GuestCountUpdateRequest();
            request.setGuestCount(newCount);
            BotRegistrationActionResponse result = botApi.botUpdateRegistrationGuestCount(regId, request);

            telegramClient.sendHtml(session.getChatId(),
                    i18n.t(lang, "myevents_guests_success").formatted(
                            result.getEventName(), result.getPreviousGuestCount(), result.getGuestCount()),
                    null);
            telegramClient.sendHtml(adminChatId, buildAdminMessage(result), null);
            MyEventsSession.clear(session);
            return myEventsHandler.handle(ctx, session);
        } catch (WebApplicationException e) {
            int status = e.getResponse().getStatus();
            if (status == 409) {
                telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "myevents_guests_capacity"), null);
                return StepResult.stay(MyEventsFlowDef.FLOW, MyEventsFlowDef.STEP_GUEST_COUNT_WAIT);
            }
            log.warnf("guest-count endpoint returned HTTP %d for regId=%s", status, regId);
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "myevents_error"), null);
            MyEventsSession.clear(session);
            return myEventsHandler.handle(ctx, session);
        } catch (Exception e) {
            log.warnf(e, "Failed to update guest count for registration %s", regId);
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "myevents_error"), null);
            MyEventsSession.clear(session);
            return myEventsHandler.handle(ctx, session);
        }
    }

    private String buildAdminMessage(BotRegistrationActionResponse r) {
        String guestName = (safe(r.getGuestFirstName()) + " " + safe(r.getGuestLastName())).trim();
        var sb = new StringBuilder();
        sb.append("👥 <b>Изменение числа гостей</b>\n\n");
        sb.append("Гость: ").append(guestName.isBlank() ? "-" : guestName).append("\n");
        sb.append("Мероприятие: ").append(safe(r.getEventName())).append("\n");
        if (r.getGuestPhone() != null && !r.getGuestPhone().isBlank()) {
            sb.append("Телефон: ").append(r.getGuestPhone()).append("\n");
        }
        if (r.getGuestTelegram() != null && !r.getGuestTelegram().isBlank()) {
            sb.append("Telegram: ").append(r.getGuestTelegram()).append("\n");
        }
        sb.append("Было: ").append(r.getPreviousGuestCount()).append(" → Стало: ").append(r.getGuestCount());
        return sb.toString();
    }

    private String safe(String s) {
        return s != null ? s : "";
    }
}
