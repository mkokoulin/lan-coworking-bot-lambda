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
public class RegistrationWaitFinish implements StepHandler {
    
    private final TelegramClient telegramClient;
    private final I18n i18n;
    
    @ConfigProperty(name = "telegram.admin-chat-id")
    Long adminChatId;

    @Inject
    public RegistrationWaitFinish(
        TelegramClient telegramClient,
        I18n i18n
    ) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String firstName = RegistrationSession.getFirstName(session);
        String lastName  = RegistrationSession.getLastName(session);

        String username = RegistrationSession.getUsername(session);
        if ((username == null || username.isBlank()) && ctx.username() != null) {
            username = ctx.username();
            RegistrationSession.setUsername(session, username);
        }

        RegistrationSession.markRegistered(session);

        String contactLine = (username != null && !username.isBlank())
                ? "@" + username
                : "tg://user?id=" + session.getUserId();

        String adminMsg = "🆕 Новый гость:\n"
                + "👤 " + firstName + " " + lastName + "\n"
                + "📞 " + phone + "\n"
                + "✈️ " + contactLine;
        telegramClient.sendHtml(adminChatId, adminMsg, null);

        telegramClient.sendHtml(session.getChatId(),
                i18n.t(lang, "reg_success").formatted(firstName), null);

        RegistrationSession.clearTemp(session);
        session.setFlow("");
        session.setStep("");
        return StepResult.finish();
    }
}
