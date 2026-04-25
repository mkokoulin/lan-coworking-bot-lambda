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
public class RegistrationWaitNameHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;

    @Inject
    public RegistrationWaitNameHandler(
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
            return StepResult.stay(RegistrationFlowDef.FLOW, RegistrationFlowDef.STEP_WAIT_NAME);
        }

        String text = ctx.messageText() == null ? "" : ctx.messageText().trim();
        if (text.isBlank()) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "reg_name_empty"), null);
            return StepResult.stay(RegistrationFlowDef.FLOW, RegistrationFlowDef.STEP_WAIT_NAME);
        }

        String[] parts = text.split("\\s+", 2);
        String firstName = parts[0];
        String lastName  = parts.length > 1 ? parts[1] : "";

        if (firstName.length() < 2) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "reg_name_too_short"), null);
            return StepResult.stay(RegistrationFlowDef.FLOW, RegistrationFlowDef.STEP_WAIT_NAME);
        }

        RegistrationSession.setFirstName(session, firstName);
        RegistrationSession.setLastName(session, lastName);

        String username = ctx.username();
        if (username != null && !username.isBlank()) {
            RegistrationSession.setUsername(session, username);
        }

        if (ctx.chatType() != null && ctx.chatType().equals("private")) {
            telegramClient.sendPhoneRequest(
                session.getChatId(),
                i18n.t(lang, "reg_ask_phone"),
                i18n.t(lang, "reg_btn_share_phone")
            );
        } else {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "reg_ask_phone"), null);
        }

        return StepResult.stay(RegistrationFlowDef.FLOW, RegistrationFlowDef.STEP_WAIT_PHONE);
    }
}
