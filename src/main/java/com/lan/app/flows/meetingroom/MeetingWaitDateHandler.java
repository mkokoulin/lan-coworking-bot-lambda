package com.lan.app.flows.meetingroom;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MeetingWaitDateHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;

    @Inject
    public MeetingWaitDateHandler(
        TelegramClient telegramClient,
        I18n i18n
    ) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();
        String payload = ctx.callbackPayload(); // "meet:date:2026-04-12"

        if (!ctx.hasCallback() || payload == null || !payload.startsWith(MeetingFlowDef.CB_DATE_PFX)) {
            return StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_DATE);
        }

        String date = payload.substring(MeetingFlowDef.CB_DATE_PFX.length());
        MeetingSession.setDate(session, date);

        telegramClient.sendHtml(
                session.getChatId(),
                i18n.t(lang, "meeting_pick_start_time").formatted(date),
                MeetingSlots.startTimeKeyboard()
        );
        return StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_START);
    }
}