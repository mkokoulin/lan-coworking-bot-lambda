package com.lan.app.flows.eventslist;

import com.lan.app.client.baserow.api.EventRegistrationsApi;
import com.lan.app.client.baserow.model.EventRegistrationCreateRequest;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.registration.RegistrationSession;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.ui.KeyboardBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class EventRegisterHandler implements StepHandler {

    private static final Logger LOG = Logger.getLogger(EventRegisterHandler.class);

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final EventRegistrationsApi registrationsApi;

    @Inject
    public EventRegisterHandler(
        TelegramClient telegramClient,
        I18n i18n,
        @RestClient EventRegistrationsApi registrationsApi
    ) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.registrationsApi = registrationsApi;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        // Callback data: "evt_reg_<uuid>"
        String cb = ctx.callbackData();
        if (cb == null || !cb.startsWith(EventsListFlowDef.CB_EVT_REG_PREFIX)) {
            telegramClient.sendHtml(session.getChatId(),
                i18n.t(lang, "events_register_error"), homeButton(lang));
            return StepResult.finish();
        }

        String uuidStr = cb.substring(EventsListFlowDef.CB_EVT_REG_PREFIX.length());
        UUID eventId;
        try {
            eventId = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            telegramClient.sendHtml(session.getChatId(),
                i18n.t(lang, "events_register_error"), homeButton(lang));
            return StepResult.finish();
        }

        String guestIdStr = RegistrationSession.getGuestId(session);
        if (guestIdStr == null) {
            telegramClient.sendHtml(session.getChatId(),
                i18n.t(lang, "auth_required"), homeButton(lang));
            return StepResult.finish();
        }

        UUID guestId;
        try {
            guestId = UUID.fromString(guestIdStr);
        } catch (IllegalArgumentException e) {
            telegramClient.sendHtml(session.getChatId(),
                i18n.t(lang, "events_register_error"), homeButton(lang));
            return StepResult.finish();
        }

        try {
            var request = new EventRegistrationCreateRequest();
            request.setEventId(eventId);
            request.setGuestId(guestId);
            request.setGuestCount(1);
            request.setSource("telegram-bot");

            registrationsApi.createEventRegistration(request);

            telegramClient.sendHtml(session.getChatId(),
                i18n.t(lang, "events_register_success"), homeButton(lang));

        } catch (WebApplicationException e) {
            int status = e.getResponse().getStatus();
            if (status == 409) {
                telegramClient.sendHtml(session.getChatId(),
                    i18n.t(lang, "events_register_already"), backToListButton(lang));
            } else {
                LOG.warnf("Event registration failed: HTTP %d, event=%s, guest=%s",
                    status, eventId, guestIdStr);
                telegramClient.sendHtml(session.getChatId(),
                    i18n.t(lang, "events_register_error"), backToListButton(lang));
            }
        } catch (Exception e) {
            LOG.warnf(e, "Event registration failed: event=%s, guest=%s", eventId, guestIdStr);
            telegramClient.sendHtml(session.getChatId(),
                i18n.t(lang, "events_register_error"), backToListButton(lang));
        }

        return StepResult.finish();
    }

    private Object homeButton(String lang) {
        return KeyboardBuilder.inline(List.of(
            KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "events_list_btn_home"), "/start")
            )
        ));
    }

    private Object backToListButton(String lang) {
        return KeyboardBuilder.inline(List.of(
            KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "events_detail_back_btn"), "events"),
                KeyboardBuilder.cbCmd(i18n.t(lang, "events_list_btn_home"), "/start")
            )
        ));
    }
}
