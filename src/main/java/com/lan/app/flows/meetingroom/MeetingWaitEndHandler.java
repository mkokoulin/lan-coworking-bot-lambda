package com.lan.app.flows.meetingroom;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class MeetingWaitEndHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;

    @Inject
    public MeetingWaitEndHandler(
        TelegramClient telegramClient,
        I18n i18n
    ) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }

    @ConfigProperty(name = "telegram.admin-chat-id")
    Long adminChatId;

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();
        String payload = ctx.callbackPayload(); // "meet:end:12:00"

        if (!ctx.hasCallback() || payload == null || !payload.startsWith(MeetingFlowDef.CB_END_PFX)) {
            return StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_END);
        }

        String date  = MeetingSession.getDate(session);
        String start = MeetingSession.getStart(session);
        if (date == null || start == null) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "meeting_flow_broken"), null);
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "meeting_pick_date"),
                    MeetingSlots.calendarKeyboard());
            return StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_DATE);
        }

        String end = payload.substring(MeetingFlowDef.CB_END_PFX.length());

        if (!MeetingSlots.isValidInterval(start, end)) {
            telegramClient.sendHtml(session.getChatId(),
                    i18n.t(lang, "meeting_invalid_interval").formatted(start + "–" + end), null);
            telegramClient.sendHtml(session.getChatId(),
                    i18n.t(lang, "meeting_pick_end_time").formatted(date, start),
                    MeetingSlots.endTimeKeyboard(start));
            return StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_END);
        }

        MeetingSession.setEnd(session, end);
        String intervalHuman = date + " " + start + "–" + end;

        String username = ctx.username();
        if (username != null && !username.isBlank()) {
            telegramClient.sendHtml(adminChatId,
                    i18n.t(lang, "meeting_request_admin").formatted(intervalHuman, "@" + username), null);
            telegramClient.sendHtml(session.getChatId(),
                    i18n.t(lang, "meeting_confirm_interval").formatted(intervalHuman), null);
            MeetingSession.clear(session);
            return StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_DONE);
        }

        telegramClient.sendHtml(session.getChatId(),
                i18n.t(lang, "meeting_need_contact").formatted(intervalHuman), null);
        return StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_CONTACT);
    }
}