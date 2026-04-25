package com.lan.app.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lan.app.domain.IncomingUpdate;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.CommandRouter;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.start.StartFlowDef;
import com.lan.app.session.Session;
import com.lan.app.session.SessionRepository;
import com.lan.app.telegram.IncomingUpdateFactory;
import com.lan.app.telegram.dto.TelegramUpdate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class UpdateHandler {

    private static final Logger logger = LoggerFactory.getLogger(UpdateHandler.class);

    private final IncomingUpdateFactory incomingUpdateFactory;
    private final SessionRepository sessionRepository;
    private final CommandRouter commandRouter;

    @Inject
    public UpdateHandler(
        IncomingUpdateFactory incomingUpdateFactory,
        SessionRepository sessionRepository,
        CommandRouter commandRouter
    ) {
        this.incomingUpdateFactory = incomingUpdateFactory;
        this.sessionRepository = sessionRepository;
        this.commandRouter = commandRouter;
    }

    public void handle(TelegramUpdate rawUpdate) {
        IncomingUpdate update = incomingUpdateFactory.fromTelegram(rawUpdate);
        
        logger.info("Received update: {}", update);
        
        if (update == null || update.getUserId() == null || update.getChatId() == null) {
            logger.info("Skip update: invalid mapped update");
            return;
        }

        Session session = sessionRepository.findByUserId(update.getUserId())
                .orElseGet(() -> newSession(update));

        if (alreadyProcessed(session, update)) {
            logger.info("Skip update: already processed updateId={}", update.getUpdateId());
            return;
        }

        UpdateContext ctx = UpdateContext.fromIncomingUpdate(update);

        StepResult result = commandRouter.route(ctx, session);
        applyStepResult(session, result);

        session.setLastProcessedUpdateId(update.getUpdateId());
        sessionRepository.save(session);
    }

    private void applyStepResult(Session session, StepResult result) {
        if (result == null) {
            return;
        }

        if (result.nextFlow() != null) {
            session.setFlow(result.nextFlow());
        }

        if (result.nextStep() != null) {
            session.setStep(result.nextStep());
        }
    }

    private Session newSession(IncomingUpdate update) {
        Session session = new Session();
        session.setUserId(update.getUserId());
        session.setChatId(update.getChatId());
        session.setFlow(StartFlowDef.FLOW);
        session.setStep(StartFlowDef.STEP_SHOW);
        session.setPayloadJson("{}");
        session.setLang(detectLanguage(update.getUserLanguageCode()));
        return session;
    }

    private boolean alreadyProcessed(Session session, IncomingUpdate update) {
        return update.getUpdateId() != null
                && session.getLastProcessedUpdateId() != null
                && update.getUpdateId() <= session.getLastProcessedUpdateId();
    }

    private String detectLanguage(String code) {
        if (code == null || code.isBlank()) {
            return "ru";
        }
        if (code.startsWith("ru")) {
            return "ru";
        }
        if (code.startsWith("hy")) {
            return "hy";
        }
        return "en";
    }
}