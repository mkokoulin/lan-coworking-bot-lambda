package com.lan.app.flows.eventsurvey;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
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
public class EventSurveyTextHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final ReviewService reviewService;
    private final GuestService guestService;

    @Inject
    public EventSurveyTextHandler(
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

        boolean skip = ctx.hasCallback() && "survey_skip".equals(ctx.callbackPayload());
        String text = skip ? null : ctx.messageText();

        if (!skip && (text == null || text.isBlank())) {
            var kb = KeyboardBuilder.inline(List.of(
                KeyboardBuilder.row(KeyboardBuilder.cbCmd(i18n.t(lang, "review_btn_skip"), "survey_skip"))
            ));
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "review_ask_text"), kb);
            return StepResult.stay(EventSurveyFlowDef.FLOW, EventSurveyFlowDef.STEP_TEXT);
        }

        int rating = 5;
        try { rating = Integer.parseInt(EventSurveySession.getRating(session)); } catch (NumberFormatException ignored) {}

        Integer eventRowId = parseOrNull(EventSurveySession.getEventRowId(session));
        Integer guestRowId = parseOrNull(EventSurveySession.getGuestRowId(session));
        Integer registrationRowId = parseOrNull(EventSurveySession.getRegistrationRowId(session));

        String authorName = resolveAuthorName(session);

        boolean ok = reviewService.createReview(authorName, rating, text, eventRowId, guestRowId, registrationRowId);

        EventSurveySession.clear(session);
        session.setFlow("");
        session.setStep("");

        String msgKey = ok ? "review_success" : "review_error";
        var kb = KeyboardBuilder.inline(List.of(
            KeyboardBuilder.row(KeyboardBuilder.cbCmd(i18n.t(lang, "review_btn_home"), "/start"))
        ));
        telegramClient.sendHtml(session.getChatId(), i18n.t(lang, msgKey), kb);
        return StepResult.finish();
    }

    private static Integer parseOrNull(String value) {
        try { return value == null ? null : Integer.valueOf(value); }
        catch (NumberFormatException e) { return null; }
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
