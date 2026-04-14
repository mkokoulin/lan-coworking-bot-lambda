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
public class MeetingWaitStartHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;

    @Inject
    public MeetingWaitStartHandler(
        TelegramClient telegramClient,
        I18n i18n
    ) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();
        String payload = ctx.callbackPayload(); // "meet:start:10:00"

        if (!ctx.hasCallback() || payload == null || !payload.startsWith(MeetingFlowDef.CB_START_PFX)) {
            return StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_START);
        }

        String date = MeetingSession.getDate(session);
        if (date == null) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "meeting_select_date_first"), null);
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "meeting_pick_date"),
                    MeetingSlots.calendarKeyboard());
            return StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_DATE);
        }

        String start = payload.substring(MeetingFlowDef.CB_START_PFX.length());
        MeetingSession.setStart(session, start);

        telegramClient.sendHtml(
                session.getChatId(),
                i18n.t(lang, "meeting_pick_end_time").formatted(date, start),
                MeetingSlots.endTimeKeyboard(start)
        );
        return StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_END);
    }
}