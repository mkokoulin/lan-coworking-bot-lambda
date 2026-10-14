package com.lan.app.flows.eventslist;

import com.lan.app.client.baserow.api.EventResourceApi;
import com.lan.app.client.baserow.api.FestivalsApi;
import com.lan.app.client.baserow.model.EventResponse;
import com.lan.app.client.baserow.model.FestivalResponse;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class FestivalDetailHandlerTest {

    @Inject
    FestivalDetailHandler handler;

    @InjectMock
    TelegramClient telegramClient;

    @InjectMock
    I18n i18n;

    @InjectMock
    @RestClient
    FestivalsApi festivalsApi;

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

    private static FestivalResponse festival(UUID id, String name, UUID... eventIds) {
        FestivalResponse f = new FestivalResponse();
        f.setId(id);
        f.setName(name);
        f.setEventsIds(List.of(eventIds));
        return f;
    }

    private static EventResponse event(UUID id, String name) {
        EventResponse e = new EventResponse();
        e.setId(id);
        e.setName(name);
        return e;
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
    void invalidUuid_sendsErrorAndFinishes() {
        Session s = session();

        StepResult result = handler.handle(callbackCtx(EventsListFlowDef.CB_EVF_PREFIX + "not-a-uuid"), s);

        assertThat(result).isEqualTo(StepResult.finish());
    }

    @Test
    void festivalsApiThrows_sendsErrorAndFinishes() {
        Session s = session();
        UUID id = UUID.randomUUID();
        when(festivalsApi.getFestivalById(id)).thenThrow(new RuntimeException("boom"));

        StepResult result = handler.handle(callbackCtx(EventsListFlowDef.CB_EVF_PREFIX + id), s);

        assertThat(result).isEqualTo(StepResult.finish());
    }

    @Test
    void festivalsApiReturnsNull_sendsErrorAndFinishes() {
        Session s = session();
        UUID id = UUID.randomUUID();
        when(festivalsApi.getFestivalById(id)).thenReturn(null);

        StepResult result = handler.handle(callbackCtx(EventsListFlowDef.CB_EVF_PREFIX + id), s);

        assertThat(result).isEqualTo(StepResult.finish());
    }

    @Test
    void validFestivalWithMatchingSubEvents_rendersEventRowsAndStoresParentId() {
        Session s = session();
        UUID festivalId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        FestivalResponse fest = festival(festivalId, "Fest", eventId);
        when(festivalsApi.getFestivalById(festivalId)).thenReturn(fest);
        when(eventApi.eventsV1Get()).thenReturn(List.of(event(eventId, "Sub Event")));

        StepResult result = handler.handle(callbackCtx(EventsListFlowDef.CB_EVF_PREFIX + festivalId), s);

        assertThat(result).isEqualTo(StepResult.stay(EventsListFlowDef.FLOW, EventsListFlowDef.STEP_FESTIVAL));
        assertThat(EventsListSession.getParentFestivalId(s)).isEqualTo(festivalId.toString());
        var captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(telegramClient).sendHtml(eq(100L), any(), captor.capture());
        // 1 sub-event row + nav row
        assertThat(keyboardRows(captor.getValue())).hasSize(2);
    }

    @Test
    void subEventIdNotFoundInEventList_isSkippedGracefully() {
        Session s = session();
        UUID festivalId = UUID.randomUUID();
        UUID missingEventId = UUID.randomUUID();
        FestivalResponse fest = festival(festivalId, "Fest", missingEventId);
        when(festivalsApi.getFestivalById(festivalId)).thenReturn(fest);
        when(eventApi.eventsV1Get()).thenReturn(List.of());

        handler.handle(callbackCtx(EventsListFlowDef.CB_EVF_PREFIX + festivalId), s);

        var captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(telegramClient).sendHtml(eq(100L), any(), captor.capture());
        // only nav row, no sub-event row since it wasn't found
        assertThat(keyboardRows(captor.getValue())).hasSize(1);
    }

    @Test
    void festivalWithNoSubEvents_onlyRendersNavRow() {
        Session s = session();
        UUID festivalId = UUID.randomUUID();
        FestivalResponse fest = festival(festivalId, "Fest");
        when(festivalsApi.getFestivalById(festivalId)).thenReturn(fest);

        handler.handle(callbackCtx(EventsListFlowDef.CB_EVF_PREFIX + festivalId), s);

        var captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(telegramClient).sendHtml(eq(100L), any(), captor.capture());
        assertThat(keyboardRows(captor.getValue())).hasSize(1);
    }

    @Test
    void longDescription_isTruncated() {
        Session s = session();
        UUID festivalId = UUID.randomUUID();
        FestivalResponse fest = festival(festivalId, "Fest");
        fest.setDescription("x".repeat(700));
        when(festivalsApi.getFestivalById(festivalId)).thenReturn(fest);

        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        handler.handle(callbackCtx(EventsListFlowDef.CB_EVF_PREFIX + festivalId), s);

        verify(telegramClient).sendHtml(eq(100L), captor.capture(), any());
        assertThat(captor.getValue()).contains("…");
    }
}
