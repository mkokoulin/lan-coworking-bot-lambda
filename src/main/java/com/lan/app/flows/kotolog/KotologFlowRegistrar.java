package com.lan.app.flows.kotolog;

import com.lan.app.engine.FlowEntry;
import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class KotologFlowRegistrar {

    private final FlowRegistry registry;
    private final KotologHomeHandler homeHandler;
    private final KotologHelpHandler helpHandler;

    @Inject
    public KotologFlowRegistrar(
        FlowRegistry registry,
        KotologHomeHandler homeHandler,
        KotologHelpHandler helpHandler
    ) {
        this.registry = registry;
        this.homeHandler = homeHandler;
        this.helpHandler = helpHandler;
    }

    public void register() {
        registry.registerStep(KotologFlowDef.FLOW, KotologFlowDef.STEP_HOME, homeHandler);
        registry.registerStep(KotologFlowDef.FLOW, KotologFlowDef.STEP_HELP, helpHandler);

        FlowEntry home = new FlowEntry(KotologFlowDef.FLOW, KotologFlowDef.STEP_HOME);
        FlowEntry help = new FlowEntry(KotologFlowDef.FLOW, KotologFlowDef.STEP_HELP);

        registry.registerCommand("kotolog", home);
        registry.registerCommand("kotolog_help", help);
        registry.registerCommand("kotolog:help", help);
        registry.registerCommand("kotolog:home", home);
    }
}