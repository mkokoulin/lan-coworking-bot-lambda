package com.lan.app.flows.eventnotify;

import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class EventNotifyFlowRegistrar {

    private final FlowRegistry registry;
    private final EventNotifyActionHandler actionHandler;

    @Inject
    public EventNotifyFlowRegistrar(FlowRegistry registry, EventNotifyActionHandler actionHandler) {
        this.registry = registry;
        this.actionHandler = actionHandler;
    }

    public void register() {
        registry.registerStep(EventNotifyFlowDef.FLOW, EventNotifyFlowDef.STEP_ACTION, actionHandler);
    }
}
