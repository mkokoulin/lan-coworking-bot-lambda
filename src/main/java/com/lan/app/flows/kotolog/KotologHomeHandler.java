package com.lan.app.flows.kotolog;

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
import java.util.Map;

@ApplicationScoped
public class KotologHomeHandler implements StepHandler {

    @Inject TelegramClient telegramClient;
    @Inject I18n i18n;

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        var kb = KeyboardBuilder.inline(List.of(
                KeyboardBuilder.row(
                        Map.of("text", i18n.t(lang, "kotolog_btn_view"),
                                "url",  KotologFlowDef.VIEW_URL)
                ),
                KeyboardBuilder.row(
                        KeyboardBuilder.cbCmd(i18n.t(lang, "kotolog_btn_help"), KotologFlowDef.CB_HELP)
                )
        ));

        telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "kotolog_intro"), kb);

        return StepResult.stay(KotologFlowDef.FLOW, KotologFlowDef.STEP_HOME);
    }
}