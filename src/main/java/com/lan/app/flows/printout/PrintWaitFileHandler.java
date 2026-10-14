package com.lan.app.flows.printout;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.registration.RegistrationSession;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class PrintWaitFileHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final Long adminChatId;

    @Inject
    public PrintWaitFileHandler(
            TelegramClient telegramClient,
            I18n i18n,
            @ConfigProperty(name = "telegram.admin-chat-id") Long adminChatId
    ) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.adminChatId = adminChatId;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        if (ctx.hasCallback()) {
            return StepResult.stay(PrintFlowDef.FLOW, PrintFlowDef.STEP_WAIT_FILE);
        }

        if (!ctx.hasPhoto()) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "print_no_file"), null);
            return StepResult.stay(PrintFlowDef.FLOW, PrintFlowDef.STEP_WAIT_FILE);
        }

        String details = PrintSession.getDetails(session);
        if (details == null) {
            // Shouldn't happen, but recover gracefully
            PrintSession.clear(session);
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "print_prompt"), null);
            return StepResult.stay(PrintFlowDef.FLOW, PrintFlowDef.STEP_WAIT_DETAILS);
        }

        String userTag   = ctx.username() != null ? "@" + ctx.username() : "id:" + ctx.userId();
        String guestId   = RegistrationSession.getGuestId(session);
        String userLabel = guestId != null ? userTag + " (guestId: " + guestId + ")" : userTag;
        String fileName  = ctx.fileName() != null ? ctx.fileName() : "—";

        String adminCaption = i18n.t(lang, "print_admin")
                .formatted(userLabel, fileName, details);

        telegramClient.sendDocumentByFileId(adminChatId, ctx.fileId(), adminCaption);

        PrintSession.clear(session);
        telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "print_done"), null);
        return StepResult.stay(PrintFlowDef.FLOW, PrintFlowDef.STEP_DONE);
    }
}
