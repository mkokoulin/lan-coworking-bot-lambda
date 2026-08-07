package com.lan.app.flows.eventslist;

import com.lan.app.client.baserow.api.EventGuestsApi;
import com.lan.app.client.baserow.api.EventRegistrationsApi;
import com.lan.app.client.baserow.api.EventResourceApi;
import com.lan.app.client.baserow.model.CoworkingGuestResponse;
import com.lan.app.client.baserow.model.EventGuestResponse;
import com.lan.app.client.baserow.model.EventRegistrationCreateRequest;
import com.lan.app.client.baserow.model.EventResponse;
import com.lan.app.domain.IncomingUpdate;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.registration.RegistrationSession;
import com.lan.app.i18n.I18n;
import com.lan.app.service.GuestService;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class EventRegisterHandlerTest {

    @Inject
    EventRegisterHandler handler;

    @InjectMock
    TelegramClient telegramClient;

    @InjectMock
    I18n i18n;

    @InjectMock
    @RestClient
    EventRegistrationsApi registrationsApi;

    @InjectMock
    @RestClient
    EventGuestsApi eventGuestsApi;

    @InjectMock
    @RestClient
    EventResourceApi eventApi;

    @InjectMock
    GuestService guestService;

    @BeforeEach
    void stubTranslations() {
        when(i18n.t(any(), any())).thenReturn("translated");
        when(guestService.findByChatId(anyLong())).thenReturn(Optional.empty());
    }

    private static Session session() {
        Session s = Session.newDefault(100L, 200L);
        RegistrationSession.setGuestId(s, UUID.randomUUID().toString());
        return s;
    }

    private static UpdateContext callbackCtx(String data) {
        IncomingUpdate u = new IncomingUpdate();
        u.setChatId(100L);
        u.setUserId(200L);
        u.setCallbackData(data);
        return UpdateContext.fromIncomingUpdate(u);
    }

    private void stubEventGuestSuccess(String eventGuestId) {
        EventGuestResponse resp = new EventGuestResponse();
        resp.setId(eventGuestId);
        when(eventGuestsApi.createEventGuest(any())).thenReturn(resp);
    }

    @Test
    void nullCallback_sendsErrorAndFinishes() {
        Session s = session();
        UpdateContext ctx = new UpdateContext(100L, "private", 200L, null, null, null, null, false, null, null, null, null);

        StepResult result = handler.handle(ctx, s);

        assertThat(result).isEqualTo(StepResult.finish());
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }

    @Test
    void invalidUuidInCallback_sendsErrorAndFinishes() {
        Session s = session();

        StepResult result = handler.handle(callbackCtx(EventsListFlowDef.CB_EVT_REG_PREFIX + "not-a-uuid"), s);

        assertThat(result).isEqualTo(StepResult.finish());
    }

    @Test
    void missingGuestId_sendsAuthRequiredAndFinishes() {
        Session s = Session.newDefault(100L, 200L); // no guestId set
        UUID eventId = UUID.randomUUID();

        StepResult result = handler.handle(callbackCtx(EventsListFlowDef.CB_EVT_REG_PREFIX + eventId), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verify(i18n).t(eq("ru"), eq("auth_required"));
    }

    @Test
    void malformedGuestId_sendsErrorAndFinishes() {
        Session s = Session.newDefault(100L, 200L);
        RegistrationSession.setGuestId(s, "not-a-uuid");
        UUID eventId = UUID.randomUUID();

        StepResult result = handler.handle(callbackCtx(EventsListFlowDef.CB_EVT_REG_PREFIX + eventId), s);

        assertThat(result).isEqualTo(StepResult.finish());
    }

    @Test
    void success_registersEventGuestAndCreatesRegistration() {
        Session s = session();
        UUID eventId = UUID.randomUUID();
        String eventGuestId = UUID.randomUUID().toString();
        stubEventGuestSuccess(eventGuestId);

        StepResult result = handler.handle(callbackCtx(EventsListFlowDef.CB_EVT_REG_PREFIX + eventId), s);

        assertThat(result).isEqualTo(StepResult.finish());
        var captor = org.mockito.ArgumentCaptor.forClass(EventRegistrationCreateRequest.class);
        verify(registrationsApi).createEventRegistration(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo(eventId);
        assertThat(captor.getValue().getGuestId()).isEqualTo(UUID.fromString(eventGuestId));
        assertThat(captor.getValue().getGuestCount()).isEqualTo(1);
        assertThat(captor.getValue().getSource()).isEqualTo("telegram-bot");
        verify(i18n).t(eq("ru"), eq("events_register_success"));
    }

    @Test
    void success_notifiesAdminWithEventName() {
        Session s = session();
        UUID eventId = UUID.randomUUID();
        String eventGuestId = UUID.randomUUID().toString();
        stubEventGuestSuccess(eventGuestId);

        EventResponse event = mock(EventResponse.class);
        when(event.getName()).thenReturn("Ламповый вечер настолок");
        when(eventApi.eventsV1ExternalIdGet(eventId)).thenReturn(event);

        CoworkingGuestResponse guest = mock(CoworkingGuestResponse.class);
        when(guest.getFirstName()).thenReturn("Ann");
        when(guest.getLastName()).thenReturn("Smith");
        when(guest.getPhone()).thenReturn("+37491123456");
        when(guestService.findByChatId(100L)).thenReturn(Optional.of(guest));

        handler.handle(callbackCtx(EventsListFlowDef.CB_EVT_REG_PREFIX + eventId), s);

        // telegram.admin-chat-id resolves to 999999 in the test environment (see build.gradle.kts /
        // TG_ADMIN_CHAT_ID), so the admin notification is asserted against that fixed chat id.
        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(telegramClient).sendHtml(eq(999999L), captor.capture(), any());
        assertThat(captor.getValue()).contains("Ламповый вечер настолок");
        assertThat(captor.getValue()).contains("Ann Smith");
    }

    @Test
    void success_usesCoworkingGuestProfileFields() {
        Session s = session();
        UUID eventId = UUID.randomUUID();
        String eventGuestId = UUID.randomUUID().toString();
        stubEventGuestSuccess(eventGuestId);

        CoworkingGuestResponse guest = mock(CoworkingGuestResponse.class);
        when(guest.getFirstName()).thenReturn("Ann");
        when(guest.getLastName()).thenReturn("Smith");
        when(guest.getPhone()).thenReturn("+37491123456");
        when(guest.getTelegram()).thenReturn("annsmith");
        when(guestService.findByChatId(100L)).thenReturn(Optional.of(guest));

        handler.handle(callbackCtx(EventsListFlowDef.CB_EVT_REG_PREFIX + eventId), s);

        var captor = org.mockito.ArgumentCaptor.forClass(com.lan.app.client.baserow.model.CreateEventGuestRequest.class);
        verify(eventGuestsApi).createEventGuest(captor.capture());
        assertThat(captor.getValue().getFirstName()).isEqualTo("Ann");
        assertThat(captor.getValue().getLastName()).isEqualTo("Smith");
        assertThat(captor.getValue().getPhone()).isEqualTo("+37491123456");
        assertThat(captor.getValue().getTelegram()).isEqualTo("@annsmith");
    }

    @Test
    void noCoworkingGuestProfile_usesSyntheticPhoneAndDefaultFirstName() {
        Session s = session();
        UUID eventId = UUID.randomUUID();
        String eventGuestId = UUID.randomUUID().toString();
        stubEventGuestSuccess(eventGuestId);

        handler.handle(callbackCtx(EventsListFlowDef.CB_EVT_REG_PREFIX + eventId), s);

        var captor = org.mockito.ArgumentCaptor.forClass(com.lan.app.client.baserow.model.CreateEventGuestRequest.class);
        verify(eventGuestsApi).createEventGuest(captor.capture());
        assertThat(captor.getValue().getFirstName()).isEqualTo("User");
        assertThat(captor.getValue().getPhone()).isEqualTo("tg:100");
    }

    @Test
    void eventGuestCreationThrowsWebApplicationException_fallsBackToCoworkingGuestId() {
        Session s = session();
        UUID eventId = UUID.randomUUID();
        UUID cwGuestId = UUID.fromString(RegistrationSession.getGuestId(s));
        when(eventGuestsApi.createEventGuest(any()))
                .thenThrow(new WebApplicationException(Response.status(500).build()));

        handler.handle(callbackCtx(EventsListFlowDef.CB_EVT_REG_PREFIX + eventId), s);

        var captor = org.mockito.ArgumentCaptor.forClass(EventRegistrationCreateRequest.class);
        verify(registrationsApi).createEventRegistration(captor.capture());
        assertThat(captor.getValue().getGuestId()).isEqualTo(cwGuestId);
    }

    @Test
    void eventGuestCreationThrowsGenericException_fallsBackToCoworkingGuestId() {
        Session s = session();
        UUID eventId = UUID.randomUUID();
        UUID cwGuestId = UUID.fromString(RegistrationSession.getGuestId(s));
        when(eventGuestsApi.createEventGuest(any())).thenThrow(new RuntimeException("boom"));

        handler.handle(callbackCtx(EventsListFlowDef.CB_EVT_REG_PREFIX + eventId), s);

        var captor = org.mockito.ArgumentCaptor.forClass(EventRegistrationCreateRequest.class);
        verify(registrationsApi).createEventRegistration(captor.capture());
        assertThat(captor.getValue().getGuestId()).isEqualTo(cwGuestId);
    }

    @Test
    void eventGuestCreationReturnsEmptyId_fallsBackToCoworkingGuestId() {
        Session s = session();
        UUID eventId = UUID.randomUUID();
        UUID cwGuestId = UUID.fromString(RegistrationSession.getGuestId(s));
        EventGuestResponse resp = new EventGuestResponse();
        resp.setId(null);
        when(eventGuestsApi.createEventGuest(any())).thenReturn(resp);

        handler.handle(callbackCtx(EventsListFlowDef.CB_EVT_REG_PREFIX + eventId), s);

        var captor = org.mockito.ArgumentCaptor.forClass(EventRegistrationCreateRequest.class);
        verify(registrationsApi).createEventRegistration(captor.capture());
        assertThat(captor.getValue().getGuestId()).isEqualTo(cwGuestId);
    }

    @Test
    void registration409Conflict_sendsAlreadyRegisteredMessage() {
        Session s = session();
        UUID eventId = UUID.randomUUID();
        stubEventGuestSuccess(UUID.randomUUID().toString());
        when(registrationsApi.createEventRegistration(any()))
                .thenThrow(new WebApplicationException(Response.status(409).build()));

        StepResult result = handler.handle(callbackCtx(EventsListFlowDef.CB_EVT_REG_PREFIX + eventId), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verify(i18n).t(eq("ru"), eq("events_register_already"));
    }

    @Test
    void registrationOtherHttpError_sendsGenericErrorMessage() {
        Session s = session();
        UUID eventId = UUID.randomUUID();
        stubEventGuestSuccess(UUID.randomUUID().toString());
        when(registrationsApi.createEventRegistration(any()))
                .thenThrow(new WebApplicationException(Response.status(500).build()));

        handler.handle(callbackCtx(EventsListFlowDef.CB_EVT_REG_PREFIX + eventId), s);

        verify(i18n).t(eq("ru"), eq("events_register_error"));
    }

    @Test
    void registrationGenericException_sendsGenericErrorMessage() {
        Session s = session();
        UUID eventId = UUID.randomUUID();
        stubEventGuestSuccess(UUID.randomUUID().toString());
        when(registrationsApi.createEventRegistration(any())).thenThrow(new RuntimeException("boom"));

        handler.handle(callbackCtx(EventsListFlowDef.CB_EVT_REG_PREFIX + eventId), s);

        verify(i18n).t(eq("ru"), eq("events_register_error"));
    }
}
