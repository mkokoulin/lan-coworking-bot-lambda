package com.lan.app.flows.eventsurvey;

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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@QuarkusTest
class EventSurveyRatingHandlerTest {

    @Inject
    EventSurveyRatingHandler handler;

    @InjectMock
    TelegramClient telegramClient;

    @InjectMock
    I18n i18n;

    @BeforeEach
    void setup() {
        lenient().when(i18n.t(any(), any())).thenReturn("translated");
    }

    private static Session session() {
        return Session.newDefault(100L, 200L);
    }

    private static UpdateContext callbackCtx(String data) {
        return new UpdateContext(100L, "private", 200L, 55, null, data, "q1", true, "bob", null, null, null);
    }

    @Test
    void validCallback_storesRatingAndIdsInSessionAndAsksForText() {
        Session s = session();

        StepResult result = handler.handle(callbackCtx("survey_rate_4_42_101_7"), s);

        assertThat(result).isEqualTo(StepResult.stay(EventSurveyFlowDef.FLOW, EventSurveyFlowDef.STEP_TEXT));
        assertThat(EventSurveySession.getRating(s)).isEqualTo("4");
        assertThat(EventSurveySession.getEventRowId(s)).isEqualTo("42");
        assertThat(EventSurveySession.getGuestRowId(s)).isEqualTo("101");
        assertThat(EventSurveySession.getRegistrationRowId(s)).isEqualTo("7");
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }

    @Test
    void nullCallbackData_returnsFinishWithoutSideEffects() {
        Session s = session();

        StepResult result = handler.handle(callbackCtx(null), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verifyNoInteractions(telegramClient);
    }

    @Test
    void nonMatchingPrefix_returnsFinishWithoutSideEffects() {
        Session s = session();

        StepResult result = handler.handle(callbackCtx("something_else"), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verifyNoInteractions(telegramClient);
    }

    @Test
    void malformedPayload_wrongPartCount_returnsFinish() {
        Session s = session();

        StepResult result = handler.handle(callbackCtx("survey_rate_4_42_101"), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verifyNoInteractions(telegramClient);
    }

    @Test
    void malformedPayload_nonNumeric_returnsFinish() {
        Session s = session();

        StepResult result = handler.handle(callbackCtx("survey_rate_x_42_101_7"), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verifyNoInteractions(telegramClient);
    }
}
