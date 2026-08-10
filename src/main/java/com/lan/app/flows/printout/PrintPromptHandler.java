package com.lan.app.flows.printout;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PrintPromptHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;

    @Inject
    public PrintPromptHandler(TelegramClient telegramClient, I18n i18n) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        PrintSession.clear(session);
        telegramClient.sendHtml(session.getChatId(), i18n.t(session.getLang(), "print_prompt"), null);
        return StepResult.stay(PrintFlowDef.FLOW, PrintFlowDef.STEP_WAIT_DETAILS);
    }
}
