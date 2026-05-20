package com.lan.app.flows.registration;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RegistrationWaitAdditionalPhoneHandler implements StepHandler {

    private final TelegramClient telegramClient;

    @Inject
    public RegistrationWaitAdditionalPhoneHandler(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String rawPhone = ctx.messageText();
        String input = rawPhone != null ? rawPhone.trim() : "";

        if ("/skip".equals(input)) {
            return StepResult.stay(RegistrationFlowDef.FLOW, RegistrationFlowDef.STEP_SUMMARY);
        }

        String normalized = PhoneValidator.normalize(input.replaceAll("\\s+", ""));
        if (normalized != null) {
            RegistrationSession.setPhone(session, normalized);
            return StepResult.stay(RegistrationFlowDef.FLOW, RegistrationFlowDef.STEP_SUMMARY);
        }

        telegramClient.sendHtml(session.getChatId(),
                "Напиши свой армянский номер 😊 Он нужен, чтобы мы могли оперативно с тобой связаться!\n"
                + "Например: +374 XX XXX XXX\n\nНет армянского номера? Просто нажми /skip 👌", null);

        return StepResult.stay(RegistrationFlowDef.FLOW, RegistrationFlowDef.STEP_WAIT_ADDITIONAL_PHONE);
    }
}

