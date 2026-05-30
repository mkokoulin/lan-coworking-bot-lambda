package com.lan.app.flows.cwbooking;

import com.lan.app.config.TelegramConfig;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class CwBookingConfirmHandler implements StepHandler {

    private static final Logger log = Logger.getLogger(CwBookingConfirmHandler.class);

    private final TelegramClient telegramClient;
    private final TelegramConfig telegramConfig;
    private final I18n i18n;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @ConfigProperty(name = "app.site-url", defaultValue = "")
    String siteUrl;

    @Inject
    public CwBookingConfirmHandler(TelegramClient telegramClient, TelegramConfig telegramConfig, I18n i18n) {
        this.telegramClient = telegramClient;
        this.telegramConfig = telegramConfig;
        this.i18n = i18n;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String args = ctx.commandArgs();
        String bookingId = null;

        if (args != null && args.startsWith("cwbooking_")) {
            bookingId = args.substring("cwbooking_".length());
        }

        String lang = session.getLang();

        if (bookingId != null) {
            confirmOnSite(bookingId);
        }

        telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "cwbooking_confirm_message"), null);

        notifyAdmin(bookingId, session.getChatId());

        var kbRows = new ArrayList<List<Map<String, String>>>();
        kbRows.add(KeyboardBuilder.row(
            KeyboardBuilder.cbCmd(i18n.t(lang, "cwbooking_btn_home"), "start")
        ));
        telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "cwbooking_confirm_next"),
                KeyboardBuilder.inline(kbRows));

        return StepResult.finish();
    }

    private void confirmOnSite(String bookingId) {
        if (siteUrl.isBlank()) return;
        String url = siteUrl + "/api/coworking/booking/" + bookingId + "/confirm";
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(res -> {
                        if (res.statusCode() != 200) {
                            log.warnf("Booking confirm returned %d for %s: %s",
                                    res.statusCode(), bookingId, res.body());
                        }
                    })
                    .exceptionally(ex -> {
                        log.warnf("Failed to confirm booking %s: %s", bookingId, ex.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            log.warnf("Failed to build confirm request for booking %s: %s", bookingId, e.getMessage());
        }
    }

    private void notifyAdmin(String bookingId, Long chatId) {
        Long adminId = telegramConfig.adminChatId();
        if (adminId == null) return;
        try {
            String text = "✅ <b>Бронирование коворкинга подтверждено</b>\n\n"
                    + "👤 chat_id: " + chatId + "\n"
                    + "🆔 Заявка: " + (bookingId != null ? bookingId : "—");
            telegramClient.sendHtml(adminId, text, null);
        } catch (Exception e) {
            log.warnf("Failed to notify admin about booking %s: %s", bookingId, e.getMessage());
        }
    }
}
