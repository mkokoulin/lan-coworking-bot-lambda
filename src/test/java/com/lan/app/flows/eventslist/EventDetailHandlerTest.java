package com.lan.app.flows.eventslist;

import com.lan.app.client.baserow.api.EventResourceApi;
import com.lan.app.client.baserow.model.EventResponse;
import com.lan.app.domain.IncomingUpdate;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class EventDetailHandlerTest {

    @Inject
    EventDetailHandler handler;

    @InjectMock
    TelegramClient telegramClient;

    @InjectMock
    I18n i18n;

    @InjectMock
    @RestClient
    EventResourceApi eventApi;

    @BeforeEach
    void stubTranslations() {
        when(i18n.t(any(), any())).thenReturn("translated");
    }

    private static Session session() {
        return Session.newDefault(100L, 200L);
    }

    private static UpdateContext callbackCtx(String data) {
        IncomingUpdate u = new IncomingUpdate();
        u.setChatId(100L);
        u.setUserId(200L);
        u.setCallbackData(data);
        return UpdateContext.fromIncomingUpdate(u);
    }

    @SuppressWarnings("unchecked")
    private static List<?> keyboardRows(Object replyMarkup) {
        return (List<?>) ((Map<String, Object>) replyMarkup).get("inline_keyboard");
    }

    private static EventResponse baseEvent(UUID id) {
        EventResponse event = new EventResponse();
        event.setId(id);
        event.setName("Cool Event");
        event.setDateStart(OffsetDateTime.now());
        return event;
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
    void callbackWithoutRecognizedPrefix_sendsErrorAndFinishes() {
        Session s = session();

        StepResult result = handler.handle(callbackCtx("something_else"), s);

        assertThat(result).isEqualTo(StepResult.finish());
    }

    @Test
    void invalidUuidInCallback_sendsErrorAndFinishes() {
        Session s = session();

        StepResult result = handler.handle(callbackCtx(EventsListFlowDef.CB_EVT_PREFIX + "not-a-uuid"), s);

        assertThat(result).isEqualTo(StepResult.finish());
    }

    @Test
    void eventApiThrows_sendsErrorAndFinishes() {
        Session s = session();
        UUID id = UUID.randomUUID();
        when(eventApi.eventsV1ExternalIdGet(id)).thenThrow(new RuntimeException("boom"));

        StepResult result = handler.handle(callbackCtx(EventsListFlowDef.CB_EVT_PREFIX + id), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }

    @Test
    void eventApiReturnsNull_sendsErrorAndFinishes() {
        Session s = session();
        UUID id = UUID.randomUUID();
        when(eventApi.eventsV1ExternalIdGet(id)).thenReturn(null);

        StepResult result = handler.handle(callbackCtx(EventsListFlowDef.CB_EVT_PREFIX + id), s);

        assertThat(result).isEqualTo(StepResult.finish());
    }

    @Test
    void showFormTrue_addsRegisterButton() {
        Session s = session();
        UUID id = UUID.randomUUID();
        EventResponse event = baseEvent(id);
        event.setShowForm(true);
        when(eventApi.eventsV1ExternalIdGet(id)).thenReturn(event);

        StepResult result = handler.handle(callbackCtx(EventsListFlowDef.CB_EVT_PREFIX + id), s);

        assertThat(result).isEqualTo(StepResult.stay(EventsListFlowDef.FLOW, EventsListFlowDef.STEP_DETAIL));
        var captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(telegramClient).sendHtml(eq(100L), any(), captor.capture());
        // register row + back-nav row
        assertThat(keyboardRows(captor.getValue())).hasSize(2);
    }

    @Test
    void showFormFalse_withExternalUrl_addsExternalButton() {
        Session s = session();
        UUID id = UUID.randomUUID();
        EventResponse event = baseEvent(id);
        event.setShowForm(false);
        event.setExternalRegistrationUrl(URI.create("https://example.com/register"));
        when(eventApi.eventsV1ExternalIdGet(id)).thenReturn(event);

        handler.handle(callbackCtx(EventsListFlowDef.CB_EVT_PREFIX + id), s);

        var captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(telegramClient).sendHtml(eq(100L), any(), captor.capture());
        assertThat(keyboardRows(captor.getValue())).hasSize(2);
    }

    @Test
    void withInstagramUrl_addsExtraRow() {
        Session s = session();
        UUID id = UUID.randomUUID();
        EventResponse event = baseEvent(id);
        event.setInstagramUrl(URI.create("https://instagram.com/x"));
        when(eventApi.eventsV1ExternalIdGet(id)).thenReturn(event);

        handler.handle(callbackCtx(EventsListFlowDef.CB_EVT_PREFIX + id), s);

        var captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(telegramClient).sendHtml(eq(100L), any(), captor.capture());
        // instagram row + back-nav row (no register/external button since showForm not true and no external url)
        assertThat(keyboardRows(captor.getValue())).hasSize(2);
    }

    @Test
    void withParentFestivalId_usesBackToFestivalNavigation() {
        Session s = session();
        EventsListSession.setParentFestivalId(s, "fest-1");
        UUID id = UUID.randomUUID();
        EventResponse event = baseEvent(id);
        when(eventApi.eventsV1ExternalIdGet(id)).thenReturn(event);

        handler.handle(callbackCtx(EventsListFlowDef.CB_EVT_PREFIX + id), s);

        var captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(telegramClient).sendHtml(eq(100L), any(), captor.capture());
        assertThat(keyboardRows(captor.getValue())).hasSize(1);
    }

    @Test
    void longDescription_isTruncated() {
        Session s = session();
        UUID id = UUID.randomUUID();
        EventResponse event = baseEvent(id);
        event.setDescription("x".repeat(900));
        when(eventApi.eventsV1ExternalIdGet(id)).thenReturn(event);

        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        handler.handle(callbackCtx(EventsListFlowDef.CB_EVT_PREFIX + id), s);

        verify(telegramClient).sendHtml(eq(100L), captor.capture(), any());
        assertThat(captor.getValue()).contains("…");
        assertThat(captor.getValue().length()).isLessThan(900 + 200);
    }
}
