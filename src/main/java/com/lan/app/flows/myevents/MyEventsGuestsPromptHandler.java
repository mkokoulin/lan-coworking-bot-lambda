package com.lan.app.flows.myevents;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.ui.KeyboardBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/** Handles the "👥 Изменить кол-во" tap (me_g_<regId>) — asks for the new guest count as free text. */
@ApplicationScoped
public class MyEventsGuestsPromptHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;

    @Inject
    public MyEventsGuestsPromptHandler(TelegramClient telegramClient, I18n i18n) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();
        String raw = ctx.command();
        if (raw == null || !raw.startsWith(MyEventsFlowDef.CB_GUESTS_PFX)) {
            return StepResult.finish();
        }
        String regId = raw.substring(MyEventsFlowDef.CB_GUESTS_PFX.length());
        MyEventsSession.setPendingRegId(session, regId);

        var kb = KeyboardBuilder.inline(List.of(KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "my_events_btn_back"), "myevents")
        )));
        telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "my_events_guests_prompt"), kb);
        return StepResult.stay(MyEventsFlowDef.FLOW, MyEventsFlowDef.STEP_GUESTS_WAIT);
    }
}
