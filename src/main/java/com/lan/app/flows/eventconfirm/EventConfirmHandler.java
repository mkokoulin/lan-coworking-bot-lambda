package com.lan.app.flows.eventconfirm;

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
import java.util.List;

@ApplicationScoped
public class EventConfirmHandler implements StepHandler {

    private static final Logger log = Logger.getLogger(EventConfirmHandler.class);

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final HttpClient httpClient = HttpClient.newHttpClient();

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
            // Format: reg_<uuid>_<lang>  or  reg_<uuid>
            String payload = args.substring("reg_".length());
            String[] parts = payload.split("_", 2);
            regId = parts[0];
            if (parts.length > 1 && !parts[1].isBlank()) {
                session.setLang(parts[1]);
            }
        }

        String lang = session.getLang();

        if (regId != null) {
            notifyBackend(regId);
        }

        telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "event_confirm_message"), null);

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

        telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "event_confirm_next"),
                KeyboardBuilder.inline(kbBuilder));

        return StepResult.finish();
    }

    private void notifyBackend(String regId) {
        String url = resolveBackendUrl(regId);
        if (url == null) return;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            httpClient.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(res -> {
                        if (res.statusCode() != 200) {
                            log.warnf("Backend confirm returned %d for reg %s", res.statusCode(), regId);
                        }
                    })
                    .exceptionally(ex -> {
                        log.warnf("Failed to notify backend for reg %s: %s", regId, ex.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            log.warnf("Failed to build backend confirm request for reg %s: %s", regId, e.getMessage());
        }
    }

    private String resolveBackendUrl(String regId) {
        if (!backendUrl.isBlank()) {
            return backendUrl + "/events/registrations/" + regId + "/confirm";
        }
        if (!siteUrl.isBlank()) {
            return siteUrl + "/api/registration/" + regId + "/confirm";
        }
        return null;
    }
}
