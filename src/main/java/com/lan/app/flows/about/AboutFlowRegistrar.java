package com.lan.app.flows.about;

import com.lan.app.engine.FlowEntry;
import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AboutFlowRegistrar {

    @Inject FlowRegistry registry;
    @Inject AboutHandler aboutHandler;

    public void register() {
        registry.registerStep(AboutFlowDef.FLOW, AboutFlowDef.STEP_SEND, aboutHandler);
        registry.registerCommand("about", new FlowEntry(AboutFlowDef.FLOW, AboutFlowDef.STEP_SEND));
    }
}
