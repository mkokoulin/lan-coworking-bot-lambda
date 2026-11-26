package com.lan.app.flows.eventchange;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.myevents.MyEventsFlowDef;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.ui.KeyboardBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/** Handles the "✏️ Изменились планы" tap (ecm_&lt;regId&gt;) — offers cancel or message-admin. */
@ApplicationScoped
public class EventChangeMenuHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;

    @Inject
    public EventChangeMenuHandler(TelegramClient telegramClient, I18n i18n) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();
        String cb = ctx.callbackData();
        String regId = cb != null && cb.startsWith(EventChangeFlowDef.CB_MENU_PREFIX)
                ? cb.substring(EventChangeFlowDef.CB_MENU_PREFIX.length())
                : null;

        if (regId == null || regId.isBlank()) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "event_change_menu_error"), null);
            return StepResult.finish();
        }

        var kb = KeyboardBuilder.inline(List.of(
                KeyboardBuilder.row(
                        KeyboardBuilder.rawBtn(i18n.t(lang, "event_change_menu_btn_cancel"),
                                MyEventsFlowDef.CB_CANCEL_PFX + regId)
                ),
                KeyboardBuilder.row(
                        KeyboardBuilder.rawBtn(i18n.t(lang, "event_change_menu_btn_message"),
                                EventChangeFlowDef.CB_PREFIX + regId)
                )
        ));
        telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "event_change_menu_prompt"), kb);
        return StepResult.finish();
    }
}
