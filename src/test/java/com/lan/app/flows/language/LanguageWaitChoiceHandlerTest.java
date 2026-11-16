package com.lan.app.flows.language;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class LanguageWaitChoiceHandlerTest {

    @Inject
    LanguageWaitChoiceHandler handler;

    @InjectMock
    TelegramClient telegramClient;

    @InjectMock
    I18n i18n;

    @BeforeEach
    void stubTranslations() {
        when(i18n.t(any(), any())).thenReturn("translated");
    }

    private static Session session() {
        return Session.newDefault(100L, 200L);
    }

    private static UpdateContext callbackCtx(String data) {
        return new UpdateContext(100L, "private", 200L, null, null, data, "qid", true, "bob", null, null, null);
    }

    private static UpdateContext noCallbackCtx() {
        return new UpdateContext(100L, "private", 200L, null, "hello", null, null, false, "bob", null, null, null);
    }

    @Test
    void enChoice_setsLangEnAndFinishesFlow() {
        Session s = session();
        s.setLang("ru");

        StepResult result = handler.handle(callbackCtx(LanguageFlowDef.CB_EN), s);

        assertThat(result).isEqualTo(StepResult.finish());
        assertThat(s.getLang()).isEqualTo("en");
        assertThat(s.getFlow()).isEmpty();
        assertThat(s.getStep()).isEmpty();
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }

    @Test
    void ruChoice_setsLangRuAndFinishesFlow() {
        Session s = session();
        s.setLang("en");

        StepResult result = handler.handle(callbackCtx(LanguageFlowDef.CB_RU), s);

        assertThat(result).isEqualTo(StepResult.finish());
        assertThat(s.getLang()).isEqualTo("ru");
        assertThat(s.getFlow()).isEmpty();
        assertThat(s.getStep()).isEmpty();
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }

    @Test
    void unrecognizedCallbackPayload_staysOnWaitChoiceWithoutChangingLang() {
        Session s = session();
        s.setLang("ru");

        StepResult result = handler.handle(callbackCtx("lang:fr"), s);

        assertThat(result).isEqualTo(StepResult.stay(LanguageFlowDef.FLOW, LanguageFlowDef.STEP_WAIT_CHOICE));
        assertThat(s.getLang()).isEqualTo("ru");
        verify(telegramClient, never()).sendHtml(any(), any(), any());
    }

    @Test
    void noCallback_staysOnWaitChoiceWithoutSideEffects() {
        Session s = session();
        s.setLang("ru");

        StepResult result = handler.handle(noCallbackCtx(), s);

        assertThat(result).isEqualTo(StepResult.stay(LanguageFlowDef.FLOW, LanguageFlowDef.STEP_WAIT_CHOICE));
        assertThat(s.getLang()).isEqualTo("ru");
        verify(telegramClient, never()).sendHtml(any(), any(), any());
    }
}
