package com.lan.app.flows.donation;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DonationDoneHandler implements StepHandler {

    @Inject TelegramClient telegramClient;
    @Inject I18n i18n;

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        telegramClient.sendHtml(session.getChatId(), i18n.t(session.getLang(), "donation_always"), null);
        session.setFlow("");
        session.setStep("");
        return StepResult.finish();
    }
}
