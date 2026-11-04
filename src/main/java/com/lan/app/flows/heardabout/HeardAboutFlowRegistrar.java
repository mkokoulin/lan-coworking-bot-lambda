package com.lan.app.flows.heardabout;

import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class HeardAboutFlowRegistrar {

    private final FlowRegistry registry;
    private final HeardAboutChoiceHandler choiceHandler;
    private final HeardAboutCommentHandler commentHandler;

    @Inject
    public HeardAboutFlowRegistrar(
        FlowRegistry registry,
        HeardAboutChoiceHandler choiceHandler,
        HeardAboutCommentHandler commentHandler
    ) {
        this.registry = registry;
        this.choiceHandler = choiceHandler;
        this.commentHandler = commentHandler;
    }

    // No registerCommand — this flow is only entered via the prefixed callback buttons that
    // HeardAboutScheduler sends proactively, routed by CommandRouter (see PREFIX_INSTAGRAM etc.).
    public void register() {
        registry.registerStep(HeardAboutFlowDef.FLOW, HeardAboutFlowDef.STEP_CHOICE, choiceHandler);
        registry.registerStep(HeardAboutFlowDef.FLOW, HeardAboutFlowDef.STEP_COMMENT, commentHandler);
    }
}
