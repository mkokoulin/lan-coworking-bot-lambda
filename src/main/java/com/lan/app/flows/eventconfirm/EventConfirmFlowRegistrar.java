package com.lan.app.flows.eventconfirm;

import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class EventConfirmFlowRegistrar {

    private final FlowRegistry registry;
    private final EventConfirmHandler confirmHandler;

    @Inject
    public EventConfirmFlowRegistrar(FlowRegistry registry, EventConfirmHandler confirmHandler) {
        this.registry = registry;
        this.confirmHandler = confirmHandler;
    }

    public void register() {
        registry.registerStep(EventConfirmFlowDef.FLOW, EventConfirmFlowDef.STEP_CONFIRM, confirmHandler);
    }
}
