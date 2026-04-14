package com.lan.app.flows.start;

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
public class StartShowHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;

    @Inject
    public StartShowHandler(
        TelegramClient telegramClient,
        I18n i18n
    ) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        var kb = KeyboardBuilder.inline(List.of(
                KeyboardBuilder.row(
                        KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_booking"), "booking"),
                        KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_coworking"), "coworking")
                ),
                KeyboardBuilder.row(
                        KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_meetingroom"), "meetingroom"),
                        KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_printout"), "printout")
                ),
                KeyboardBuilder.row(
                        KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_events"), "events"),
                        KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_wifi"), "wifi")
                ),
                KeyboardBuilder.row(
                        KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_menu"), "menu"),
                        KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_about"), "about")
                ),
                KeyboardBuilder.row(
                        KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_language"), "language"),
                        KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_donation"), "donation")
                )
        ));

        telegramClient.sendHtml(
                session.getChatId(),
                i18n.t(lang, "start_message"),
                kb
        );

        return StepResult.stay(StartFlowDef.FLOW, StartFlowDef.STEP_SHOW);
    }
}