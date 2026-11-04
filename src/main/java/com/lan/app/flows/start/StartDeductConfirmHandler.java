package com.lan.app.flows.start;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.registration.RegistrationSession;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.ui.KeyboardBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class StartDeductConfirmHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;

    @Inject
    public StartDeductConfirmHandler(TelegramClient telegramClient, I18n i18n) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        String tariffId = RegistrationSession.getDeductTariffId(session);
        String tariffName = RegistrationSession.getDeductTariffName(session);

        if (tariffId == null) {
            telegramClient.sendHtml(session.getChatId(),
                i18n.t(lang, "deduct_no_tariff"), null);
            session.setFlow(StartFlowDef.FLOW);
            session.setStep(StartFlowDef.STEP_SHOW);
            return StepResult.finish();
        }

        String name = (tariffName != null && !tariffName.isBlank()) ? tariffName : "—";

        var kb = KeyboardBuilder.inline(List.of(
            KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "deduct_btn_confirm"), "deduct_do"),
                KeyboardBuilder.cbCmd(i18n.t(lang, "deduct_btn_cancel"), "profile")
            )
        ));

        telegramClient.sendHtml(session.getChatId(),
            i18n.t(lang, "deduct_confirm").formatted(name), kb);

        return StepResult.stay(StartFlowDef.FLOW, StartFlowDef.STEP_DEDUCT_CONFIRM);
    }
}
