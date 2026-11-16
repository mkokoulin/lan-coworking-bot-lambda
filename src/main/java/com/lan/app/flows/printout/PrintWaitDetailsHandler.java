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
public class PrintWaitDetailsHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;

    @Inject
    public PrintWaitDetailsHandler(TelegramClient telegramClient, I18n i18n) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        if (ctx.hasCallback()) {
            return StepResult.stay(PrintFlowDef.FLOW, PrintFlowDef.STEP_WAIT_DETAILS);
        }

        String text = ctx.messageText() != null ? ctx.messageText().trim() : "";
        if (text.isBlank() || text.startsWith("/")) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "print_empty_details"), null);
            return StepResult.stay(PrintFlowDef.FLOW, PrintFlowDef.STEP_WAIT_DETAILS);
        }
        if (text.length() > 500) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "print_details_too_long"), null);
            return StepResult.stay(PrintFlowDef.FLOW, PrintFlowDef.STEP_WAIT_DETAILS);
        }

        PrintSession.setDetails(session, text);
        telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "print_send_file"), null);
        return StepResult.stay(PrintFlowDef.FLOW, PrintFlowDef.STEP_WAIT_FILE);
    }
}
