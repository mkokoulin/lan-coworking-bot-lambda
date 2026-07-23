package com.lan.app.flows.myevents;

import com.lan.app.client.baserow.api.BotApi;
import com.lan.app.client.baserow.model.BotRegistrationDto;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class MyEventsHandlerTest {

    @Inject
    MyEventsHandler handler;

    @InjectMock
    TelegramClient telegramClient;

    @InjectMock
    I18n i18n;

    @InjectMock
    @RestClient
    BotApi botApi;

    @BeforeEach
    void stubTranslations() {
        when(i18n.t(any(), any())).thenReturn("translated");
    }

    private static Session session() {
        return Session.newDefault(100L, 200L);
    }

    private static UpdateContext ctx() {
        return new UpdateContext(100L, "private", 200L, null, "/myevents", null, null, false, "bob", null, null, null);
    }

    private static BotRegistrationDto registration(String name, OffsetDateTime dateStart) {
        BotRegistrationDto dto = new BotRegistrationDto();
        dto.setEventName(name);
        dto.setDateStart(dateStart);
        return dto;
    }

    @Test
    void botApiThrows_sendsErrorMessage() {
        Session s = session();
        when(botApi.botMyRegistrations(anyLong())).thenThrow(new RuntimeException("boom"));

        StepResult result = handler.handle(ctx(), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verify(i18n).t(eq("ru"), eq("myevents_error"));
    }

    @Test
    void emptyRegistrations_sendsEmptyMessage() {
        Session s = session();
        when(botApi.botMyRegistrations(100L)).thenReturn(List.of());

        StepResult result = handler.handle(ctx(), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verify(i18n).t(eq("ru"), eq("myevents_empty"));
    }

    @Test
    void registrations_areListedNumberedWithNameAndDate() {
        Session s = session();
        OffsetDateTime date = OffsetDateTime.parse("2026-08-01T18:00:00Z");
        when(botApi.botMyRegistrations(100L)).thenReturn(List.of(registration("Party Night", date)));

        StepResult result = handler.handle(ctx(), s);

        assertThat(result).isEqualTo(StepResult.finish());
        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(telegramClient).sendHtml(eq(100L), captor.capture(), any());
        assertThat(captor.getValue()).contains("1. <b>Party Night</b>");
    }

    @Test
    void registrationWithoutDateStart_omitsDateLine() {
        Session s = session();
        when(botApi.botMyRegistrations(100L)).thenReturn(List.of(registration("No Date Event", null)));

        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        handler.handle(ctx(), s);

        verify(telegramClient).sendHtml(eq(100L), captor.capture(), any());
        assertThat(captor.getValue()).contains("No Date Event");
        assertThat(captor.getValue()).doesNotContain("📆");
    }

    @Test
    void multipleRegistrations_areNumberedSequentially() {
        Session s = session();
        when(botApi.botMyRegistrations(100L)).thenReturn(List.of(
                registration("First", null),
                registration("Second", null)
        ));

        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        handler.handle(ctx(), s);

        verify(telegramClient).sendHtml(eq(100L), captor.capture(), any());
        assertThat(captor.getValue()).contains("1. <b>First</b>");
        assertThat(captor.getValue()).contains("2. <b>Second</b>");
    }

    @Test
    void eventNameWithHtml_isEscaped() {
        Session s = session();
        when(botApi.botMyRegistrations(100L)).thenReturn(List.of(registration("<b>Injected</b>", null)));

        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        handler.handle(ctx(), s);

        verify(telegramClient).sendHtml(eq(100L), captor.capture(), any());
        assertThat(captor.getValue()).contains("&lt;b&gt;Injected&lt;/b&gt;");
    }
}
