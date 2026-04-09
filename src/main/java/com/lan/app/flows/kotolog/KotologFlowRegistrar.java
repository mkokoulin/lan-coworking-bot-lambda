package com.lan.app.flows.kotolog;

import com.lan.app.engine.FlowEntry;
import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class KotologFlowRegistrar {

    @Inject FlowRegistry registry;
    @Inject KotologHomeHandler homeHandler;
    @Inject KotologHelpHandler helpHandler;

    public void register() {
        registry.registerStep(KotologFlowDef.FLOW, KotologFlowDef.STEP_HOME, homeHandler);
        registry.registerStep(KotologFlowDef.FLOW, KotologFlowDef.STEP_HELP, helpHandler);

        registry.registerStep(KotologFlowDef.FLOW, KotologFlowDef.CB_HELP, helpHandler);
        registry.registerStep(KotologFlowDef.FLOW, KotologFlowDef.CB_HOME, homeHandler);

        FlowEntry home = new FlowEntry(KotologFlowDef.FLOW, KotologFlowDef.STEP_HOME);
        FlowEntry help = new FlowEntry(KotologFlowDef.FLOW, KotologFlowDef.STEP_HELP);

        registry.registerCommand("kotolog",      home);
        registry.registerCommand("cats",         home);
        registry.registerCommand("kotolog_help", help);
    }
}