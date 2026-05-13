package com.lan.app.flows.myevents;

import com.lan.app.engine.FlowEntry;
import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MyEventsFlowRegistrar {

    private final FlowRegistry registry;
    private final MyEventsHandler handler;

    @Inject
    public MyEventsFlowRegistrar(FlowRegistry registry, MyEventsHandler handler) {
        this.registry = registry;
        this.handler = handler;
    }

    public void register() {
        registry.registerStep(MyEventsFlowDef.FLOW, MyEventsFlowDef.STEP_SHOW, handler);

        FlowEntry entry = new FlowEntry(MyEventsFlowDef.FLOW, MyEventsFlowDef.STEP_SHOW);
        registry.registerCommand("myevents", entry);
        registry.registerCommand("events", entry);
    }
}
