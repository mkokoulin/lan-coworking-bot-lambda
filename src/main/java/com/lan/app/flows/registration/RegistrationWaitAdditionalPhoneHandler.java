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
        String lang = session.getLang();

        telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "reg_verify_wrong"), null);

        return StepResult.finish();
    }
}
