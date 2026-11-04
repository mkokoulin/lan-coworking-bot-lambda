package com.lan.app.flows.eventsurvey;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.flows.eventsurvey.dto.EventSurveyDueDto;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.session.SessionRepository;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.ui.KeyboardBuilder;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * Polls the backend for guests due to receive the day-after-event feedback survey (see
 * BotResource#dueEventSurveys on lan-baserow-api-lambda — only guests who confirmed attendance
 * via the reminder flow, one day after the event). Sends a 1-5 rating prompt whose buttons carry
 * all the identifying ids in their callback_data, since this send doesn't touch session state —
 * the guest may be anywhere else in the bot when it arrives (same pattern as EventReminderScheduler).
 */
@ApplicationScoped
public class EventSurveyScheduler {

    private static final Logger log = Logger.getLogger(EventSurveyScheduler.class);

    @ConfigProperty(name = "app.backend-url", defaultValue = "")
    String backendUrl;

    private final TelegramClient telegramClient;
    private final SessionRepository sessionRepository;
    private final I18n i18n;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Inject
    public EventSurveyScheduler(TelegramClient telegramClient, SessionRepository sessionRepository, I18n i18n) {
        this.telegramClient = telegramClient;
        this.sessionRepository = sessionRepository;
        this.i18n = i18n;
    }

    @Scheduled(every = "5m")
    void pollAndSend() {
        if (backendUrl.isBlank()) {
            log.debug("app.backend-url not set, skipping event survey poll");
            return;
        }

        List<EventSurveyDueDto> due;
        try {
            due = fetchDue();
        } catch (Exception e) {
            log.warnf("Failed to fetch due event surveys: %s", e.getMessage());
            return;
        }
        if (due.isEmpty()) return;

        log.infof("Sending %d due event survey(s)", due.size());
        for (EventSurveyDueDto recipient : due) {
            sendOne(recipient);
        }
    }

    private void sendOne(EventSurveyDueDto recipient) {
        if (recipient.chatId == null) return;

        String lang = sessionRepository.findByUserId(recipient.chatId)
                .map(Session::getLang)
                .orElse("ru");

        String suffix = "_" + recipient.eventRowId + "_" + recipient.guestRowId + "_" + recipient.registrationRowId;
        var buttons = new java.util.ArrayList<Map<String, String>>();
        for (int rating = 1; rating <= 5; rating++) {
            buttons.add(KeyboardBuilder.rawBtn(
                i18n.t(lang, "review_btn_rate_" + rating),
                EventSurveyFlowDef.CB_SURVEY_RATE_PREFIX + rating + suffix
            ));
        }
        var keyboard = KeyboardBuilder.inline(List.of(buttons));

        String body = String.format(i18n.t(lang, "event_survey_prompt"), recipient.eventName);

        try {
            telegramClient.sendHtml(recipient.chatId, body, keyboard);
        } catch (Exception e) {
            log.warnf("Failed to send event survey to chatId=%d: %s", recipient.chatId, e.getMessage());
        }
    }

    private List<EventSurveyDueDto> fetchDue() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(backendUrl + "/events/v1/bot/event-surveys/due"))
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("event-surveys/due endpoint returned " + resp.statusCode());
        }
        EventSurveyDueDto[] arr = mapper.readValue(resp.body(), EventSurveyDueDto[].class);
        return List.of(arr);
    }
}
