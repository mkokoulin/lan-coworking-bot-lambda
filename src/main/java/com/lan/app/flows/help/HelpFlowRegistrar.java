package com.lan.app.flows.help;

import com.lan.app.engine.FlowEntry;
import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class HelpFlowRegistrar {

    private final FlowRegistry registry;
    private final HelpHandler helpHandler;

    @Inject
    public HelpFlowRegistrar(FlowRegistry registry, HelpHandler helpHandler) {
        this.registry = registry;
        this.helpHandler = helpHandler;
    }

    public void register() {
        registry.registerStep(HelpFlowDef.FLOW, HelpFlowDef.STEP_SHOW, helpHandler);
        registry.registerCommand("help", new FlowEntry(HelpFlowDef.FLOW, HelpFlowDef.STEP_SHOW));
    }
}
