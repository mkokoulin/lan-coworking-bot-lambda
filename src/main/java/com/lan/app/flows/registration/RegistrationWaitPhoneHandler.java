package com.lan.app.flows.registration;

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
public class RegistrationWaitPhoneHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final RegistrationSummaryHandler summaryHandler;

    @Inject
    public RegistrationWaitPhoneHandler(TelegramClient telegramClient, I18n i18n, RegistrationSummaryHandler summaryHandler) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.summaryHandler = summaryHandler;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        if (ctx.hasCallback()) {
            return StepResult.stay(RegistrationFlowDef.FLOW, RegistrationFlowDef.STEP_WAIT_PHONE);
        }

        String rawPhone = ctx.sharedPhone() != null ? ctx.sharedPhone() : ctx.messageText();
        if (rawPhone == null || rawPhone.isBlank()) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "reg_phone_empty"), null);
            return StepResult.stay(RegistrationFlowDef.FLOW, RegistrationFlowDef.STEP_WAIT_PHONE);
        }

        String normalized = PhoneValidator.normalize(rawPhone.trim());
        if (normalized == null) {
            RegistrationSession.setAdditionalPhone(session, rawPhone.trim());
            var skipKb = KeyboardBuilder.inline(List.of(
                KeyboardBuilder.row(KeyboardBuilder.cbCmd(i18n.t(lang, "reg_btn_skip"), "reg_skip"))
            ));
            telegramClient.sendHtml(session.getChatId(),
                i18n.t(lang, "reg_need_armenian_phone"), skipKb);
            return StepResult.stay(RegistrationFlowDef.FLOW, RegistrationFlowDef.STEP_WAIT_ADDITIONAL_PHONE);
        }

        RegistrationSession.setPhone(session, normalized);
        return summaryHandler.handle(ctx, session);
    }
}
