package com.lan.app.flows.cwbooking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static final Map<String, String> TARIFF_LABELS = Map.of(
        "1h",    "1 час — 1 300 ֏",
        "4h",    "4 часа — 3 000 ֏",
        "day",   "1 день — 5 000 ֏",
        "week",  "7 дней — 25 000 ֏",
        "month", "30 дней — 75 000 ֏"
    );

    private final TelegramClient telegramClient;
    private final TelegramConfig telegramConfig;
    private final I18n i18n;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

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

        // Direct button click (no deep link) — show booking info with site link
        if (bookingId == null) {
            var kbRows = new ArrayList<List<Map<String, String>>>();
            if (!siteUrl.isBlank()) {
                kbRows.add(KeyboardBuilder.row(
                    KeyboardBuilder.urlBtn(i18n.t(lang, "booking_btn_site"), siteUrl + "/coworking")
                ));
            }
            kbRows.add(KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "booking_btn_home"), "/start")
            ));
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "booking_prompt"),
                    KeyboardBuilder.inline(kbRows));
            return StepResult.finish();
        }

        JsonNode bookingData = confirmOnSite(bookingId);

        boolean alreadyConfirmed = bookingData != null && bookingData.path("alreadyConfirmed").asBoolean(false);

        if (!alreadyConfirmed) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "cwbooking_confirm_message"), null);
            notifyAdmin(bookingId, ctx.username(), session.getChatId(), bookingData);
        }

        var kbRows = new ArrayList<List<Map<String, String>>>();
        kbRows.add(KeyboardBuilder.row(
            KeyboardBuilder.cbCmd(i18n.t(lang, "cwbooking_btn_home"), "start")
        ));
        telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "cwbooking_confirm_next"),
                KeyboardBuilder.inline(kbRows));

        return StepResult.finish();
    }

    private JsonNode confirmOnSite(String bookingId) {
        if (bookingId == null || siteUrl.isBlank()) return null;
        String url = siteUrl + "/api/coworking/booking/" + bookingId + "/confirm";
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                return mapper.readTree(resp.body());
            }
            log.warnf("Booking confirm returned %d for %s: %s", resp.statusCode(), bookingId, resp.body());
        } catch (Exception e) {
            log.warnf("Failed to confirm booking %s: %s", bookingId, e.getMessage());
        }
        return null;
    }

    private void notifyAdmin(String bookingId, String tgUsername, Long chatId, JsonNode data) {
        Long adminId = telegramConfig.adminChatId();
        if (adminId == null) return;

        StringBuilder sb = new StringBuilder("✅ <b>Бронирование коворкинга подтверждено</b>\n\n");

        if (data != null) {
            String firstName = textOrDash(data, "firstName");
            String phone     = textOrDash(data, "phone");
            String telegram  = data.path("telegram").asText("").trim();
            String tariffId  = data.path("tariffId").asText("").trim();
            String date      = textOrDash(data, "bookingDate");
            String start     = textOrDash(data, "startTime");
            String end       = textOrDash(data, "endTime");

            String tariffLabel = TARIFF_LABELS.getOrDefault(tariffId, tariffId);

            sb.append("👤 ").append(firstName).append("\n");
            sb.append("📞 ").append(phone).append("\n");
            if (!telegram.isEmpty()) {
                sb.append("✈️ ").append(telegram).append("\n");
            } else if (tgUsername != null && !tgUsername.isBlank()) {
                sb.append("✈️ @").append(tgUsername).append("\n");
            }
            sb.append("🗂 Тариф: ").append(tariffLabel).append("\n");
            sb.append("📅 ").append(date).append(" ").append(start).append("–").append(end);
        } else {
            if (tgUsername != null && !tgUsername.isBlank()) {
                sb.append("✈️ @").append(tgUsername).append("\n");
            }
            sb.append("💬 chat_id: ").append(chatId);
            if (bookingId != null) {
                sb.append("\n🆔 ").append(bookingId);
            }
        }

        try {
            telegramClient.sendHtml(adminId, sb.toString(), null);
        } catch (Exception e) {
            log.warnf("Failed to notify admin about booking %s: %s", bookingId, e.getMessage());
        }
    }

    private String textOrDash(JsonNode node, String field) {
        String val = node.path(field).asText("").trim();
        return val.isEmpty() ? "—" : val;
    }
}
