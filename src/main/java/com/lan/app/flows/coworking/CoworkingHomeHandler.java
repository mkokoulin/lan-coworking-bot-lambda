package com.lan.app.flows.coworking;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.FlowEntry;
import com.lan.app.engine.FlowRegistry;
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
public class CoworkingHomeHandler implements StepHandler {

    @Inject
    TelegramClient telegramClient;

    @Inject
    I18n i18n;

    @Inject
    FlowRegistry registry;

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        if (ctx.hasCallback() && ctx.callbackData().startsWith("/")) {
            String command = ctx.callbackData().substring(1); // "booking", "meetingroom", ...
            FlowEntry entry = registry.getCommand(command).orElse(null);
            if (entry != null) {
                session.setFlow(entry.flow());
                session.setStep(entry.step());
                return new StepResult(entry.flow(), entry.step());
            }
        }

        String text = i18n.t(lang, "coworking_intro") + "\n\n"
                + i18n.t(lang, "coworking_prices") + "\n\n"
                + i18n.t(lang, "coworking_meeting") + "\n\n"
                + i18n.t(lang, "coworking_options");

        var kb = KeyboardBuilder.inline(List.of(
                KeyboardBuilder.row(
                        KeyboardBuilder.cbCmd(i18n.t(lang, "coworking_btn_booking"), "booking"),
                        KeyboardBuilder.cbCmd(i18n.t(lang, "coworking_btn_meetingroom"), "meetingroom")
                ),
                KeyboardBuilder.row(
                        KeyboardBuilder.cbCmd(i18n.t(lang, "coworking_btn_events"), "events")
                ),
                KeyboardBuilder.row(
                        KeyboardBuilder.cbCmd(i18n.t(lang, "coworking_btn_about"), "about"),
                        KeyboardBuilder.cbCmd(i18n.t(lang, "coworking_btn_language"), "language")
                )
        ));

        telegramClient.sendHtml(session.getChatId(), text, kb);

        return StepResult.stay(CoworkingFlowDef.FLOW, CoworkingFlowDef.STEP_HOME);
    }
}