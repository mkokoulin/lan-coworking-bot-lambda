package com.lan.app.flows.cwbooking;

import com.lan.app.engine.FlowEntry;
import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CwBookingFlowRegistrar {

    private final FlowRegistry registry;
    private final CwBookingConfirmHandler confirmHandler;

    @Inject
    public CwBookingFlowRegistrar(FlowRegistry registry, CwBookingConfirmHandler confirmHandler) {
        this.registry = registry;
        this.confirmHandler = confirmHandler;
    }

    public void register() {
        registry.registerStep(CwBookingFlowDef.FLOW, CwBookingFlowDef.STEP_CONFIRM, confirmHandler);
        registry.registerCommand("booking", new FlowEntry(CwBookingFlowDef.FLOW, CwBookingFlowDef.STEP_CONFIRM));
    }
}
