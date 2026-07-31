package com.lan.app.flows.myevents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.myevents.dto.RegistrationActionDto;
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

/** Handles the "❌ Отменить" flow: confirm prompt (me_c_), then yes (me_y_) / no (me_n_). */
@ApplicationScoped
public class MyEventsCancelActionHandler implements StepHandler {

    private static final Logger log = Logger.getLogger(MyEventsCancelActionHandler.class);

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final MyEventsListHandler listHandler;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @ConfigProperty(name = "app.backend-url", defaultValue = "")
    String backendUrl;

    @ConfigProperty(name = "telegram.admin-chat-id")
    Long adminChatId;

    @Inject
    public MyEventsCancelActionHandler(TelegramClient telegramClient, I18n i18n, MyEventsListHandler listHandler) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.listHandler = listHandler;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();
        String raw = ctx.command();
        if (raw == null) return StepResult.finish();

        if (raw.startsWith(MyEventsFlowDef.CB_CANCEL_YES_PFX)) {
            String regId = raw.substring(MyEventsFlowDef.CB_CANCEL_YES_PFX.length());
            return doCancel(ctx, session, regId);
        }
        if (raw.startsWith(MyEventsFlowDef.CB_CANCEL_NO_PFX)) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "my_events_cancel_aborted"), null);
            return listHandler.handle(ctx, session);
        }
        if (raw.startsWith(MyEventsFlowDef.CB_CANCEL_PFX)) {
            String regId = raw.substring(MyEventsFlowDef.CB_CANCEL_PFX.length());
            var kb = KeyboardBuilder.inline(List.of(KeyboardBuilder.row(
                    KeyboardBuilder.cbCmd(i18n.t(lang, "my_events_btn_yes"), MyEventsFlowDef.CB_CANCEL_YES_PFX + regId),
                    KeyboardBuilder.cbCmd(i18n.t(lang, "my_events_btn_no"), MyEventsFlowDef.CB_CANCEL_NO_PFX + regId)
            )));
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "my_events_cancel_confirm"), kb);
            return StepResult.stay(MyEventsFlowDef.FLOW, MyEventsFlowDef.STEP_CANCEL_ACTION);
        }
        return StepResult.finish();
    }

    private StepResult doCancel(UpdateContext ctx, Session session, String regId) {
        String lang = session.getLang();
        if (!backendUrl.isBlank()) {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(backendUrl + "/events/v1/bot/registrations/" + regId + "/cancel"))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    var result = mapper.readValue(resp.body(), RegistrationActionDto.class);
                    telegramClient.sendHtml(session.getChatId(),
                            i18n.t(lang, "my_events_cancel_success")
                                    .formatted(result.eventName, MyEventsSession.formatDate(result.dateStart)),
                            null);
                    telegramClient.sendHtml(adminChatId, buildAdminMessage(result), null);
                } else if (resp.statusCode() == 409) {
                    telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "my_events_cancel_conflict"), null);
                } else {
                    log.warnf("cancel endpoint returned %d for regId=%s", resp.statusCode(), regId);
                    telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "my_events_error"), null);
                }
            } catch (Exception e) {
                log.warnf("Failed to cancel registration %s: %s", regId, e.getMessage());
                telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "my_events_error"), null);
            }
        }
        return listHandler.handle(ctx, session);
    }

    private String buildAdminMessage(RegistrationActionDto r) {
        StringBuilder sb = new StringBuilder();
        sb.append("❌ <b>Отмена регистрации</b>\n\n");
        sb.append("🎪 ").append(r.eventName).append("\n");
        sb.append("📅 ").append(MyEventsSession.formatDate(r.dateStart)).append("\n");
        sb.append("👤 ").append(safe(r.guestFirstName)).append(" ").append(safe(r.guestLastName)).append("\n");
        if (r.guestPhone != null && !r.guestPhone.isBlank()) {
            sb.append("📞 ").append(r.guestPhone).append("\n");
        }
        if (r.guestTelegram != null && !r.guestTelegram.isBlank()) {
            sb.append("✈️ ").append(r.guestTelegram).append("\n");
        }
        sb.append("👥 ").append(r.guestCount).append(" ").append(MyEventsSession.guestsLabel(r.guestCount));
        return sb.toString();
    }

    private String safe(String s) {
        return s != null ? s : "";
    }
}
