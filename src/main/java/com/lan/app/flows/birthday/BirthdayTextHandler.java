package com.lan.app.flows.birthday;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.service.BirthdayWishService;
import com.lan.app.service.GuestService;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.ui.KeyboardBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class BirthdayTextHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final BirthdayWishService birthdayWishService;
    private final GuestService guestService;

    @Inject
    public BirthdayTextHandler(
        TelegramClient telegramClient,
        I18n i18n,
        BirthdayWishService birthdayWishService,
        GuestService guestService
    ) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.birthdayWishService = birthdayWishService;
        this.guestService = guestService;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();
        String text = ctx.messageText();

        if (text == null || text.isBlank()) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "birthday_ask_text"), null);
            return StepResult.stay(BirthdayFlowDef.FLOW, BirthdayFlowDef.STEP_TEXT);
        }

        String authorName = resolveAuthorName(session);
        String telegramUserId = String.valueOf(session.getUserId());

        boolean ok = birthdayWishService.createWish(authorName, telegramUserId, text);

        session.setFlow("");
        session.setStep("");

        String msgKey = ok ? "birthday_success" : "birthday_error";
        var kb = KeyboardBuilder.inline(List.of(
            KeyboardBuilder.row(KeyboardBuilder.cbCmd(i18n.t(lang, "birthday_btn_home"), "/start"))
        ));
        telegramClient.sendHtml(session.getChatId(), i18n.t(lang, msgKey), kb);
        return StepResult.finish();
    }

    private String resolveAuthorName(Session session) {
        try {
            return guestService.findByChatId(session.getChatId())
                .map(g -> g.getFirstName() + " " + g.getLastName())
                .orElseGet(() -> "tg_" + session.getUserId());
        } catch (Exception e) {
            return "tg_" + session.getUserId();
        }
    }
}
