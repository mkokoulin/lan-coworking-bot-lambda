package com.lan.app.flows.eventconfirm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.eventchange.EventChangeFlowDef;
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
import java.util.List;

@ApplicationScoped
public class EventConfirmHandler implements StepHandler {

    private static final Logger log = Logger.getLogger(EventConfirmHandler.class);

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @ConfigProperty(name = "app.site-url", defaultValue = "")
    String siteUrl;

    @ConfigProperty(name = "app.backend-url", defaultValue = "")
    String backendUrl;

    @Inject
    public EventConfirmHandler(TelegramClient telegramClient, I18n i18n) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String args = ctx.commandArgs();
        String regId = null;

        if (args != null && args.startsWith("reg_")) {
            // Format: reg_<uuid>_<lang>
            String payload = args.substring("reg_".length());
            String[] parts = payload.split("_", 2);
            regId = parts[0];
            if (parts.length > 1 && !parts[1].isBlank()) {
                session.setLang(parts[1]);
            }
        }

        String lang = session.getLang();

        String eventName = regId != null ? notifyBackend(regId, session.getChatId()) : null;

        String confirmMessage = (eventName != null && !eventName.isBlank())
                ? i18n.t(lang, "event_confirm_message_named").formatted(escapeHtml(eventName))
                : i18n.t(lang, "event_confirm_message");

        telegramClient.sendHtml(session.getChatId(), confirmMessage, null);

        var kbBuilder = new java.util.ArrayList<List<java.util.Map<String, String>>>();
        kbBuilder.add(KeyboardBuilder.row(
            KeyboardBuilder.cbCmd(i18n.t(lang, "event_confirm_btn_start"), "start")
        ));

        if (regId != null && siteUrl.startsWith("https://")) {
            String url = siteUrl + "/registration/" + regId;
            kbBuilder.add(KeyboardBuilder.row(
                KeyboardBuilder.urlBtn(i18n.t(lang, "event_confirm_btn_site"), url)
            ));
        }

        if (regId != null) {
            kbBuilder.add(KeyboardBuilder.row(
                KeyboardBuilder.rawBtn(i18n.t(lang, "event_confirm_btn_change"), EventChangeFlowDef.CB_MENU_PREFIX + regId)
            ));
        }

        telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "event_confirm_next"),
                KeyboardBuilder.inline(kbBuilder));

        return StepResult.finish();
    }

    /** Confirms the registration with the backend and returns the event name, if available. */
    private String notifyBackend(String regId, Long chatId) {
        String url = resolveBackendUrl(regId);
        if (url == null) return null;
        if (chatId != null) {
            url = url + "?chatId=" + chatId;
        }
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                log.warnf("Backend confirm returned %d for reg %s: %s", res.statusCode(), regId, res.body());
                return null;
            }
            JsonNode node = mapper.readTree(res.body());
            JsonNode eventName = node.get("event_name");
            return eventName != null && !eventName.isNull() ? eventName.asText() : null;
        } catch (Exception e) {
            log.warnf("Failed to notify backend for reg %s: %s", regId, e.getMessage());
            return null;
        }
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String resolveBackendUrl(String regId) {
        if (!backendUrl.isBlank()) {
            return backendUrl + "/events/v1/registrations/" + regId + "/confirm";
        }
        if (!siteUrl.isBlank()) {
            return siteUrl + "/api/registration/" + regId + "/confirm";
        }
        return null;
    }
}
