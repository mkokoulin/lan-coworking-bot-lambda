package com.lan.app.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lan.app.domain.UpdateContext;
import com.lan.app.flows.start.StartFlowDef;
import com.lan.app.session.Session;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CommandRouter {

    private static final Logger logger = LoggerFactory.getLogger(CommandRouter.class);
    private final FlowRegistry registry;

    @Inject
    public CommandRouter(FlowRegistry registry) {
        this.registry = registry;
    }

    public StepResult route(UpdateContext ctx, Session session) {
        String command = normalizeCommand(ctx.command());

        logger.info(
            "Routing command: '{}', session: {}, chatId: {}", 
            command != null ? command : "<none>",
            session,
            session.getChatId()
        );

        if (command != null) {
            FlowEntry entry = registry.getCommand(command).orElse(null);
            if (entry != null) {
                session.setFlow(entry.flow());
                session.setStep(entry.step());
            }
        }

        if (isBlank(session.getFlow()) || isBlank(session.getStep())) {
            session.setFlow(StartFlowDef.FLOW);
            session.setStep(StartFlowDef.STEP_DONE);
        }

        StepHandler handler = registry.getStep(session.getFlow(), session.getStep()).orElse(null);
        if (handler == null) {
            session.setFlow(StartFlowDef.FLOW);
            session.setStep(StartFlowDef.STEP_SHOW);
            handler = registry.getStep(session.getFlow(), session.getStep()).orElse(null);
            if (handler == null) {
                return StepResult.finish();
            }
        }

        return handler.handle(ctx, session);
    }

    private String normalizeCommand(String command) {
        if (isBlank(command)) {
            return null;
        }

        String raw = command.trim();

        int atIdx = raw.indexOf('@');
        if (atIdx >= 0) {
            raw = raw.substring(0, atIdx);
        }

        return raw.trim().toLowerCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}