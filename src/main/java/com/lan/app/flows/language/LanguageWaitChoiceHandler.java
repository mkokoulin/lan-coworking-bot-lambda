package com.lan.app.flows.language;

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
public class LanguageWaitChoiceHandler implements StepHandler {

    @Inject TelegramClient telegramClient;
    @Inject I18n i18n;

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        if (!ctx.hasCallback()) {
            return StepResult.stay(LanguageFlowDef.FLOW, LanguageFlowDef.STEP_WAIT_CHOICE);
        }

        // callbackPayload() срезает ведущий "/" → "lang:en" / "lang:ru"
        String payload = ctx.callbackPayload();

        String newLang;
        String label;
        if (LanguageFlowDef.CB_EN.equals(payload)) {
            newLang = "en";
            label   = "🇺🇸 English";
        } else if (LanguageFlowDef.CB_RU.equals(payload)) {
            newLang = "ru";
            label   = "🇷🇺 Русский";
        } else {
            return StepResult.stay(LanguageFlowDef.FLOW, LanguageFlowDef.STEP_WAIT_CHOICE);
        }

        session.setLang(newLang);

        var kb = KeyboardBuilder.inline(List.of(
            KeyboardBuilder.row(KeyboardBuilder.cbCmd(i18n.t(newLang, "profile_btn_back"), "start"))
        ));
        telegramClient.sendHtml(
                session.getChatId(),
                i18n.t(newLang, "language_selected").formatted(label),
                kb
        );

        // сбрасываем флоу — пользователь возвращается в свободное состояние
        session.setFlow("");
        session.setStep("");

        return StepResult.finish();
    }
}
