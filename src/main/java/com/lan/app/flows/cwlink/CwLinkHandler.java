package com.lan.app.flows.cwlink;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.registration.RegistrationSession;
import com.lan.app.flows.start.StartFlowDef;
import com.lan.app.i18n.I18n;
import com.lan.app.service.GuestService;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.ui.KeyboardBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class CwLinkHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final GuestService guestService;

    @Inject
    public CwLinkHandler(TelegramClient telegramClient, I18n i18n, GuestService guestService) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.guestService = guestService;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String args = ctx.commandArgs();
        String lang = "ru";
        String source = "login"; // "signup" or "login"
        UUID guestId = null;

        // Format: cwlink_{uuid}_{lang}_{source} or cwlink_{uuid}_{lang} or cwlink_{uuid}
        if (args != null && args.startsWith("cwlink_")) {
            String payload = args.substring("cwlink_".length());
            String[] parts = payload.split("_", 2);
            try {
                guestId = UUID.fromString(parts[0]);
            } catch (IllegalArgumentException ignored) {}
            if (parts.length > 1 && !parts[1].isBlank()) {
                String[] langAndSource = parts[1].split("_", 2);
                lang = langAndSource[0];
                if (langAndSource.length > 1 && !langAndSource[1].isBlank()) {
                    source = langAndSource[1];
                }
            }
        }
        session.setLang(lang);

        if (guestId == null) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "cwlink_invalid"), null);
            session.setFlow(StartFlowDef.FLOW);
            session.setStep(StartFlowDef.STEP_SHOW);
            return StepResult.finish();
        }

        var outcome = guestService.linkChatById(guestId, session.getChatId());
        if (outcome.result() == GuestService.LinkChatResult.CHAT_ID_CONFLICT) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "cwlink_chat_id_conflict"), null);
            session.setFlow(StartFlowDef.FLOW);
            session.setStep(StartFlowDef.STEP_SHOW);
            return StepResult.finish();
        }
        if (outcome.result() != GuestService.LinkChatResult.LINKED) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "cwlink_not_found"), null);
            session.setFlow(StartFlowDef.FLOW);
            session.setStep(StartFlowDef.STEP_SHOW);
            return StepResult.finish();
        }

        var g = outcome.guest();
        RegistrationSession.clearLogout(session);
        RegistrationSession.markRegistered(session);
        if (g.getId() != null) {
            RegistrationSession.setGuestId(session, g.getId().toString());
        }
        guestService.confirmLink(guestId);

        var kb = KeyboardBuilder.inline(List.of(
            KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "cwlink_btn_profile"), "profile")
            )
        ));

        String msgKey = "signup".equals(source) ? "cwlink_success_signup" : "cwlink_success_login";
        telegramClient.sendHtml(session.getChatId(),
            i18n.t(lang, msgKey).formatted(g.getFirstName()), kb);

        session.setFlow(StartFlowDef.FLOW);
        session.setStep(StartFlowDef.STEP_SHOW);
        return StepResult.finish();
    }
}
