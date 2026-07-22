package com.lan.app.flows.eventchange;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class EventChangeHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;

    @ConfigProperty(name = "telegram.admin-chat-id")
    Long adminChatId;

    @Inject
    public EventChangeHandler(TelegramClient telegramClient, I18n i18n) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        if (ctx.hasCallback()) {
            String cb = ctx.callbackData();
            String regId = cb != null && cb.startsWith(EventChangeFlowDef.CB_PREFIX)
                    ? cb.substring(EventChangeFlowDef.CB_PREFIX.length())
                    : null;
            EventChangeSession.setRegId(session, regId);
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "event_change_prompt"), null);
            return StepResult.stay(EventChangeFlowDef.FLOW, EventChangeFlowDef.STEP_WAIT_MESSAGE);
        }

        String text = ctx.messageText() != null ? ctx.messageText().trim() : "";
        if (text.isBlank()) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "event_change_empty"), null);
            return StepResult.stay(EventChangeFlowDef.FLOW, EventChangeFlowDef.STEP_WAIT_MESSAGE);
        }

        String regId = EventChangeSession.getRegId(session);
        String guest = ctx.username() != null && !ctx.username().isBlank()
                ? "@" + ctx.username()
                : "id:" + ctx.userId();

        telegramClient.sendHtml(adminChatId,
                i18n.t(lang, "event_change_admin_notify").formatted(
                        guest, regId != null ? regId : "-", escapeHtml(text)),
                null);

        telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "event_change_thanks"), null);

        EventChangeSession.clear(session);
        return StepResult.finish();
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
