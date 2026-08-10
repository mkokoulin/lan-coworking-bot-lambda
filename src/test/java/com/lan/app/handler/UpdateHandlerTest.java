package com.lan.app.handler;

import com.lan.app.domain.IncomingUpdate;
import com.lan.app.engine.CommandRouter;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.start.StartFlowDef;
import com.lan.app.session.Session;
import com.lan.app.session.SessionRepository;
import com.lan.app.telegram.IncomingUpdateFactory;
import com.lan.app.telegram.dto.TelegramUpdate;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class UpdateHandlerTest {

    @Inject
    UpdateHandler updateHandler;

    @InjectMock
    IncomingUpdateFactory incomingUpdateFactory;

    @InjectMock
    SessionRepository sessionRepository;

    @InjectMock
    CommandRouter commandRouter;

    private static final TelegramUpdate RAW = new TelegramUpdate();

    private static IncomingUpdate update(Long userId, Long chatId) {
        IncomingUpdate u = new IncomingUpdate();
        u.setUserId(userId);
        u.setChatId(chatId);
        u.setUpdateId(1L);
        return u;
    }

    // ===== invalid/mapped-null updates are skipped =====

    @Test
    void nullMappedUpdate_isSkipped_noSessionOrRouterInteraction() {
        when(incomingUpdateFactory.fromTelegram(RAW)).thenReturn(null);

        updateHandler.handle(RAW);

        verify(sessionRepository, never()).findByUserId(any());
        verify(sessionRepository, never()).save(any());
        verify(commandRouter, never()).route(any(), any());
    }

    @Test
    void missingUserId_isSkipped() {
        IncomingUpdate u = update(null, 100L);
        when(incomingUpdateFactory.fromTelegram(RAW)).thenReturn(u);

        updateHandler.handle(RAW);

        verify(sessionRepository, never()).findByUserId(any());
        verify(commandRouter, never()).route(any(), any());
    }

    @Test
    void missingChatId_isSkipped() {
        IncomingUpdate u = update(200L, null);
        when(incomingUpdateFactory.fromTelegram(RAW)).thenReturn(u);

        updateHandler.handle(RAW);

        verify(sessionRepository, never()).findByUserId(any());
        verify(commandRouter, never()).route(any(), any());
    }

    // ===== new session creation =====

    @Test
    void noExistingSession_createsNewSessionWithDefaults() {
        IncomingUpdate u = update(200L, 100L);
        u.setUserLanguageCode("ru");
        when(incomingUpdateFactory.fromTelegram(RAW)).thenReturn(u);
        when(sessionRepository.findByUserId(200L)).thenReturn(Optional.empty());
        when(commandRouter.route(any(), any())).thenReturn(StepResult.finish());

        updateHandler.handle(RAW);

        var captor = org.mockito.ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(captor.capture());
        Session saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(200L);
        assertThat(saved.getChatId()).isEqualTo(100L);
        assertThat(saved.getFlow()).isEqualTo(StartFlowDef.FLOW);
        assertThat(saved.getStep()).isEqualTo(StartFlowDef.STEP_SHOW);
        assertThat(saved.getLang()).isEqualTo("ru");
        assertThat(saved.getLastProcessedUpdateId()).isEqualTo(1L);
    }

    // ===== stale chatId resync =====

    @Test
    void staleChatId_isResyncedFromUpdate() {
        IncomingUpdate u = update(200L, 999L);
        when(incomingUpdateFactory.fromTelegram(RAW)).thenReturn(u);
        Session existing = Session.newDefault(100L, 200L); // stale chatId=100
        when(sessionRepository.findByUserId(200L)).thenReturn(Optional.of(existing));
        when(commandRouter.route(any(), any())).thenReturn(StepResult.finish());

        updateHandler.handle(RAW);

        var captor = org.mockito.ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(captor.capture());
        assertThat(captor.getValue().getChatId()).isEqualTo(999L);
    }

    // ===== idempotency guard =====

    @Test
    void alreadyProcessedUpdateId_isSkipped_routerNeverCalled() {
        IncomingUpdate u = update(200L, 100L);
        u.setUpdateId(5L);
        when(incomingUpdateFactory.fromTelegram(RAW)).thenReturn(u);
        Session existing = Session.newDefault(100L, 200L);
        existing.setLastProcessedUpdateId(5L);
        when(sessionRepository.findByUserId(200L)).thenReturn(Optional.of(existing));

        updateHandler.handle(RAW);

        verify(commandRouter, never()).route(any(), any());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void updateIdGreaterThanLastProcessed_isProcessed() {
        IncomingUpdate u = update(200L, 100L);
        u.setUpdateId(6L);
        when(incomingUpdateFactory.fromTelegram(RAW)).thenReturn(u);
        Session existing = Session.newDefault(100L, 200L);
        existing.setLastProcessedUpdateId(5L);
        when(sessionRepository.findByUserId(200L)).thenReturn(Optional.of(existing));
        when(commandRouter.route(any(), any())).thenReturn(StepResult.finish());

        updateHandler.handle(RAW);

        verify(commandRouter).route(any(), any());
        verify(sessionRepository).save(any());
    }

    // ===== normal routing path applies StepResult =====

    @Test
    void routing_appliesNextFlowAndStepFromResult_andPersists() {
        IncomingUpdate u = update(200L, 100L);
        u.setUpdateId(10L);
        when(incomingUpdateFactory.fromTelegram(RAW)).thenReturn(u);
        Session existing = Session.newDefault(100L, 200L);
        when(sessionRepository.findByUserId(200L)).thenReturn(Optional.of(existing));
        when(commandRouter.route(any(), any())).thenReturn(StepResult.stay("registration", "registration:wait_name"));

        updateHandler.handle(RAW);

        var captor = org.mockito.ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(captor.capture());
        Session saved = captor.getValue();
        assertThat(saved.getFlow()).isEqualTo("registration");
        assertThat(saved.getStep()).isEqualTo("registration:wait_name");
        assertThat(saved.getLastProcessedUpdateId()).isEqualTo(10L);
    }

    @Test
    void routing_nullStepResult_doesNotChangeFlowOrStep_butStillPersists() {
        IncomingUpdate u = update(200L, 100L);
        u.setUpdateId(10L);
        when(incomingUpdateFactory.fromTelegram(RAW)).thenReturn(u);
        Session existing = Session.newDefault(100L, 200L);
        existing.setFlow("original");
        existing.setStep("original:step");
        when(sessionRepository.findByUserId(200L)).thenReturn(Optional.of(existing));
        when(commandRouter.route(any(), any())).thenReturn(null);

        updateHandler.handle(RAW);

        var captor = org.mockito.ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(captor.capture());
        Session saved = captor.getValue();
        assertThat(saved.getFlow()).isEqualTo("original");
        assertThat(saved.getStep()).isEqualTo("original:step");
    }

    // ===== detectLanguage branching (observed via lang on brand-new session) =====

    @Test
    void detectLanguage_ruPrefix_setsRu() {
        IncomingUpdate u = update(200L, 100L);
        u.setUserLanguageCode("ru-RU");
        when(incomingUpdateFactory.fromTelegram(RAW)).thenReturn(u);
        when(sessionRepository.findByUserId(200L)).thenReturn(Optional.empty());
        when(commandRouter.route(any(), any())).thenReturn(StepResult.finish());

        updateHandler.handle(RAW);

        var captor = org.mockito.ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(captor.capture());
        assertThat(captor.getValue().getLang()).isEqualTo("ru");
    }

    @Test
    void detectLanguage_hyPrefix_setsHy() {
        IncomingUpdate u = update(200L, 100L);
        u.setUserLanguageCode("hy-AM");
        when(incomingUpdateFactory.fromTelegram(RAW)).thenReturn(u);
        when(sessionRepository.findByUserId(200L)).thenReturn(Optional.empty());
        when(commandRouter.route(any(), any())).thenReturn(StepResult.finish());

        updateHandler.handle(RAW);

        var captor = org.mockito.ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(captor.capture());
        assertThat(captor.getValue().getLang()).isEqualTo("hy");
    }

    @Test
    void detectLanguage_otherPrefix_setsEn() {
        IncomingUpdate u = update(200L, 100L);
        u.setUserLanguageCode("en-US");
        when(incomingUpdateFactory.fromTelegram(RAW)).thenReturn(u);
        when(sessionRepository.findByUserId(200L)).thenReturn(Optional.empty());
        when(commandRouter.route(any(), any())).thenReturn(StepResult.finish());

        updateHandler.handle(RAW);

        var captor = org.mockito.ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(captor.capture());
        assertThat(captor.getValue().getLang()).isEqualTo("en");
    }

    @Test
    void detectLanguage_blankOrNull_defaultsToRu() {
        IncomingUpdate u = update(200L, 100L);
        u.setUserLanguageCode(null);
        when(incomingUpdateFactory.fromTelegram(RAW)).thenReturn(u);
        when(sessionRepository.findByUserId(200L)).thenReturn(Optional.empty());
        when(commandRouter.route(any(), any())).thenReturn(StepResult.finish());

        updateHandler.handle(RAW);

        var captor = org.mockito.ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(captor.capture());
        assertThat(captor.getValue().getLang()).isEqualTo("ru");
    }
}
