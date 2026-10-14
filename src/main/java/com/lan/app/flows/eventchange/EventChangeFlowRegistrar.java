package com.lan.app.flows.eventchange;

import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class EventChangeFlowRegistrar {

    private final FlowRegistry registry;
    private final EventChangeHandler waitMessageHandler;
    private final EventChangeMenuHandler menuHandler;

    @Inject
    public EventChangeFlowRegistrar(
        FlowRegistry registry,
        EventChangeHandler waitMessageHandler,
        EventChangeMenuHandler menuHandler
    ) {
        this.registry = registry;
        this.waitMessageHandler = waitMessageHandler;
        this.menuHandler = menuHandler;
    }

    public void register() {
        registry.registerStep(EventChangeFlowDef.FLOW, EventChangeFlowDef.STEP_WAIT_MESSAGE, waitMessageHandler);
        registry.registerStep(EventChangeFlowDef.FLOW, EventChangeFlowDef.STEP_MENU, menuHandler);
    }
}
