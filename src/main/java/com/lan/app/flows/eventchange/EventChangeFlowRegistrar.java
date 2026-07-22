package com.lan.app.flows.eventchange;

import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class EventChangeFlowRegistrar {

    private final FlowRegistry registry;
    private final EventChangeHandler waitMessageHandler;

    @Inject
    public EventChangeFlowRegistrar(FlowRegistry registry, EventChangeHandler waitMessageHandler) {
        this.registry = registry;
        this.waitMessageHandler = waitMessageHandler;
    }

    public void register() {
        registry.registerStep(EventChangeFlowDef.FLOW, EventChangeFlowDef.STEP_WAIT_MESSAGE, waitMessageHandler);
    }
}
