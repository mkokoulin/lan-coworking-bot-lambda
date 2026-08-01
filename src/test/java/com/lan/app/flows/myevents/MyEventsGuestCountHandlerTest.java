package com.lan.app.flows.myevents;

import com.lan.app.client.baserow.api.BotApi;
import com.lan.app.client.baserow.model.BotRegistrationActionResponse;
import com.lan.app.client.baserow.model.GuestCountUpdateRequest;
import com.lan.app.domain.IncomingUpdate;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
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

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class MyEventsGuestCountHandlerTest {

    @Inject
    MyEventsGuestCountHandler handler;

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

    private static UpdateContext callbackCtx(String data) {
        IncomingUpdate u = new IncomingUpdate();
        u.setChatId(100L);
        u.setUserId(200L);
        u.setCallbackData(data);
        return UpdateContext.fromIncomingUpdate(u);
    }

    private static UpdateContext textCtx(String text) {
        IncomingUpdate u = new IncomingUpdate();
        u.setChatId(100L);
        u.setUserId(200L);
        u.setText(text);
        return UpdateContext.fromIncomingUpdate(u);
    }

    private static BotRegistrationActionResponse actionResponse(int previous, int updated) {
        var r = new BotRegistrationActionResponse();
        r.setEventName("Party Night");
        r.setDateStart(OffsetDateTime.now().plusDays(10));
        r.setPreviousGuestCount(previous);
        r.setGuestCount(updated);
        r.setGuestFirstName("Ann");
        r.setGuestLastName("Smith");
        return r;
    }

    @Test
    void callback_withRegId_storesItAndPromptsForNumber() {
        Session s = session();

        StepResult result = handler.handle(callbackCtx(MyEventsFlowDef.CB_GUEST_COUNT_PFX + "reg-1"), s);

        assertThat(result).isEqualTo(StepResult.stay(MyEventsFlowDef.FLOW, MyEventsFlowDef.STEP_GUEST_COUNT_WAIT));
        assertThat(MyEventsSession.getPendingRegId(s)).isEqualTo("reg-1");
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }

    @Test
    void nonNumericText_sendsInvalidMessageAndStays() {
        Session s = session();
        MyEventsSession.setPendingRegId(s, "reg-1");

        StepResult result = handler.handle(textCtx("abc"), s);

        assertThat(result).isEqualTo(StepResult.stay(MyEventsFlowDef.FLOW, MyEventsFlowDef.STEP_GUEST_COUNT_WAIT));
        verify(i18n).t(eq("ru"), eq("myevents_guests_invalid"));
    }

    @Test
    void zeroOrNegative_sendsInvalidMessageAndStays() {
        Session s = session();
        MyEventsSession.setPendingRegId(s, "reg-1");

        handler.handle(textCtx("0"), s);

        verify(i18n).t(eq("ru"), eq("myevents_guests_invalid"));
    }

    @Test
    void validNumber_updatesGuestCountAndNotifiesAdmin() {
        Session s = session();
        MyEventsSession.setPendingRegId(s, "reg-1");
        when(botApi.botUpdateRegistrationGuestCount(eq("reg-1"), any())).thenReturn(actionResponse(2, 4));
        when(botApi.botMyRegistrations(100L)).thenReturn(java.util.List.of());

        StepResult result = handler.handle(textCtx("4"), s);

        assertThat(result).isEqualTo(StepResult.finish());
        var captor = org.mockito.ArgumentCaptor.forClass(GuestCountUpdateRequest.class);
        verify(botApi).botUpdateRegistrationGuestCount(eq("reg-1"), captor.capture());
        assertThat(captor.getValue().getGuestCount()).isEqualTo(4);
        verify(i18n).t(eq("ru"), eq("myevents_guests_success"));
        verify(telegramClient).sendHtml(eq(999999L), any(), any());
        assertThat(MyEventsSession.getPendingRegId(s)).isNull();
    }

    @Test
    void capacityConflict_sendsCapacityMessageAndStays() {
        Session s = session();
        MyEventsSession.setPendingRegId(s, "reg-1");
        when(botApi.botUpdateRegistrationGuestCount(eq("reg-1"), any()))
                .thenThrow(new WebApplicationException(Response.status(409).build()));

        StepResult result = handler.handle(textCtx("10"), s);

        assertThat(result).isEqualTo(StepResult.stay(MyEventsFlowDef.FLOW, MyEventsFlowDef.STEP_GUEST_COUNT_WAIT));
        verify(i18n).t(eq("ru"), eq("myevents_guests_capacity"));
    }

    @Test
    void otherHttpError_sendsGenericErrorAndRedisplaysList() {
        Session s = session();
        MyEventsSession.setPendingRegId(s, "reg-1");
        when(botApi.botUpdateRegistrationGuestCount(eq("reg-1"), any()))
                .thenThrow(new WebApplicationException(Response.status(500).build()));
        when(botApi.botMyRegistrations(100L)).thenReturn(java.util.List.of());

        handler.handle(textCtx("4"), s);

        verify(i18n).t(eq("ru"), eq("myevents_error"));
        assertThat(MyEventsSession.getPendingRegId(s)).isNull();
    }

    @Test
    void noPendingRegId_redisplaysListWithoutCallingApi() {
        Session s = session();
        when(botApi.botMyRegistrations(100L)).thenReturn(java.util.List.of());

        handler.handle(textCtx("4"), s);

        verify(botApi, org.mockito.Mockito.never()).botUpdateRegistrationGuestCount(anyString(), any());
        verify(botApi).botMyRegistrations(100L);
    }
}
