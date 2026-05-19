package com.lan.app.flows.myevents;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class MyEventsHandler implements StepHandler {

    private static final Logger log = Logger.getLogger(MyEventsHandler.class);

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final ObjectMapper mapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @ConfigProperty(name = "app.backend-url", defaultValue = "")
    String backendUrl;

    @Inject
    public MyEventsHandler(TelegramClient telegramClient, I18n i18n, ObjectMapper mapper) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.mapper = mapper;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        if (backendUrl.isBlank()) {
            telegramClient.sendHtml(session.getChatId(),
                    i18n.t(lang, "myevents_unavailable"),
                    homeButton(lang));
            return StepResult.finish();
        }

        List<MyRegistrationDto> registrations = fetchRegistrations(session.getChatId());

        String message;
        if (registrations == null) {
            message = i18n.t(lang, "myevents_error");
        } else if (registrations.isEmpty()) {
            message = i18n.t(lang, "myevents_title") + "\n\n" + i18n.t(lang, "myevents_empty");
        } else {
            message = buildMessage(lang, registrations);
        }

        telegramClient.sendHtml(session.getChatId(), message, homeButton(lang));
        return StepResult.finish();
    }

    private List<MyRegistrationDto> fetchRegistrations(Long chatId) {
        try {
            String url = backendUrl + "/bot/my-registrations?chatId=" + chatId;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warnf("my-registrations returned %d for chatId %d: %s",
                        response.statusCode(), chatId, response.body());
                return null;
            }
            return mapper.readValue(response.body(), new TypeReference<>() {});
        } catch (Exception e) {
            log.warnf("Failed to fetch registrations for chatId %d: %s", chatId, e.getMessage());
            return null;
        }
    }

    private String buildMessage(String lang, List<MyRegistrationDto> registrations) {
        boolean isRu = "ru".equals(lang);
        Locale locale = isRu ? Locale.of("ru") : Locale.ENGLISH;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(
                isRu ? "d MMMM yyyy, HH:mm" : "MMMM d, yyyy, HH:mm", locale);

        var sb = new StringBuilder();
        sb.append(i18n.t(lang, "myevents_title")).append("\n\n");

        for (int i = 0; i < registrations.size(); i++) {
            MyRegistrationDto r = registrations.get(i);
            sb.append(i + 1).append(". <b>").append(escapeHtml(r.eventName)).append("</b>\n");
            if (r.dateStart != null) {
                sb.append("   📆 ").append(r.dateStart.format(fmt)).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString().trim();
    }

    private Object homeButton(String lang) {
        return KeyboardBuilder.inline(List.of(
                KeyboardBuilder.row(
                        KeyboardBuilder.cbCmd(i18n.t(lang, "myevents_btn_home"), "start")
                )
        ));
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
