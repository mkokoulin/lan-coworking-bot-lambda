package com.lan.app.flows.myevents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.myevents.dto.MyRegistrationDto;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.ui.KeyboardBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Renders "Мои мероприятия": active (upcoming, not cancelled) registrations with action buttons, plus a history section. */
@ApplicationScoped
public class MyEventsListHandler implements StepHandler {

    private static final Logger log = Logger.getLogger(MyEventsListHandler.class);
    private static final int HISTORY_LIMIT = 15;

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @ConfigProperty(name = "app.backend-url", defaultValue = "")
    String backendUrl;

    @Inject
    public MyEventsListHandler(TelegramClient telegramClient, I18n i18n) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();
        List<MyRegistrationDto> all = fetchMyRegistrations(session.getChatId());

        Instant now = Instant.now();
        List<MyRegistrationDto> active = new ArrayList<>();
        List<MyRegistrationDto> history = new ArrayList<>();
        for (var r : all) {
            Instant dateStart = MyEventsSession.parseInstant(r.dateStart);
            boolean isPast = dateStart == null || !dateStart.isAfter(now);
            if (!r.isCancelled && !isPast) {
                active.add(r);
            } else {
                history.add(r);
            }
        }
        active.sort(Comparator.comparing((MyRegistrationDto r) -> MyEventsSession.parseInstant(r.dateStart),
                Comparator.nullsLast(Comparator.naturalOrder())));
        history.sort(Comparator.comparing((MyRegistrationDto r) -> MyEventsSession.parseInstant(r.dateStart),
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        if (history.size() > HISTORY_LIMIT) {
            history = history.subList(0, HISTORY_LIMIT);
        }

        List<List<Map<String, String>>> kbRows = new ArrayList<>();
        StringBuilder text = new StringBuilder();

        if (active.isEmpty() && history.isEmpty()) {
            text.append(i18n.t(lang, "my_events_empty"));
        } else {
            if (!active.isEmpty()) {
                text.append(i18n.t(lang, "my_events_active_header")).append("\n\n");
                for (var r : active) {
                    text.append("🎪 <b>").append(r.eventName).append("</b>\n")
                            .append("📅 ").append(MyEventsSession.formatDate(r.dateStart)).append("\n")
                            .append("👥 ").append(r.guestCount).append(" ")
                            .append(MyEventsSession.guestsLabel(r.guestCount)).append("\n\n");
                    kbRows.add(KeyboardBuilder.row(
                            KeyboardBuilder.cbCmd(i18n.t(lang, "my_events_btn_cancel"),
                                    MyEventsFlowDef.CB_CANCEL_PFX + r.registrationId),
                            KeyboardBuilder.cbCmd(i18n.t(lang, "my_events_btn_guests"),
                                    MyEventsFlowDef.CB_GUESTS_PFX + r.registrationId)
                    ));
                }
            }
            if (!history.isEmpty()) {
                text.append(i18n.t(lang, "my_events_history_header")).append("\n\n");
                for (var r : history) {
                    String mark = r.isCancelled ? "❌ " + i18n.t(lang, "my_events_history_cancelled") : "✅";
                    text.append(mark).append(" ").append(r.eventName)
                            .append(" — ").append(MyEventsSession.formatDate(r.dateStart)).append("\n");
                }
            }
        }

        kbRows.add(KeyboardBuilder.row(KeyboardBuilder.cbCmd(i18n.t(lang, "event_confirm_btn_start"), "start")));

        telegramClient.sendHtml(session.getChatId(), text.toString().trim(), KeyboardBuilder.inline(kbRows));
        return StepResult.stay(MyEventsFlowDef.FLOW, MyEventsFlowDef.STEP_LIST);
    }

    private List<MyRegistrationDto> fetchMyRegistrations(Long chatId) {
        if (backendUrl.isBlank() || chatId == null) return List.of();
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(backendUrl + "/events/v1/bot/my-registrations?chatId=" + chatId))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.warnf("my-registrations endpoint returned %d for chatId=%d", resp.statusCode(), chatId);
                return List.of();
            }
            MyRegistrationDto[] arr = mapper.readValue(resp.body(), MyRegistrationDto[].class);
            return List.of(arr);
        } catch (Exception e) {
            log.warnf("Failed to fetch my-registrations for chatId=%d: %s", chatId, e.getMessage());
            return List.of();
        }
    }
}
