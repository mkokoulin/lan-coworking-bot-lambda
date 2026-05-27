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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class EventsListHandler implements StepHandler {

    private static final Logger LOG = Logger.getLogger(EventsListHandler.class);
    private static final int MAX_EVENTS    = 8;
    private static final int MAX_FESTIVALS = 5;

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final EventResourceApi eventApi;
    private final FestivalsApi festivalsApi;

    @Inject
    public EventsListHandler(
        TelegramClient telegramClient,
        I18n i18n,
        @RestClient EventResourceApi eventApi,
        @RestClient FestivalsApi festivalsApi
    ) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.eventApi = eventApi;
        this.festivalsApi = festivalsApi;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        List<EventResponse> events = fetchEvents();
        List<FestivalResponse> festivals = fetchFestivals();

        if (events == null && festivals == null) {
            telegramClient.sendHtml(session.getChatId(),
                i18n.t(lang, "events_list_error"), homeButton(lang));
            return StepResult.finish();
        }

        // Clear festival navigation context — user is back at the main list
        EventsListSession.clearParentFestivalId(session);

        List<EventResponse> safeEvents = events != null ? events : List.of();
        List<FestivalResponse> safeFestivals = festivals != null
            ? festivals.stream().filter(f -> Boolean.TRUE.equals(f.getIsVisible())).toList()
            : List.of();

        // Collect all event IDs that belong to any festival so they can be hidden from the plain list
        Set<UUID> festivalEventIds = safeFestivals.stream()
            .filter(f -> f.getEventsIds() != null)
            .flatMap(f -> f.getEventsIds().stream())
            .collect(Collectors.toSet());

        // Standalone events only — festival sub-events are shown inside their festival pages
        List<EventResponse> standAloneEvents = safeEvents.stream()
            .filter(e -> e.getId() == null || !festivalEventIds.contains(e.getId()))
            .toList();

        if (standAloneEvents.isEmpty() && safeFestivals.isEmpty()) {
            telegramClient.sendHtml(session.getChatId(),
                i18n.t(lang, "events_list_title") + "\n\n" + i18n.t(lang, "events_list_empty"),
                homeButton(lang));
            return StepResult.finish();
        }

        var rows = new ArrayList<List<Map<String, String>>>();

        // Pinned festivals first
        safeFestivals.stream()
            .filter(f -> Boolean.TRUE.equals(f.getIsPin()))
            .limit(MAX_FESTIVALS)
            .forEach(f -> rows.add(KeyboardBuilder.row(rawBtn("🎪 " + shortLabel(f.getName()), "evf_" + f.getId()))));

        // Standalone events
        int eventLimit = Math.min(standAloneEvents.size(), MAX_EVENTS);
        for (int i = 0; i < eventLimit; i++) {
            EventResponse e = standAloneEvents.get(i);
            rows.add(KeyboardBuilder.row(rawBtn(formatEventLabel(lang, e), "evt_" + e.getId())));
        }

        // Non-pinned festivals below events
        safeFestivals.stream()
            .filter(f -> !Boolean.TRUE.equals(f.getIsPin()))
            .limit(MAX_FESTIVALS)
            .forEach(f -> rows.add(KeyboardBuilder.row(rawBtn("🎪 " + shortLabel(f.getName()), "evf_" + f.getId()))));

        // Navigation
        rows.add(KeyboardBuilder.row(
            KeyboardBuilder.cbCmd(i18n.t(lang, "events_list_btn_myevents"), "myevents"),
            KeyboardBuilder.cbCmd(i18n.t(lang, "events_list_btn_home"), "/start")
        ));

        telegramClient.sendHtml(session.getChatId(),
            i18n.t(lang, "events_list_title"), KeyboardBuilder.inline(rows));

        return StepResult.stay(EventsListFlowDef.FLOW, EventsListFlowDef.STEP_LIST);
    }

    // ---- private helpers ----

    private List<EventResponse> fetchEvents() {
        try {
            return eventApi.eventsV1Get();
        } catch (Exception e) {
            LOG.warnf(e, "Failed to fetch events");
            return null;
        }
    }

    private List<FestivalResponse> fetchFestivals() {
        try {
            return festivalsApi.listFestivals();
        } catch (Exception e) {
            LOG.warnf(e, "Failed to fetch festivals");
            return null;
        }
    }

    private String formatEventLabel(String lang, EventResponse e) {
        String name = shortLabel(e.getName());
        if (e.getDateStart() != null) {
            boolean isRu = "ru".equals(lang);
            Locale locale = isRu ? Locale.of("ru") : Locale.ENGLISH;
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern(
                isRu ? "d MMM, HH:mm" : "MMM d, HH:mm", locale);
            return "📅 " + name + " — " + e.getDateStart().format(fmt);
        }
        return "📅 " + name;
    }

    private Object homeButton(String lang) {
        return KeyboardBuilder.inline(List.of(
            KeyboardBuilder.row(KeyboardBuilder.cbCmd(i18n.t(lang, "events_list_btn_home"), "/start"))
        ));
    }

    private static String shortLabel(String text) {
        if (text == null) return "—";
        return text.length() > 40 ? text.substring(0, 38) + "…" : text;
    }

    /**
     * Creates a raw inline button (no "/" prefix in callback_data).
     * Used for dynamic callbacks like evt_<uuid> that are routed by CommandRouter prefix matching.
     */
    static Map<String, String> rawBtn(String text, String callbackData) {
        return Map.of("text", text, "callback_data", callbackData);
    }
}
