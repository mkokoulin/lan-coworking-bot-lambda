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

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class MyEventsHandler implements StepHandler {

    private static final Logger log = Logger.getLogger(MyEventsHandler.class);

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

        var sb = new StringBuilder();
        sb.append(i18n.t(lang, "myevents_title")).append("\n\n");

        for (int i = 0; i < registrations.size(); i++) {
            BotRegistrationDto r = registrations.get(i);
            sb.append(i + 1).append(". <b>").append(escapeHtml(r.getEventName())).append("</b>\n");
            if (r.getDateStart() != null) {
                sb.append("   📆 ").append(r.getDateStart().format(fmt)).append("\n");
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
