package com.lan.app.flows.cwlink;

import com.lan.app.client.baserow.model.CoworkingGuestResponse;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.registration.RegistrationSession;
import com.lan.app.flows.start.StartFlowDef;
import com.lan.app.i18n.I18n;
import com.lan.app.service.GuestService;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class CwLinkHandlerTest {

    @Inject
    CwLinkHandler handler;

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

    @Test
    void missingDeepLinkArgs_sendsInvalidAndRoutesToStartShow() {
        Session s = session();

        StepResult result = handler.handle(textCtx("/start"), s);

        assertThat(result).isEqualTo(StepResult.finish());
        assertThat(s.getFlow()).isEqualTo(StartFlowDef.FLOW);
        assertThat(s.getStep()).isEqualTo(StartFlowDef.STEP_SHOW);
        verify(telegramClient).sendHtml(eq(100L), any(), eq(null));
    }

    @Test
    void malformedUuidInDeepLink_sendsInvalidAndRoutesToStartShow() {
        Session s = session();

        StepResult result = handler.handle(textCtx("/start cwlink_not-a-uuid"), s);

        assertThat(result).isEqualTo(StepResult.finish());
        assertThat(s.getFlow()).isEqualTo(StartFlowDef.FLOW);
        assertThat(s.getStep()).isEqualTo(StartFlowDef.STEP_SHOW);
        verify(telegramClient).sendHtml(eq(100L), any(), eq(null));
    }

    @Test
    void chatIdConflict_sendsConflictMessageAndRoutesToStartShow() {
        Session s = session();
        UUID id = UUID.randomUUID();
        when(guestService.linkChatById(eq(id), eq(100L)))
            .thenReturn(new GuestService.LinkChatOutcome(GuestService.LinkChatResult.CHAT_ID_CONFLICT, null));

        StepResult result = handler.handle(textCtx("/start cwlink_" + id), s);

        assertThat(result).isEqualTo(StepResult.finish());
        assertThat(s.getFlow()).isEqualTo(StartFlowDef.FLOW);
        assertThat(s.getStep()).isEqualTo(StartFlowDef.STEP_SHOW);
        assertThat(RegistrationSession.isRegistered(s)).isFalse();
    }

    @Test
    void guestNotFound_sendsNotFoundMessageAndRoutesToStartShow() {
        Session s = session();
        UUID id = UUID.randomUUID();
        when(guestService.linkChatById(eq(id), eq(100L)))
            .thenReturn(new GuestService.LinkChatOutcome(GuestService.LinkChatResult.NOT_FOUND, null));

        StepResult result = handler.handle(textCtx("/start cwlink_" + id), s);

        assertThat(result).isEqualTo(StepResult.finish());
        assertThat(s.getFlow()).isEqualTo(StartFlowDef.FLOW);
        assertThat(s.getStep()).isEqualTo(StartFlowDef.STEP_SHOW);
        assertThat(RegistrationSession.isRegistered(s)).isFalse();
    }

    @Test
    void linkedViaLogin_marksRegisteredAndConfirmsLink() {
        Session s = session();
        UUID id = UUID.randomUUID();
        CoworkingGuestResponse guest = mock(CoworkingGuestResponse.class);
        when(guest.getId()).thenReturn(id);
        when(guest.getFirstName()).thenReturn("Ann");
        when(guestService.linkChatById(eq(id), eq(100L)))
            .thenReturn(new GuestService.LinkChatOutcome(GuestService.LinkChatResult.LINKED, guest));

        StepResult result = handler.handle(textCtx("/start cwlink_" + id), s);

        assertThat(result).isEqualTo(StepResult.finish());
        assertThat(s.getFlow()).isEqualTo(StartFlowDef.FLOW);
        assertThat(s.getStep()).isEqualTo(StartFlowDef.STEP_SHOW);
        assertThat(RegistrationSession.isRegistered(s)).isTrue();
        assertThat(RegistrationSession.getGuestId(s)).isEqualTo(id.toString());
        verify(guestService).confirmLink(id);
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }

    @Test
    void linkedViaSignup_marksRegisteredAndUsesSignupLangAndSource() {
        Session s = session();
        UUID id = UUID.randomUUID();
        CoworkingGuestResponse guest = mock(CoworkingGuestResponse.class);
        when(guest.getId()).thenReturn(id);
        when(guest.getFirstName()).thenReturn("Ann");
        when(guestService.linkChatById(eq(id), eq(100L)))
            .thenReturn(new GuestService.LinkChatOutcome(GuestService.LinkChatResult.LINKED, guest));

        StepResult result = handler.handle(textCtx("/start cwlink_" + id + "_en_signup"), s);

        assertThat(result).isEqualTo(StepResult.finish());
        assertThat(s.getLang()).isEqualTo("en");
        assertThat(RegistrationSession.isRegistered(s)).isTrue();
        verify(guestService).confirmLink(id);
    }
}
