package com.lan.app.flows.heardabout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.flows.heardabout.dto.HeardAboutSourceDueDto;
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

/**
 * Polls the backend for guests due for the "how did you hear about us?" survey (see
 * BotResource#dueHeardAboutSource on lan-baserow-api-lambda — 24h after the guest's row was
 * created, gated to coworking working hours) and sends the initial source-choice message.
 * Idempotency and the working-hours gate are entirely backend-side; this scheduler just delivers
 * whatever /due returns.
 */
@ApplicationScoped
public class HeardAboutScheduler {

    private static final Logger log = Logger.getLogger(HeardAboutScheduler.class);

    @ConfigProperty(name = "app.backend-url", defaultValue = "")
    String backendUrl;

    private final TelegramClient telegramClient;
    private final SessionRepository sessionRepository;
    private final I18n i18n;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Inject
    public HeardAboutScheduler(TelegramClient telegramClient, SessionRepository sessionRepository, I18n i18n) {
        this.telegramClient = telegramClient;
        this.sessionRepository = sessionRepository;
        this.i18n = i18n;
    }

    @Scheduled(every = "5m")
    void pollAndSend() {
        if (backendUrl.isBlank()) {
            log.debug("app.backend-url not set, skipping heard-about-source poll");
            return;
        }

        List<HeardAboutSourceDueDto> due;
        try {
            due = fetchDue();
        } catch (Exception e) {
            log.warnf("Failed to fetch due heard-about-source recipients: %s", e.getMessage());
            return;
        }
        if (due.isEmpty()) return;

        log.infof("Sending %d due heard-about-source survey(s)", due.size());
        for (HeardAboutSourceDueDto recipient : due) {
            sendOne(recipient);
        }
    }

    private void sendOne(HeardAboutSourceDueDto recipient) {
        if (recipient.chatId == null) return;

        String lang = sessionRepository.findByUserId(recipient.chatId)
                .map(Session::getLang)
                .orElse("ru");

        var keyboard = KeyboardBuilder.inline(List.of(
            KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "heard_about_btn_instagram"), HeardAboutFlowDef.PREFIX_INSTAGRAM + recipient.guestRowId),
                KeyboardBuilder.cbCmd(i18n.t(lang, "heard_about_btn_google"), HeardAboutFlowDef.PREFIX_GOOGLE + recipient.guestRowId)
            ),
            KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "heard_about_btn_friends"), HeardAboutFlowDef.PREFIX_FRIENDS + recipient.guestRowId),
                KeyboardBuilder.cbCmd(i18n.t(lang, "heard_about_btn_other"), HeardAboutFlowDef.PREFIX_OTHER + recipient.guestRowId)
            )
        ));

        try {
            telegramClient.sendHtml(recipient.chatId, i18n.t(lang, "heard_about_ask"), keyboard);
        } catch (Exception e) {
            log.warnf("Failed to send heard-about-source survey to chatId=%d: %s", recipient.chatId, e.getMessage());
        }
    }

    private List<HeardAboutSourceDueDto> fetchDue() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(backendUrl + "/events/v1/bot/heard-about-source/due"))
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("heard-about-source/due endpoint returned " + resp.statusCode());
        }
        HeardAboutSourceDueDto[] arr = mapper.readValue(resp.body(), HeardAboutSourceDueDto[].class);
        return List.of(arr);
    }
}
