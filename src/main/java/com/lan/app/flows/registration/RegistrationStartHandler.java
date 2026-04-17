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
public class RegistrationStartHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;

    @Inject
    public RegistrationStartHandler(
        TelegramClient telegramClient,
        I18n i18n
    ) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        if (RegistrationSession.isRegistered(session)) {
            telegramClient.sendHtml(session.getChatId(),
                    i18n.t(session.getLang(), "reg_already_registered"), null);
            session.setFlow("");
            session.setStep("");
            return StepResult.finish();
        }

        telegramClient.sendHtml(session.getChatId(),
                i18n.t(session.getLang(), "reg_welcome"), null);

        return StepResult.stay(RegistrationFlowDef.FLOW, RegistrationFlowDef.STEP_WAIT_NAME);
    }
}
