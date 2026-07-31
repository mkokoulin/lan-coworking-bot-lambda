    package com.lan.app.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lan.app.domain.UpdateContext;
import com.lan.app.flows.eventconfirm.EventConfirmFlowDef;
import com.lan.app.flows.eventnotify.EventNotifyFlowDef;
import com.lan.app.flows.myevents.MyEventsFlowDef;
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
            String args = ctx.commandArgs();
            if ("start".equals(command) && args != null && args.startsWith("reg_")) {
                session.setFlow(EventConfirmFlowDef.FLOW);
                session.setStep(EventConfirmFlowDef.STEP_CONFIRM);
            } else if (command.startsWith(EventNotifyFlowDef.PREFIX_YES) || command.startsWith(EventNotifyFlowDef.PREFIX_NO)) {
                session.setFlow(EventNotifyFlowDef.FLOW);
                session.setStep(EventNotifyFlowDef.STEP_ACTION);
            } else if (command.startsWith(MyEventsFlowDef.CB_CANCEL_PFX)
                    || command.startsWith(MyEventsFlowDef.CB_CANCEL_YES_PFX)
                    || command.startsWith(MyEventsFlowDef.CB_CANCEL_NO_PFX)) {
                session.setFlow(MyEventsFlowDef.FLOW);
                session.setStep(MyEventsFlowDef.STEP_CANCEL_ACTION);
            } else if (command.startsWith(MyEventsFlowDef.CB_GUESTS_PFX)) {
                session.setFlow(MyEventsFlowDef.FLOW);
                session.setStep(MyEventsFlowDef.STEP_GUESTS_PROMPT);
            } else {
                FlowEntry entry = registry.getCommand(command).orElse(null);
                if (entry != null) {
                    session.setFlow(entry.flow());
                    session.setStep(entry.step());
                }
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