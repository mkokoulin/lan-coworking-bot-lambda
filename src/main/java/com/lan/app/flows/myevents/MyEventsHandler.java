package com.lan.app.flows.myevents;

import com.lan.app.client.baserow.api.BotApi;
import com.lan.app.client.baserow.model.BotRegistrationDto;
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

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ApplicationScoped
public class MyEventsHandler implements StepHandler {

    private static final Logger log = Logger.getLogger(MyEventsHandler.class);
    private static final int HISTORY_LIMIT = 15;

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final BotApi botApi;

    @Inject
    public MyEventsHandler(TelegramClient telegramClient, I18n i18n, @RestClient BotApi botApi) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.botApi = botApi;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        List<BotRegistrationDto> registrations = fetchRegistrations(session.getChatId());

        String message;
        Object keyboard;
        if (registrations == null) {
            message = i18n.t(lang, "myevents_error");
            keyboard = homeButton(lang);
        } else if (registrations.isEmpty()) {
            message = i18n.t(lang, "myevents_title") + "\n\n" + i18n.t(lang, "myevents_empty");
            keyboard = homeButton(lang);
        } else {
            message = buildMessage(lang, registrations);
            keyboard = buildKeyboard(lang, registrations);
        }

        telegramClient.sendHtml(session.getChatId(), message, keyboard);
        return StepResult.finish();
    }

    private List<BotRegistrationDto> fetchRegistrations(Long chatId) {
        try {
            return botApi.botMyRegistrations(chatId);
        } catch (Exception e) {
            log.warnf("Failed to fetch registrations for chatId %d: %s", chatId, e.getMessage());
            return null;
        }
    }

    private String buildMessage(String lang, List<BotRegistrationDto> registrations) {
        boolean isRu = "ru".equals(lang);
        Locale locale = isRu ? Locale.of("ru") : Locale.ENGLISH;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(
                isRu ? "d MMMM yyyy, HH:mm" : "MMMM d, yyyy, HH:mm", locale);

        OffsetDateTime now = OffsetDateTime.now();
        List<BotRegistrationDto> active = new ArrayList<>();
        List<BotRegistrationDto> history = new ArrayList<>();
        for (var r : registrations) {
            boolean isCancelled = Boolean.TRUE.equals(r.getIsCancelled());
            boolean isPast = r.getDateStart() != null && !r.getDateStart().isAfter(now);
            if (!isCancelled && !isPast) {
                active.add(r);
            } else {
                history.add(r);
            }
        }
        active.sort(Comparator.comparing(BotRegistrationDto::getDateStart,
                Comparator.nullsLast(Comparator.naturalOrder())));
        history.sort(Comparator.comparing(BotRegistrationDto::getDateStart,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        if (history.size() > HISTORY_LIMIT) {
            history = history.subList(0, HISTORY_LIMIT);
        }

        var sb = new StringBuilder();
        sb.append(i18n.t(lang, "myevents_title")).append("\n\n");

        if (!active.isEmpty()) {
            sb.append(i18n.t(lang, "myevents_active_header")).append("\n\n");
            for (int i = 0; i < active.size(); i++) {
                appendItem(sb, lang, i + 1, active.get(i), fmt);
            }
        }
        if (!history.isEmpty()) {
            sb.append(i18n.t(lang, "myevents_history_header")).append("\n\n");
            for (var r : history) {
                String mark = Boolean.TRUE.equals(r.getIsCancelled())
                        ? "❌ " + i18n.t(lang, "myevents_history_cancelled")
                        : "✅";
                sb.append(mark).append(" <b>").append(escapeHtml(r.getEventName())).append("</b>");
                if (r.getDateStart() != null) {
                    sb.append(" — ").append(r.getDateStart().format(fmt));
                }
                sb.append("\n");
            }
        }

        return sb.toString().trim();
    }

    private void appendItem(StringBuilder sb, String lang, int index, BotRegistrationDto r, DateTimeFormatter fmt) {
        sb.append(index).append(". <b>").append(escapeHtml(r.getEventName())).append("</b>\n");
        if (r.getDateStart() != null) {
            sb.append("   📆 ").append(r.getDateStart().format(fmt)).append("\n");
        }
        if (r.getGuestCount() != null) {
            sb.append("   👥 ").append(r.getGuestCount()).append(" ")
                    .append(guestsLabel(lang, r.getGuestCount())).append("\n");
        }
        sb.append("\n");
    }

    private Object buildKeyboard(String lang, List<BotRegistrationDto> registrations) {
        List<List<Map<String, String>>> rows = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();
        for (var r : registrations) {
            boolean isCancelled = Boolean.TRUE.equals(r.getIsCancelled());
            boolean isPast = r.getDateStart() != null && !r.getDateStart().isAfter(now);
            if (isCancelled || isPast) continue;
            String regId = r.getRegistrationId();
            if (regId == null || regId.isBlank()) continue;
            rows.add(KeyboardBuilder.row(
                    KeyboardBuilder.rawBtn(i18n.t(lang, "myevents_btn_cancel"), MyEventsFlowDef.CB_CANCEL_PFX + regId),
                    KeyboardBuilder.rawBtn(i18n.t(lang, "myevents_btn_guests"), MyEventsFlowDef.CB_GUEST_COUNT_PFX + regId)
            ));
        }
        rows.add(KeyboardBuilder.row(KeyboardBuilder.cbCmd(i18n.t(lang, "myevents_btn_home"), "start")));
        return KeyboardBuilder.inline(rows);
    }

    private Object homeButton(String lang) {
        return KeyboardBuilder.inline(List.of(
                KeyboardBuilder.row(
                        KeyboardBuilder.cbCmd(i18n.t(lang, "myevents_btn_home"), "start")
                )
        ));
    }

    /** Russian plural form for "гость": 1 гость, 2-4 гостя, 5-20/0 гостей (11-14 always "гостей"). English is unpluralized. */
    private static String guestsLabel(String lang, int n) {
        if (!"ru".equals(lang)) return "guests";
        int mod10 = n % 10;
        int mod100 = n % 100;
        if (mod10 == 1 && mod100 != 11) return "гость";
        if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return "гостя";
        return "гостей";
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
