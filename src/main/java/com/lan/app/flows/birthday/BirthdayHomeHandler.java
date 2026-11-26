package com.lan.app.flows.birthday;

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

@ApplicationScoped
public class BirthdayHomeHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;

    @Inject
    public BirthdayHomeHandler(TelegramClient telegramClient, I18n i18n) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        if (ctx.hasCallback() && BirthdayFlowDef.CB_WISH.equals(ctx.callbackPayload())) {
            var nameKb = KeyboardBuilder.inline(List.of(
                KeyboardBuilder.row(KeyboardBuilder.cbCmd(i18n.t(lang, "birthday_btn_anonymous"), BirthdayFlowDef.CB_ANONYMOUS))
            ));
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "birthday_ask_name"), nameKb);
            return StepResult.stay(BirthdayFlowDef.FLOW, BirthdayFlowDef.STEP_NAME);
        }

        var kb = KeyboardBuilder.inline(List.of(
            KeyboardBuilder.row(KeyboardBuilder.cbCmd(i18n.t(lang, "birthday_btn_wish"), BirthdayFlowDef.CB_WISH))
        ));
        telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "birthday_greeting"), kb);
        return StepResult.stay(BirthdayFlowDef.FLOW, BirthdayFlowDef.STEP_HOME);
    }
}
