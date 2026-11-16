package com.lan.app.flows.eventsurvey;

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

/**
 * Entered only via the "survey_rate_{rating}_{eventRowId}_{guestRowId}_{registrationRowId}"
 * callback sent proactively by {@link EventSurveyScheduler} — unlike the generic review flow's
 * rating handler, this one never needs to draw the initial rating buttons itself.
 */
@ApplicationScoped
public class EventSurveyRatingHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;

    @Inject
    public EventSurveyRatingHandler(TelegramClient telegramClient, I18n i18n) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();
        String cb = ctx.callbackData();

        if (cb == null || !cb.startsWith(EventSurveyFlowDef.CB_SURVEY_RATE_PREFIX)) {
            return StepResult.finish();
        }

        String payload = cb.substring(EventSurveyFlowDef.CB_SURVEY_RATE_PREFIX.length());
        String[] parts = payload.split("_");
        if (parts.length != 4) {
            return StepResult.finish();
        }

        try {
            Integer.parseInt(parts[0]);
            Integer.parseInt(parts[1]);
            Integer.parseInt(parts[2]);
            Integer.parseInt(parts[3]);
        } catch (NumberFormatException e) {
            return StepResult.finish();
        }

        EventSurveySession.setRating(session, parts[0]);
        EventSurveySession.setEventRowId(session, parts[1]);
        EventSurveySession.setGuestRowId(session, parts[2]);
        EventSurveySession.setRegistrationRowId(session, parts[3]);

        var kb = KeyboardBuilder.inline(List.of(
            KeyboardBuilder.row(KeyboardBuilder.cbCmd(i18n.t(lang, "review_btn_skip"), "survey_skip"))
        ));
        telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "review_ask_text"), kb);
        return StepResult.stay(EventSurveyFlowDef.FLOW, EventSurveyFlowDef.STEP_TEXT);
    }
}
