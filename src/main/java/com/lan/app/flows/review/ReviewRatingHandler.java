package com.lan.app.flows.review;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.ui.KeyboardBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class ReviewRatingHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;

    @Inject
    public ReviewRatingHandler(TelegramClient telegramClient, I18n i18n) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        if (ctx.hasCallback() && ctx.callbackData() != null && ctx.callbackData().startsWith("rate_")) {
            String ratingStr = ctx.callbackData().substring(5);
            ReviewSession.setRating(session, ratingStr);

            var kb = KeyboardBuilder.inline(List.of(
                KeyboardBuilder.row(KeyboardBuilder.cbCmd(i18n.t(lang, "review_btn_skip"), "review_skip"))
            ));
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "review_ask_text"), kb);
            return StepResult.stay(ReviewFlowDef.FLOW, ReviewFlowDef.STEP_TEXT);
        }

        var kb = KeyboardBuilder.inline(List.of(
            KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "review_btn_rate_1"), "rate_1"),
                KeyboardBuilder.cbCmd(i18n.t(lang, "review_btn_rate_2"), "rate_2"),
                KeyboardBuilder.cbCmd(i18n.t(lang, "review_btn_rate_3"), "rate_3"),
                KeyboardBuilder.cbCmd(i18n.t(lang, "review_btn_rate_4"), "rate_4"),
                KeyboardBuilder.cbCmd(i18n.t(lang, "review_btn_rate_5"), "rate_5")
            )
        ));
        telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "review_ask_rating"), kb);
        return StepResult.stay(ReviewFlowDef.FLOW, ReviewFlowDef.STEP_RATING);
    }
}
