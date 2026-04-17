package com.lan.app.flows.registration;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class RegistrationVerifyPhoneHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;
    
    @ConfigProperty(name = "telegram.admin-chat-id")
    Long adminChatId;

    @Inject
    public RegistrationVerifyPhoneHandler(
        TelegramClient telegramClient,
        I18n i18n
    ) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }



    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        if (ctx.hasCallback()) {
            return StepResult.stay(RegistrationFlowDef.FLOW, RegistrationFlowDef.STEP_VERIFY_PHONE);
        }

        String input    = ctx.messageText() == null ? "" : ctx.messageText().trim().replaceAll("\\s+", "");
        String phone    = RegistrationSession.getPhone(session);
        String expected = PhoneValidator.lastFour(phone);

        if (!expected.equals(input)) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "reg_verify_wrong"), null);
            return StepResult.stay(RegistrationFlowDef.FLOW, RegistrationFlowDef.STEP_VERIFY_PHONE);
        }

        String firstName = RegistrationSession.getFirstName(session);
        String lastName  = RegistrationSession.getLastName(session);

        // Ник: сохранённый при вводе имени или свежий из текущего сообщения
        String username = RegistrationSession.getUsername(session);
        if ((username == null || username.isBlank()) && ctx.username() != null) {
            username = ctx.username();
            RegistrationSession.setUsername(session, username);
        }

        RegistrationSession.markRegistered(session);

        String contactLine = (username != null && !username.isBlank())
                ? "@" + username                          // есть ник — удобная ссылка
                : "tg://user?id=" + session.getUserId(); // нет ника — deep link по ID

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
