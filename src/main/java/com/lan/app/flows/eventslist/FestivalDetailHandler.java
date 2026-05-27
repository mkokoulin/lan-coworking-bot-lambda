package com.lan.app.flows.eventslist;

import com.lan.app.client.baserow.api.EventResourceApi;
import com.lan.app.client.baserow.api.FestivalsApi;
import com.lan.app.client.baserow.model.EventResponse;
import com.lan.app.client.baserow.model.FestivalResponse;
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

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class FestivalDetailHandler implements StepHandler {

    private static final Logger LOG = Logger.getLogger(FestivalDetailHandler.class);
    private static final int MAX_DESC_CHARS = 600;

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final FestivalsApi festivalsApi;
    private final EventResourceApi eventApi;

    @Inject
    public FestivalDetailHandler(
        TelegramClient telegramClient,
        I18n i18n,
        @RestClient FestivalsApi festivalsApi,
        @RestClient EventResourceApi eventApi
    ) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.festivalsApi = festivalsApi;
        this.eventApi = eventApi;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        // Callback data: "evf_<uuid>"
        String cb = ctx.callbackData();
        if (cb == null || !cb.startsWith(EventsListFlowDef.CB_EVF_PREFIX)) {
            return backToList(session, lang);
        }

        String uuidStr = cb.substring(EventsListFlowDef.CB_EVF_PREFIX.length());
        UUID festivalId;
        try {
            festivalId = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return backToList(session, lang);
        }

        FestivalResponse festival;
        try {
            festival = festivalsApi.getFestivalById(festivalId);
        } catch (Exception e) {
            LOG.warnf(e, "Failed to fetch festival %s", uuidStr);
            telegramClient.sendHtml(session.getChatId(),
                i18n.t(lang, "events_detail_error"), backButton(lang));
            return StepResult.finish();
        }

        if (festival == null) {
            telegramClient.sendHtml(session.getChatId(),
                i18n.t(lang, "events_detail_error"), backButton(lang));
            return StepResult.finish();
        }

        String text = buildFestivalText(lang, festival);

        // Store festival context so EventDetailHandler can render the correct "Back" button
        EventsListSession.setParentFestivalId(session, festivalId.toString());

        var rows = new ArrayList<List<Map<String, String>>>();

        // Sub-events
        if (festival.getEventsIds() != null && !festival.getEventsIds().isEmpty()) {
            for (UUID eventId : festival.getEventsIds()) {
                try {
                    EventResponse event = eventApi.eventsV1ExternalIdGet(eventId);
                    if (event != null && event.getName() != null) {
                        String label = formatEventLabel(lang, event);
                        rows.add(KeyboardBuilder.row(
                            EventsListHandler.rawBtn(label, EventsListFlowDef.CB_EVT_PREFIX + eventId)
                        ));
                    }
                } catch (Exception e) {
                    LOG.warnf(e, "Failed to fetch sub-event %s of festival %s", eventId, festivalId);
                }
            }
        }

        // Navigation
        rows.add(KeyboardBuilder.row(
            KeyboardBuilder.cbCmd(i18n.t(lang, "events_detail_back_btn"), "events"),
            KeyboardBuilder.cbCmd(i18n.t(lang, "events_list_btn_home"), "/start")
        ));

        telegramClient.sendHtml(session.getChatId(), text, KeyboardBuilder.inline(rows));
        return StepResult.stay(EventsListFlowDef.FLOW, EventsListFlowDef.STEP_FESTIVAL);
    }

    // ---- private helpers ----

    private String buildFestivalText(String lang, FestivalResponse f) {
        boolean isRu = "ru".equals(lang);
        Locale locale = isRu ? Locale.of("ru") : Locale.ENGLISH;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(
            isRu ? "d MMMM yyyy" : "MMMM d, yyyy", locale);

        var sb = new StringBuilder();
        sb.append("🎪 <b>").append(escapeHtml(f.getName())).append("</b>\n");

        if (f.getDateStart() != null) {
            sb.append("\n📆 ").append(f.getDateStart().format(fmt));
            if (f.getDateEnd() != null) {
                sb.append(" — ").append(f.getDateEnd().format(fmt));
            }
        }

        if (f.getDescription() != null && !f.getDescription().isBlank()) {
            String desc = f.getDescription().trim();
            if (desc.length() > MAX_DESC_CHARS) {
                desc = desc.substring(0, MAX_DESC_CHARS) + "…";
            }
            sb.append("\n\n").append(escapeHtml(desc));
        }

        if (f.getEventsIds() != null && !f.getEventsIds().isEmpty()) {
            boolean isRuLang = "ru".equals(lang);
            sb.append("\n\n").append(isRuLang ? "🎟 Мероприятия фестиваля:" : "🎟 Festival events:");
        }

        return sb.toString().trim();
    }

    private String formatEventLabel(String lang, EventResponse e) {
        String name = e.getName().length() > 40 ? e.getName().substring(0, 38) + "…" : e.getName();
        if (e.getDateStart() != null) {
            boolean isRu = "ru".equals(lang);
            Locale locale = isRu ? Locale.of("ru") : Locale.ENGLISH;
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern(
                isRu ? "d MMM, HH:mm" : "MMM d, HH:mm", locale);
            return "📅 " + name + " — " + e.getDateStart().format(fmt);
        }
        return "📅 " + name;
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
