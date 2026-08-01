package com.lan.app.flows.myevents;

import com.lan.app.client.baserow.api.BotApi;
import com.lan.app.client.baserow.model.BotRegistrationActionResponse;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class MyEventsCancelHandlerTest {

    @Inject
    MyEventsCancelHandler handler;

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

    private static BotRegistrationActionResponse actionResponse() {
        var r = new BotRegistrationActionResponse();
        r.setEventName("Party Night");
        r.setDateStart(OffsetDateTime.now().plusDays(10));
        r.setPreviousGuestCount(2);
        r.setGuestCount(2);
        r.setGuestFirstName("Ann");
        r.setGuestLastName("Smith");
        return r;
    }

    @Test
    void confirmPrefix_showsYesNoPromptAndStaysOnStep() {
        Session s = session();

        StepResult result = handler.handle(callbackCtx(MyEventsFlowDef.CB_CANCEL_PFX + "reg-1"), s);

        assertThat(result).isEqualTo(StepResult.stay(MyEventsFlowDef.FLOW, MyEventsFlowDef.STEP_CANCEL_CONFIRM));
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }

    @Test
    void noPrefix_sendsAbortedMessageAndRedisplaysList() {
        Session s = session();
        when(botApi.botMyRegistrations(100L)).thenReturn(java.util.List.of());

        handler.handle(callbackCtx(MyEventsFlowDef.CB_CANCEL_NO_PFX + "reg-1"), s);

        verify(i18n).t(eq("ru"), eq("myevents_cancel_aborted"));
        verify(botApi).botMyRegistrations(100L);
    }

    @Test
    void yesPrefix_success_notifiesAdminAndUser() {
        Session s = session();
        when(botApi.botCancelRegistration("reg-1")).thenReturn(actionResponse());
        when(botApi.botMyRegistrations(100L)).thenReturn(java.util.List.of());

        handler.handle(callbackCtx(MyEventsFlowDef.CB_CANCEL_YES_PFX + "reg-1"), s);

        verify(i18n).t(eq("ru"), eq("myevents_cancel_success"));
        verify(telegramClient).sendHtml(eq(999999L), any(), any());
    }

    @Test
    void yesPrefix_conflict_sendsConflictMessage() {
        Session s = session();
        when(botApi.botCancelRegistration("reg-1"))
                .thenThrow(new WebApplicationException(Response.status(409).build()));
        when(botApi.botMyRegistrations(100L)).thenReturn(java.util.List.of());

        handler.handle(callbackCtx(MyEventsFlowDef.CB_CANCEL_YES_PFX + "reg-1"), s);

        verify(i18n).t(eq("ru"), eq("myevents_cancel_conflict"));
    }

    @Test
    void yesPrefix_otherHttpError_sendsGenericError() {
        Session s = session();
        when(botApi.botCancelRegistration("reg-1"))
                .thenThrow(new WebApplicationException(Response.status(500).build()));
        when(botApi.botMyRegistrations(100L)).thenReturn(java.util.List.of());

        handler.handle(callbackCtx(MyEventsFlowDef.CB_CANCEL_YES_PFX + "reg-1"), s);

        verify(i18n).t(eq("ru"), eq("myevents_error"));
    }

    @Test
    void yesPrefix_genericException_sendsGenericError() {
        Session s = session();
        when(botApi.botCancelRegistration("reg-1")).thenThrow(new RuntimeException("boom"));
        when(botApi.botMyRegistrations(100L)).thenReturn(java.util.List.of());

        handler.handle(callbackCtx(MyEventsFlowDef.CB_CANCEL_YES_PFX + "reg-1"), s);

        verify(i18n).t(eq("ru"), eq("myevents_error"));
    }
}
