package com.lan.app.flows.weeklydigest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.client.baserow.api.EventResourceApi;
import com.lan.app.client.baserow.api.FestivalsApi;
import com.lan.app.client.baserow.model.EventResponse;
import com.lan.app.client.baserow.model.FestivalResponse;
import com.lan.app.flows.weeklydigest.dto.DigestSubscriberDto;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.session.SessionRepository;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.ui.KeyboardBuilder;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Sends a weekly digest of the coming week's events to every guest opted in on the backend
 * (see BotResource#weeklyDigestSubscribers on lan-baserow-api-lambda — opt-out model, a guest
 * with no preference set is included). Skips the send entirely when there are no events in the
 * window, rather than mailing an empty/low-value digest.
 */
@ApplicationScoped
public class WeeklyDigestScheduler {

    private static final Logger log = Logger.getLogger(WeeklyDigestScheduler.class);

    @ConfigProperty(name = "app.backend-url", defaultValue = "")
    String backendUrl;

    private final TelegramClient telegramClient;
    private final SessionRepository sessionRepository;
    private final I18n i18n;
    private final EventResourceApi eventApi;
    private final FestivalsApi festivalsApi;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Inject
    public WeeklyDigestScheduler(
        TelegramClient telegramClient,
        SessionRepository sessionRepository,
        I18n i18n,
        @RestClient EventResourceApi eventApi,
        @RestClient FestivalsApi festivalsApi
    ) {
        this.telegramClient = telegramClient;
        this.sessionRepository = sessionRepository;
        this.i18n = i18n;
        this.eventApi = eventApi;
        this.festivalsApi = festivalsApi;
    }

    @Scheduled(cron = "{app.weekly-digest.cron}", timeZone = "Asia/Yerevan")
    void sendDigest() {
        if (backendUrl.isBlank()) {
            log.debug("app.backend-url not set, skipping weekly digest");
            return;
        }

        List<EventResponse> weekEvents = fetchWeekEvents();
        if (weekEvents.isEmpty()) {
            log.info("No events this week, skipping weekly digest send");
            return;
        }

        List<DigestSubscriberDto> subscribers;
        try {
            subscribers = fetchSubscribers();
        } catch (Exception e) {
            log.warnf("Failed to fetch weekly digest subscribers: %s", e.getMessage());
            return;
        }
        if (subscribers.isEmpty()) return;

        String bodyRu = buildDigestBody("ru", weekEvents);
        String bodyEn = buildDigestBody("en", weekEvents);

        log.infof("Sending weekly digest (%d event(s)) to %d subscriber(s)", weekEvents.size(), subscribers.size());
        for (DigestSubscriberDto subscriber : subscribers) {
            if (subscriber.chatId == null) continue;

            String lang = sessionRepository.findByUserId(subscriber.chatId)
                    .map(Session::getLang)
                    .orElse("ru");
            String body = "ru".equals(lang) ? bodyRu : bodyEn;

            var keyboard = KeyboardBuilder.inline(List.of(KeyboardBuilder.row(
                    KeyboardBuilder.rawBtn(i18n.t(lang, "weekly_digest_btn_unsubscribe"), "digest_unsub_" + subscriber.guestRowId)
            )));

            try {
                telegramClient.sendHtml(subscriber.chatId, body, keyboard);
            } catch (Exception e) {
                log.warnf("Failed to send weekly digest to chatId=%d: %s", subscriber.chatId, e.getMessage());
            }
        }
    }

    // Standalone events starting within the next 7 days — excludes festival sub-events, mirroring
    // EventsListHandler's filtering (festival events are shown on their festival's own page, not
    // in the flat event list/digest).
    private List<EventResponse> fetchWeekEvents() {
        List<EventResponse> events;
        List<FestivalResponse> festivals;
        try {
            events = eventApi.eventsV1Get();
        } catch (Exception e) {
            log.warnf("Failed to fetch events for weekly digest: %s", e.getMessage());
            return List.of();
        }
        try {
            festivals = festivalsApi.listFestivals();
        } catch (Exception e) {
            log.warnf("Failed to fetch festivals for weekly digest: %s", e.getMessage());
            festivals = List.of();
        }

        Set<UUID> festivalEventIds = festivals.stream()
            .filter(f -> f.getEventsIds() != null)
            .flatMap(f -> f.getEventsIds().stream())
            .collect(Collectors.toSet());

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime weekEnd = now.plusDays(7);

        return events.stream()
            .filter(e -> e.getId() == null || !festivalEventIds.contains(e.getId()))
            .filter(e -> e.getDateStart() != null
                && e.getDateStart().isAfter(now)
                && e.getDateStart().isBefore(weekEnd))
            .sorted(Comparator.comparing(EventResponse::getDateStart))
            .toList();
    }

    private String buildDigestBody(String lang, List<EventResponse> events) {
        boolean isRu = "ru".equals(lang);
        Locale locale = isRu ? Locale.of("ru") : Locale.ENGLISH;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(isRu ? "d MMM, HH:mm" : "MMM d, HH:mm", locale);

        StringBuilder sb = new StringBuilder(i18n.t(lang, "weekly_digest_header"));
        for (EventResponse e : events) {
            sb.append("\n").append(String.format(
                i18n.t(lang, "weekly_digest_item"),
                e.getName(),
                e.getDateStart().format(fmt)
            ));
        }
        return sb.toString();
    }

    private List<DigestSubscriberDto> fetchSubscribers() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(backendUrl + "/events/v1/bot/weekly-digest/subscribers"))
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("weekly-digest/subscribers endpoint returned " + resp.statusCode());
        }
        DigestSubscriberDto[] arr = mapper.readValue(resp.body(), DigestSubscriberDto[].class);
        return List.of(arr);
    }
}
