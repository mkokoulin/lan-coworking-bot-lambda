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
public class BirthdayNameHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;

    @Inject
    public BirthdayNameHandler(TelegramClient telegramClient, I18n i18n) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        boolean anonymous = ctx.hasCallback() && BirthdayFlowDef.CB_ANONYMOUS.equals(ctx.callbackPayload());
        String name = anonymous ? null : ctx.messageText();

        if (!anonymous && (name == null || name.isBlank())) {
            var kb = KeyboardBuilder.inline(List.of(
                KeyboardBuilder.row(KeyboardBuilder.cbCmd(i18n.t(lang, "birthday_btn_anonymous"), BirthdayFlowDef.CB_ANONYMOUS))
            ));
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "birthday_ask_name"), kb);
            return StepResult.stay(BirthdayFlowDef.FLOW, BirthdayFlowDef.STEP_NAME);
        }

        BirthdaySession.setName(session, anonymous ? null : name.trim());

        telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "birthday_ask_text"), null);
        return StepResult.stay(BirthdayFlowDef.FLOW, BirthdayFlowDef.STEP_TEXT);
    }
}
