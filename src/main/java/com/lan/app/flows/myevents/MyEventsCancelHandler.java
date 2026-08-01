package com.lan.app.flows.myevents;

import com.lan.app.client.baserow.api.BotApi;
import com.lan.app.client.baserow.model.BotRegistrationActionResponse;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.ui.KeyboardBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Handles the "❌ Cancel" flow on myevents: confirm prompt (me_c_), then yes (me_y_) / no (me_n_). */
@ApplicationScoped
public class MyEventsCancelHandler implements StepHandler {

    private static final Logger log = Logger.getLogger(MyEventsCancelHandler.class);

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final BotApi botApi;
    private final MyEventsHandler myEventsHandler;

    @ConfigProperty(name = "telegram.admin-chat-id")
    Long adminChatId;

    @Inject
    public MyEventsCancelHandler(TelegramClient telegramClient, I18n i18n,
                                  @RestClient BotApi botApi, MyEventsHandler myEventsHandler) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.botApi = botApi;
        this.myEventsHandler = myEventsHandler;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();
        String cb = ctx.callbackData();
        if (cb == null) return StepResult.finish();

        if (cb.startsWith(MyEventsFlowDef.CB_CANCEL_YES_PFX)) {
            String regId = cb.substring(MyEventsFlowDef.CB_CANCEL_YES_PFX.length());
            return doCancel(ctx, session, regId);
        }
        if (cb.startsWith(MyEventsFlowDef.CB_CANCEL_NO_PFX)) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "myevents_cancel_aborted"), null);
            return myEventsHandler.handle(ctx, session);
        }
        if (cb.startsWith(MyEventsFlowDef.CB_CANCEL_PFX)) {
            String regId = cb.substring(MyEventsFlowDef.CB_CANCEL_PFX.length());
            var kb = KeyboardBuilder.inline(List.of(KeyboardBuilder.row(
                    KeyboardBuilder.rawBtn(i18n.t(lang, "myevents_btn_yes"), MyEventsFlowDef.CB_CANCEL_YES_PFX + regId),
                    KeyboardBuilder.rawBtn(i18n.t(lang, "myevents_btn_no"), MyEventsFlowDef.CB_CANCEL_NO_PFX + regId)
            )));
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "myevents_cancel_confirm"), kb);
            return StepResult.stay(MyEventsFlowDef.FLOW, MyEventsFlowDef.STEP_CANCEL_CONFIRM);
        }
        return StepResult.finish();
    }

    private StepResult doCancel(UpdateContext ctx, Session session, String regId) {
        String lang = session.getLang();
        try {
            BotRegistrationActionResponse result = botApi.botCancelRegistration(regId);
            telegramClient.sendHtml(session.getChatId(),
                    i18n.t(lang, "myevents_cancel_success").formatted(
                            result.getEventName(), formatDate(lang, result)), null);
            telegramClient.sendHtml(adminChatId, buildAdminMessage(result), null);
        } catch (WebApplicationException e) {
            int status = e.getResponse().getStatus();
            if (status == 409) {
                telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "myevents_cancel_conflict"), null);
            } else {
                log.warnf("cancel endpoint returned HTTP %d for regId=%s", status, regId);
                telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "myevents_error"), null);
            }
        } catch (Exception e) {
            log.warnf(e, "Failed to cancel registration %s", regId);
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "myevents_error"), null);
        }
        return myEventsHandler.handle(ctx, session);
    }

    private String buildAdminMessage(BotRegistrationActionResponse r) {
        String guestName = (safe(r.getGuestFirstName()) + " " + safe(r.getGuestLastName())).trim();
        var sb = new StringBuilder();
        sb.append("❌ <b>Отмена регистрации</b>\n\n");
        sb.append("Гость: ").append(guestName.isBlank() ? "-" : guestName).append("\n");
        sb.append("Мероприятие: ").append(safe(r.getEventName())).append("\n");
        if (r.getGuestPhone() != null && !r.getGuestPhone().isBlank()) {
            sb.append("Телефон: ").append(r.getGuestPhone()).append("\n");
        }
        if (r.getGuestTelegram() != null && !r.getGuestTelegram().isBlank()) {
            sb.append("Telegram: ").append(r.getGuestTelegram()).append("\n");
        }
        sb.append("Гостей: ").append(r.getGuestCount());
        return sb.toString();
    }

    private String formatDate(String lang, BotRegistrationActionResponse r) {
        if (r.getDateStart() == null) return "";
        boolean isRu = "ru".equals(lang);
        Locale locale = isRu ? Locale.of("ru") : Locale.ENGLISH;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(
                isRu ? "d MMMM yyyy, HH:mm" : "MMMM d, yyyy, HH:mm", locale);
        return r.getDateStart().format(fmt);
    }

    private String safe(String s) {
        return s != null ? s : "";
    }
}
