package com.lan.app.flows.start;

import com.lan.app.client.baserow.model.CoworkingGuestResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class StartLoginPhoneHandlerTest {

    @Inject
    StartLoginPhoneHandler handler;

    @InjectMock
    TelegramClient telegramClient;

    @InjectMock
    I18n i18n;

    @InjectMock
    GuestService guestService;

    @BeforeEach
    void stubTranslations() {
        lenient().when(i18n.t(any(), any())).thenReturn("translated");
    }

    private static Session session() {
        return Session.newDefault(100L, 200L);
    }

    private static UpdateContext textCtx(String text) {
        return new UpdateContext(100L, "private", 200L, null, text, null, null, false, "bob", null, null, null);
    }

    private static UpdateContext sharedPhoneCtx(String phone) {
        return new UpdateContext(100L, "private", 200L, null, null, null, null, false, "bob", phone, null, null);
    }

    @Test
    void blankPhone_sendsEmptyErrorAndStaysOnLoginPhone() {
        Session s = session();

        StepResult result = handler.handle(textCtx(""), s);

        assertThat(result).isEqualTo(StepResult.stay(StartFlowDef.FLOW, StartFlowDef.STEP_LOGIN_PHONE));
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }

    @Test
    void invalidPhone_sendsInvalidErrorAndStaysOnLoginPhone() {
        Session s = session();

        StepResult result = handler.handle(textCtx("not a phone"), s);

        assertThat(result).isEqualTo(StepResult.stay(StartFlowDef.FLOW, StartFlowDef.STEP_LOGIN_PHONE));
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }

    @Test
    void validPhoneNotFound_sendsNotFoundAndStaysOnLoginPhone() {
        Session s = session();
        when(guestService.linkChat(eq("+37491123456"), eq(100L))).thenReturn(Optional.empty());

        StepResult result = handler.handle(textCtx("+37491123456"), s);

        assertThat(result).isEqualTo(StepResult.stay(StartFlowDef.FLOW, StartFlowDef.STEP_LOGIN_PHONE));
        assertThat(RegistrationSession.isRegistered(s)).isFalse();
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }

    @Test
    void validPhoneFound_marksRegisteredAndFinishesAtStartShow() {
        Session s = session();
        UUID id = UUID.randomUUID();
        CoworkingGuestResponse guest = mock(CoworkingGuestResponse.class);
        when(guest.getId()).thenReturn(id);
        when(guest.getFirstName()).thenReturn("Ann");
        when(guestService.linkChat(eq("+37491123456"), eq(100L))).thenReturn(Optional.of(guest));

        StepResult result = handler.handle(textCtx("+37491123456"), s);

        assertThat(result).isEqualTo(StepResult.finish());
        assertThat(s.getFlow()).isEqualTo(StartFlowDef.FLOW);
        assertThat(s.getStep()).isEqualTo(StartFlowDef.STEP_SHOW);
        assertThat(RegistrationSession.isRegistered(s)).isTrue();
        assertThat(RegistrationSession.getGuestId(s)).isEqualTo(id.toString());
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }

    @Test
    void sharedPhoneContact_takesPriorityOverMessageText() {
        Session s = session();
        UUID id = UUID.randomUUID();
        CoworkingGuestResponse guest = mock(CoworkingGuestResponse.class);
        when(guest.getId()).thenReturn(id);
        when(guest.getFirstName()).thenReturn("Ann");
        when(guestService.linkChat(eq("+37491123456"), eq(100L))).thenReturn(Optional.of(guest));

        StepResult result = handler.handle(sharedPhoneCtx("+37491123456"), s);

        assertThat(result).isEqualTo(StepResult.finish());
        assertThat(RegistrationSession.isRegistered(s)).isTrue();
    }
}
