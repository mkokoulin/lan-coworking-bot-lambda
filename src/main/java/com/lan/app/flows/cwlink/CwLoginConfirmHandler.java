package com.lan.app.flows.cwlink;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.service.GuestService;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class CwLoginConfirmHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final GuestService guestService;

    @Inject
    public CwLoginConfirmHandler(TelegramClient telegramClient, I18n i18n, GuestService guestService) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.guestService = guestService;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String cb = ctx.callbackData();
        if (cb == null) return StepResult.finish();

        boolean confirm = cb.startsWith("cw_confirm_");
        String uuidStr = confirm
                ? cb.substring("cw_confirm_".length())
                : cb.substring("cw_reject_".length());

        UUID guestId;
        try {
            guestId = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return StepResult.finish();
        }

        String lang = session.getLang() != null ? session.getLang() : "ru";

        telegramClient.answerCallbackQuery(ctx.callbackQueryId());
        telegramClient.editMessageRemoveKeyboard(ctx.chatId(), ctx.messageId());

        if (confirm) {
            guestService.confirmLink(guestId);
            telegramClient.sendHtml(ctx.chatId(), i18n.t(lang, "cw_login_confirmed"), null);
        } else {
            guestService.rejectLink(guestId);
            telegramClient.sendHtml(ctx.chatId(), i18n.t(lang, "cw_login_rejected"), null);
        }

        return StepResult.finish();
    }
}
