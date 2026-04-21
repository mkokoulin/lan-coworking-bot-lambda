package com.lan.app.flows.registration;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RegistrationWaitPhoneHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;

    @Inject
    public RegistrationWaitPhoneHandler(TelegramClient telegramClient, I18n i18n) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
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
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Напиши свой армянский номер 😊 Он нужен, чтобы мы могли оперативно с тобой связаться!\n");
            stringBuilder.append("Например: +374 XX XXX XXX\n\n");
            stringBuilder.append("Нет армянского номера? Не страшно, просто нажми /skip 👌");
            telegramClient.sendHtml(
                session.getChatId(),
                stringBuilder.toString(), null);
            
            return StepResult.stay(RegistrationFlowDef.FLOW, RegistrationFlowDef.STEP_WAIT_ADDITIONAL_PHONE);   
        }

        RegistrationSession.setPhone(session, normalized);

        String lastFour = PhoneValidator.lastFour(normalized);
        telegramClient.sendHtml(session.getChatId(),
                i18n.t(lang, "reg_verify_phone").formatted(
                        normalized.substring(0, normalized.length() - 4) + "****"
                ), null);

        return StepResult.stay(RegistrationFlowDef.FLOW, RegistrationFlowDef.STEP_VERIFY_PHONE);
    }
}
