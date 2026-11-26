package com.lan.app.flows.eventslist;

import com.lan.app.client.baserow.api.EventResourceApi;
import com.lan.app.client.baserow.model.EventResponse;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.ui.KeyboardBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class EventDetailHandler implements StepHandler {

    private static final Logger LOG = Logger.getLogger(EventDetailHandler.class);
    private static final int MAX_DESC_CHARS = 800;

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final EventResourceApi eventApi;

    @Inject
    public EventDetailHandler(
        TelegramClient telegramClient,
        I18n i18n,
        @RestClient EventResourceApi eventApi
    ) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.eventApi = eventApi;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        // Callback data: "evt_<uuid>"
        String cb = ctx.callbackData();
        if (cb == null || !cb.startsWith(EventsListFlowDef.CB_EVT_PREFIX)) {
            return backToList(session, lang);
        }

        String uuidStr = cb.substring(EventsListFlowDef.CB_EVT_PREFIX.length());
        UUID eventId;
        try {
            eventId = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return backToList(session, lang);
        }

        EventResponse event;
        try {
            event = eventApi.eventsV1ExternalIdGet(eventId);
        } catch (Exception e) {
            LOG.warnf(e, "Failed to fetch event %s", uuidStr);
            telegramClient.sendHtml(session.getChatId(),
                i18n.t(lang, "events_detail_error"), backButton(lang));
            return StepResult.finish();
        }

        if (event == null) {
            telegramClient.sendHtml(session.getChatId(),
                i18n.t(lang, "events_detail_error"), backButton(lang));
            return StepResult.finish();
        }

        String text = buildDetailText(lang, event);

        var rows = new ArrayList<List<Map<String, String>>>();

        // Registration button
        if (Boolean.TRUE.equals(event.getShowForm())) {
            rows.add(KeyboardBuilder.row(
                EventsListHandler.rawBtn(
                    i18n.t(lang, "events_detail_register_btn"),
                    EventsListFlowDef.CB_EVT_REG_PREFIX + eventId
                )
            ));
        } else if (event.getExternalRegistrationUrl() != null) {
            rows.add(KeyboardBuilder.row(
                KeyboardBuilder.urlBtn(
                    i18n.t(lang, "events_detail_external_btn"),
                    event.getExternalRegistrationUrl().toString()
                )
            ));
        }

        // Instagram button
        if (event.getInstagramUrl() != null) {
            rows.add(KeyboardBuilder.row(
                KeyboardBuilder.urlBtn("📸 Instagram", event.getInstagramUrl().toString())
            ));
        }

        // Navigation — "Back" goes to the parent festival page if opened from one
        String parentFestivalId = EventsListSession.getParentFestivalId(session);
        if (parentFestivalId != null && !parentFestivalId.isBlank()) {
            rows.add(KeyboardBuilder.row(
                EventsListHandler.rawBtn(
                    i18n.t(lang, "events_detail_back_festival_btn"),
                    EventsListFlowDef.CB_EVF_PREFIX + parentFestivalId
                ),
                KeyboardBuilder.cbCmd(i18n.t(lang, "events_list_btn_home"), "/start")
            ));
        } else {
            rows.add(KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "events_detail_back_btn"), "events"),
                KeyboardBuilder.cbCmd(i18n.t(lang, "events_list_btn_home"), "/start")
            ));
        }

        telegramClient.sendHtml(session.getChatId(), text, KeyboardBuilder.inline(rows));
        return StepResult.stay(EventsListFlowDef.FLOW, EventsListFlowDef.STEP_DETAIL);
    }

    // ---- private helpers ----

    private String buildDetailText(String lang, EventResponse e) {
        boolean isRu = "ru".equals(lang);
        Locale locale = isRu ? Locale.of("ru") : Locale.ENGLISH;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(
            isRu ? "d MMMM yyyy, HH:mm" : "MMMM d, yyyy, HH:mm", locale)
            .withZone(ZoneId.of("Asia/Yerevan"));

        var sb = new StringBuilder();
        sb.append("<b>").append(escapeHtml(e.getName())).append("</b>\n");

        if (e.getDateStart() != null) {
            sb.append("\n🕐 ").append(e.getDateStart().format(fmt));
            if (e.getDateEnd() != null) {
                sb.append(" — ").append(e.getDateEnd().format(fmt));
            }
        }

        if (e.getDescription() != null && !e.getDescription().isBlank()) {
            String desc = e.getDescription().trim();
            if (desc.length() > MAX_DESC_CHARS) {
                desc = desc.substring(0, MAX_DESC_CHARS) + "…";
            }
            sb.append("\n\n").append(escapeHtml(desc));
        }

        return sb.toString().trim();
    }

    private StepResult backToList(Session session, String lang) {
        telegramClient.sendHtml(session.getChatId(),
            i18n.t(lang, "events_detail_error"), backButton(lang));
        return StepResult.finish();
    }

    private Object backButton(String lang) {
        return KeyboardBuilder.inline(List.of(
            KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "events_detail_back_btn"), "events"),
                KeyboardBuilder.cbCmd(i18n.t(lang, "events_list_btn_home"), "/start")
            )
        ));
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
