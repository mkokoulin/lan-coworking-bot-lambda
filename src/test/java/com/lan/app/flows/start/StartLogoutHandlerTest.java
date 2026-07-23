package com.lan.app.flows.start;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class StartLogoutHandlerTest {

    @Inject
    StartLogoutHandler handler;

    @InjectMock
    TelegramClient telegramClient;

    @InjectMock
    I18n i18n;

    @InjectMock
    GuestService guestService;

    @BeforeEach
    void stubTranslations() {
        when(i18n.t(any(), any())).thenReturn("translated");
    }

    private static Session session() {
        return Session.newDefault(100L, 200L);
    }

    private static UpdateContext ctx() {
        return new UpdateContext(100L, "private", 200L, 55, null, "logout", "q1", true, "bob", null, null, null);
    }

    @Test
    void handle_unlinksChatClearsAuthSetsManualLogoutAndRoutesToStartShow() {
        Session s = session();
        RegistrationSession.markRegistered(s);
        RegistrationSession.setGuestId(s, "guest-1");

        StepResult result = handler.handle(ctx(), s);

        assertThat(result).isEqualTo(StepResult.finish());
        assertThat(s.getFlow()).isEqualTo(StartFlowDef.FLOW);
        assertThat(s.getStep()).isEqualTo(StartFlowDef.STEP_SHOW);
        assertThat(RegistrationSession.isRegistered(s)).isFalse();
        assertThat(RegistrationSession.getGuestId(s)).isNull();
        assertThat(RegistrationSession.isManualLogout(s)).isTrue();
        verify(guestService).unlinkChat(100L);
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }
}
