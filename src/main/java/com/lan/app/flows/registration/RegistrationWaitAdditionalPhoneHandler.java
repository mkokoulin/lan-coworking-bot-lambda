package com.lan.app.flows.registration;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RegistrationWaitAdditionalPhoneHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;
    
    @ConfigProperty(name = "telegram.admin-chat-id")
    Long adminChatId;

    @Inject
    public RegistrationWaitAdditionalPhoneHandler(
        TelegramClient telegramClient,
        I18n i18n
    ) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        // String lang = session.getLang();

        String rawPhone = ctx.messageText();

        if ("/skip".equals(rawPhone.trim())) {
            return StepResult.stay(RegistrationFlowDef.FLOW, RegistrationFlowDef.STEP_SUMMARY);
        }

        telegramClient.sendHtml(session.getChatId(), "Напиши свой армянский номер 😊 Он нужен, чтобы мы могли оперативно с тобой связаться!\n" + //
                        "Например: +374 XX XXX XXX", null);

        return StepResult.stay(RegistrationFlowDef.FLOW, RegistrationFlowDef.STEP_VERIFY_PHONE);
    }
}

