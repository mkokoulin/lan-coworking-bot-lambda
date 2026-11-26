package com.lan.app.flows.birthday;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.service.BirthdayWishService;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class BirthdayTextHandlerTest {

    @Inject
    BirthdayTextHandler handler;

    @InjectMock
    TelegramClient telegramClient;

    @InjectMock
    I18n i18n;

    @InjectMock
    BirthdayWishService birthdayWishService;

    @BeforeEach
    void setup() {
        lenient().when(i18n.t(any(), any())).thenReturn("translated");
        lenient().when(i18n.t(any(), eq("birthday_anonymous_label"))).thenReturn("Anonymous");
        lenient().when(birthdayWishService.createWish(anyString(), anyString(), anyString())).thenReturn(true);
    }

    private static Session session() {
        return Session.newDefault(100L, 200L);
    }

    private static UpdateContext textCtx(String text) {
        return new UpdateContext(100L, "private", 200L, null, text, null, null, false, "bob", null, null, null);
    }

    @Test
    void blankText_repromptsAndStaysOnText() {
        Session s = session();
        BirthdaySession.setName(s, "Ann");

        StepResult result = handler.handle(textCtx("   "), s);

        assertThat(result).isEqualTo(StepResult.stay(BirthdayFlowDef.FLOW, BirthdayFlowDef.STEP_TEXT));
        verify(birthdayWishService, never()).createWish(any(), any(), any());
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }

    @Test
    void withName_createsWishWithNameAndFinishesWithSuccessMessage() {
        Session s = session();
        BirthdaySession.setName(s, "Ann");

        StepResult result = handler.handle(textCtx("Happy birthday LAN!"), s);

        assertThat(result).isEqualTo(StepResult.finish());
        assertThat(s.getFlow()).isEmpty();
        assertThat(s.getStep()).isEmpty();
        assertThat(BirthdaySession.getName(s)).isNull();
        verify(birthdayWishService).createWish(eq("Ann"), eq("200"), eq("Happy birthday LAN!"));
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }

    @Test
    void noNameStored_usesAnonymousLabelAsAuthor() {
        Session s = session(); // no name stored → anonymous choice

        handler.handle(textCtx("So happy for you!"), s);

        verify(birthdayWishService).createWish(eq("Anonymous"), eq("200"), eq("So happy for you!"));
    }

    @Test
    void wishServiceFails_stillClearsSessionAndFinishesWithErrorMessage() {
        Session s = session();
        BirthdaySession.setName(s, "Ann");
        when(birthdayWishService.createWish(any(), any(), any())).thenReturn(false);

        StepResult result = handler.handle(textCtx("Nice!"), s);

        assertThat(result).isEqualTo(StepResult.finish());
        assertThat(s.getFlow()).isEmpty();
        assertThat(s.getStep()).isEmpty();
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }
}
