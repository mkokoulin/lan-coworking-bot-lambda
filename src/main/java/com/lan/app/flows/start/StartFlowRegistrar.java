package com.lan.app.flows.start;

import com.lan.app.engine.FlowEntry;
import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StartFlowRegistrar {

    private final FlowRegistry registry;
    private final StartShowHandler startShowHandler;

    @Inject
    public StartFlowRegistrar(
        FlowRegistry registry,
        StartShowHandler startShowHandler
    ) {
        this.registry = registry;
        this.startShowHandler = startShowHandler;
    }

    public void register() {
        registry.registerStep(StartFlowDef.FLOW, StartFlowDef.STEP_SHOW, startShowHandler);
        registry.registerCommand("start", new FlowEntry(StartFlowDef.FLOW, StartFlowDef.STEP_SHOW));
    }
}
