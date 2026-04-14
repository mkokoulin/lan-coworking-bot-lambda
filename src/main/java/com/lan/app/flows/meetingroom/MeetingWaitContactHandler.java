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
public class MeetingWaitContactHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;

    @Inject
    public MeetingWaitContactHandler(TelegramClient telegramClient,  I18n i18n) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }

    @ConfigProperty(name = "telegram.admin-chat-id")
    Long adminChatId;

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        // callback-нажатие игнорируем — ждём текст
        if (ctx.hasCallback()) {
            return StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_CONTACT);
        }

        String text = ctx.messageText() != null ? ctx.messageText().trim() : "";
        if (text.isBlank()) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "meeting_empty"), null);
            return StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_CONTACT);
        }
        if (text.length() > 64) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "meeting_contact_too_long"), null);
            return StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_CONTACT);
        }

        String date  = MeetingSession.getDate(session);
        String start = MeetingSession.getStart(session);
        String end   = MeetingSession.getEnd(session);

        if (date == null || start == null || end == null) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "meeting_flow_broken"), null);
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "meeting_pick_date"),
                    MeetingSlots.calendarKeyboard());
            return StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_DATE);
        }

        String intervalHuman = date + " " + start + "–" + end;
        String fallback = ctx.userId() != null ? "id:" + ctx.userId() : "unknown";
        String contact  = text + " | " + fallback;

        telegramClient.sendHtml(adminChatId,
                i18n.t(lang, "meeting_request_admin").formatted(intervalHuman, contact), null);
        telegramClient.sendHtml(session.getChatId(),
                i18n.t(lang, "meeting_confirm_interval").formatted(intervalHuman), null);

        MeetingSession.clear(session);
        return StepResult.stay(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_DONE);
    }
}