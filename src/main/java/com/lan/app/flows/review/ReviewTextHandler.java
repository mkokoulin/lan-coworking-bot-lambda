package com.lan.app.flows.review;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.registration.RegistrationSession;
import com.lan.app.flows.start.StartFlowDef;
import com.lan.app.i18n.I18n;
import com.lan.app.service.GuestService;
import com.lan.app.service.ReviewService;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.ui.KeyboardBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class ReviewTextHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final ReviewService reviewService;
    private final GuestService guestService;

    @Inject
    public ReviewTextHandler(
        TelegramClient telegramClient,
        I18n i18n,
        ReviewService reviewService,
        GuestService guestService
    ) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.reviewService = reviewService;
        this.guestService = guestService;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        boolean skip = ctx.hasCallback() && "review_skip".equals(ctx.callbackPayload());
        String text = skip ? null : ctx.messageText();

        if (!skip && (text == null || text.isBlank())) {
            var kb = KeyboardBuilder.inline(List.of(
                KeyboardBuilder.row(KeyboardBuilder.cbCmd(i18n.t(lang, "review_btn_skip"), "review_skip"))
            ));
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "review_ask_text"), kb);
            return StepResult.stay(ReviewFlowDef.FLOW, ReviewFlowDef.STEP_TEXT);
        }

        String ratingStr = ReviewSession.getRating(session);
        int rating = 5;
        try { rating = Integer.parseInt(ratingStr); } catch (NumberFormatException ignored) {}

        String authorName = resolveAuthorName(session);

        boolean ok = reviewService.createReview(authorName, rating, text);

        ReviewSession.clear(session);
        session.setFlow("");
        session.setStep("");

        String msgKey = ok ? "review_success" : "review_error";
        var kb = KeyboardBuilder.inline(List.of(
            KeyboardBuilder.row(KeyboardBuilder.cbCmd(i18n.t(lang, "review_btn_home"), "/start"))
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
