package com.lan.app.flows.eventslist;

import com.lan.app.client.baserow.api.EventGuestsApi;
import com.lan.app.client.baserow.api.EventRegistrationsApi;
import com.lan.app.client.baserow.model.CreateEventGuestRequest;
import com.lan.app.client.baserow.model.EventGuestResponse;
import com.lan.app.client.baserow.model.EventRegistrationCreateRequest;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.registration.RegistrationSession;
import com.lan.app.i18n.I18n;
import com.lan.app.service.GuestService;
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
    private final EventGuestsApi eventGuestsApi;
    private final GuestService guestService;

    @Inject
    public EventRegisterHandler(
        TelegramClient telegramClient,
        I18n i18n,
        @RestClient EventRegistrationsApi registrationsApi,
        @RestClient EventGuestsApi eventGuestsApi,
        GuestService guestService
    ) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.registrationsApi = registrationsApi;
        this.eventGuestsApi = eventGuestsApi;
        this.guestService = guestService;
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

        // Resolve event guest UUID.
        // The /events/v1/registrations endpoint expects a guest UUID from the EVENT guest
        // system (/events/v1/guests), not from the coworking guest system. We call
        // POST /events/v1/guests first (which is typically upsert on phone+chatId) to get
        // the correct event guest UUID, then use it in the registration call.
        UUID eventGuestId = resolveEventGuestId(session, guestId, lang);
        if (eventGuestId == null) {
            return StepResult.finish(); // error message already sent
        }

        try {
            var request = new EventRegistrationCreateRequest();
            request.setEventId(eventId);
            request.setGuestId(eventGuestId);
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
                LOG.warnf("Event registration failed: HTTP %d, event=%s, eventGuest=%s (cwGuest=%s)",
                    status, eventId, eventGuestId, guestIdStr);
                telegramClient.sendHtml(session.getChatId(),
                    i18n.t(lang, "events_register_error"), backToListButton(lang));
            }
        } catch (Exception e) {
            LOG.warnf(e, "Event registration failed: event=%s, eventGuest=%s (cwGuest=%s)",
                eventId, eventGuestId, guestIdStr);
            telegramClient.sendHtml(session.getChatId(),
                i18n.t(lang, "events_register_error"), backToListButton(lang));
        }

        return StepResult.finish();
    }

    /**
     * Returns the event-guest UUID to use in {@code EventRegistrationCreateRequest.guestId}.
     * The events API maintains a separate guest table under /events/v1/guests; sending the
     * coworking guest UUID causes a 500 on the backend. We call POST /events/v1/guests
     * (upsert semantics on chatId) to obtain the correct UUID.
     *
     * Falls back to the coworking guestId if the event guest call fails, so existing
     * deployments with a unified guest model are not broken.
     */
    private UUID resolveEventGuestId(Session session, UUID cwGuestId, String lang) {
        // Collect user info from the coworking guest profile
        String firstName = "User";
        String lastName = null;
        String phone = null;
        String telegram = null;

        var cwGuest = guestService.findByChatId(session.getChatId());
        if (cwGuest.isPresent()) {
            var g = cwGuest.get();
            if (g.getFirstName() != null && !g.getFirstName().isBlank()) firstName = g.getFirstName();
            lastName  = (g.getLastName()  != null && !g.getLastName().isBlank())  ? g.getLastName()  : null;
            phone     = (g.getPhone()     != null && !g.getPhone().isBlank())     ? g.getPhone()     : null;
            telegram  = (g.getTelegram()  != null && !g.getTelegram().isBlank())  ? "@" + g.getTelegram() : null;
        }

        // phone is required — fallback to a synthetic value derived from chatId
        if (phone == null || phone.isBlank()) {
            phone = "tg:" + session.getChatId();
        }

        try {
            var req = new CreateEventGuestRequest();
            req.setFirstName(firstName);
            req.setLastName(lastName);
            req.setPhone(phone);
            req.setTelegram(telegram);
            req.setChatId(session.getChatId());
            req.setSource("telegram-bot");

            EventGuestResponse eventGuest = eventGuestsApi.createEventGuest(req);
            if (eventGuest != null && eventGuest.getId() != null && !eventGuest.getId().isBlank()) {
                return UUID.fromString(eventGuest.getId());
            }
        } catch (WebApplicationException e) {
            LOG.warnf("createEventGuest HTTP %d for chatId=%d — falling back to coworking guestId",
                e.getResponse().getStatus(), session.getChatId());
            // Fall back: maybe the backend treats coworking UUIDs as valid event guest IDs
            return cwGuestId;
        } catch (Exception e) {
            LOG.warnf(e, "createEventGuest failed for chatId=%d — falling back to coworking guestId",
                session.getChatId());
            return cwGuestId;
        }

        // Unexpected: response was empty — fall back
        LOG.warnf("createEventGuest returned empty id for chatId=%d — falling back to coworking guestId",
            session.getChatId());
        return cwGuestId;
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
