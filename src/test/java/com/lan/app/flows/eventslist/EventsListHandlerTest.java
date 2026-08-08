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
class EventsListHandlerTest {

    @Inject
    EventsListHandler handler;

    @InjectMock
    TelegramClient telegramClient;

    @InjectMock
    I18n i18n;

    @InjectMock
    @RestClient
    EventResourceApi eventApi;

    @InjectMock
    @RestClient
    FestivalsApi festivalsApi;

    @BeforeEach
    void stubTranslations() {
        when(i18n.t(any(), any())).thenReturn("translated");
        when(eventApi.eventsV1Get()).thenReturn(List.of());
        when(festivalsApi.listFestivals()).thenReturn(List.of());
    }

    private static Session session() {
        return Session.newDefault(100L, 200L);
    }

    private static UpdateContext ctx() {
        return new UpdateContext(100L, "private", 200L, null, "/events", null, null, false, "bob", null, null, null);
    }

    @SuppressWarnings("unchecked")
    private static List<?> keyboardRows(Object replyMarkup) {
        return (List<?>) ((Map<String, Object>) replyMarkup).get("inline_keyboard");
    }

    @SuppressWarnings("unchecked")
    private static List<String> buttonTexts(Object replyMarkup) {
        List<String> texts = new java.util.ArrayList<>();
        for (Object row : keyboardRows(replyMarkup)) {
            for (Object button : (List<?>) row) {
                texts.add((String) ((Map<String, String>) button).get("text"));
            }
        }
        return texts;
    }

    private static EventResponse event(String name) {
        return event(name, null);
    }

    private static EventResponse event(String name, OffsetDateTime dateStart) {
        EventResponse e = new EventResponse();
        e.setId(UUID.randomUUID());
        e.setName(name);
        e.setDateStart(dateStart);
        return e;
    }

    private static FestivalResponse festival(String name, boolean pinned, boolean visible, UUID... eventIds) {
        FestivalResponse f = new FestivalResponse();
        f.setId(UUID.randomUUID());
        f.setName(name);
        f.setIsPin(pinned);
        f.setIsVisible(visible);
        f.setEventsIds(List.of(eventIds));
        return f;
    }

    @Test
    void bothApisFail_sendsErrorAndFinishes() {
        Session s = session();
        when(eventApi.eventsV1Get()).thenThrow(new RuntimeException("boom"));
        when(festivalsApi.listFestivals()).thenThrow(new RuntimeException("boom"));

        StepResult result = handler.handle(ctx(), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verify(i18n).t(eq("en"), eq("events_list_error"));
    }

    @Test
    void noEventsNoFestivals_sendsEmptyMessage() {
        Session s = session();

        StepResult result = handler.handle(ctx(), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verify(i18n).t(eq("en"), eq("events_list_empty"));
    }

    @Test
    void standaloneEvents_areListedAsButtons() {
        Session s = session();
        EventResponse e1 = event("Event One");
        EventResponse e2 = event("Event Two");
        when(eventApi.eventsV1Get()).thenReturn(List.of(e1, e2));

        StepResult result = handler.handle(ctx(), s);

        assertThat(result).isEqualTo(StepResult.stay(EventsListFlowDef.FLOW, EventsListFlowDef.STEP_LIST));
        var captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(telegramClient).sendHtml(eq(100L), any(), captor.capture());
        // 2 event rows + nav row
        assertThat(keyboardRows(captor.getValue())).hasSize(3);
    }

    @Test
    void eventBelongingToFestival_isHiddenFromStandaloneList() {
        Session s = session();
        EventResponse standalone = event("Standalone");
        EventResponse partOfFestival = event("In Festival");
        when(eventApi.eventsV1Get()).thenReturn(List.of(standalone, partOfFestival));
        FestivalResponse fest = festival("Fest", false, true, partOfFestival.getId());
        when(festivalsApi.listFestivals()).thenReturn(List.of(fest));

        handler.handle(ctx(), s);

        var captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(telegramClient).sendHtml(eq(100L), any(), captor.capture());
        // 1 standalone event row + 1 festival row + nav row
        assertThat(keyboardRows(captor.getValue())).hasSize(3);
    }

    @Test
    void invisibleFestival_isExcluded() {
        Session s = session();
        when(eventApi.eventsV1Get()).thenReturn(List.of(event("Standalone")));
        FestivalResponse hidden = festival("Hidden", false, false);
        when(festivalsApi.listFestivals()).thenReturn(List.of(hidden));

        handler.handle(ctx(), s);

        var captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(telegramClient).sendHtml(eq(100L), any(), captor.capture());
        // 1 event row + nav row (festival excluded)
        assertThat(keyboardRows(captor.getValue())).hasSize(2);
    }

    @Test
    void eventsApiFailsButFestivalsSucceed_stillRendersFestivals() {
        Session s = session();
        when(eventApi.eventsV1Get()).thenThrow(new RuntimeException("boom"));
        FestivalResponse fest = festival("Fest", true, true);
        when(festivalsApi.listFestivals()).thenReturn(List.of(fest));

        StepResult result = handler.handle(ctx(), s);

        assertThat(result).isEqualTo(StepResult.stay(EventsListFlowDef.FLOW, EventsListFlowDef.STEP_LIST));
        var captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(telegramClient).sendHtml(eq(100L), any(), captor.capture());
        // 1 pinned festival row + nav row
        assertThat(keyboardRows(captor.getValue())).hasSize(2);
    }

    @Test
    void pastEvent_isExcludedFromList() {
        Session s = session();
        EventResponse past = event("Past Event", OffsetDateTime.now().minusDays(1));
        EventResponse future = event("Future Event", OffsetDateTime.now().plusDays(1));
        when(eventApi.eventsV1Get()).thenReturn(List.of(past, future));

        handler.handle(ctx(), s);

        var captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(telegramClient).sendHtml(eq(100L), any(), captor.capture());
        var texts = buttonTexts(captor.getValue());
        assertThat(texts.stream().anyMatch(t -> t.contains("Future Event"))).isTrue();
        assertThat(texts.stream().anyMatch(t -> t.contains("Past Event"))).isFalse();
    }

    @Test
    void onlyPastEvents_sendsEmptyMessage() {
        Session s = session();
        when(eventApi.eventsV1Get()).thenReturn(List.of(event("Past Event", OffsetDateTime.now().minusDays(1))));

        StepResult result = handler.handle(ctx(), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verify(i18n).t(eq("en"), eq("events_list_empty"));
    }

    @Test
    void events_areSortedByDateAscending() {
        Session s = session();
        EventResponse later = event("Later", OffsetDateTime.now().plusDays(5));
        EventResponse sooner = event("Sooner", OffsetDateTime.now().plusDays(1));
        when(eventApi.eventsV1Get()).thenReturn(List.of(later, sooner));

        handler.handle(ctx(), s);

        var captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(telegramClient).sendHtml(eq(100L), any(), captor.capture());
        var texts = buttonTexts(captor.getValue());
        int sonerIdx = indexContaining(texts, "Sooner");
        int laterIdx = indexContaining(texts, "Later");
        assertThat(sonerIdx).isLessThan(laterIdx);
    }

    private static int indexContaining(List<String> texts, String needle) {
        for (int i = 0; i < texts.size(); i++) {
            if (texts.get(i).contains(needle)) return i;
        }
        return -1;
    }

    @Test
    void listing_clearsParentFestivalIdFromSession() {
        Session s = session();
        EventsListSession.setParentFestivalId(s, "some-festival");
        when(eventApi.eventsV1Get()).thenReturn(List.of(event("Standalone")));

        handler.handle(ctx(), s);

        assertThat(EventsListSession.getParentFestivalId(s)).isNull();
    }
}
