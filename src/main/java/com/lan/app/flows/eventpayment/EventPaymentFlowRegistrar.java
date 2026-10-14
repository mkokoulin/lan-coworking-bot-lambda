package com.lan.app.flows.eventpayment;

import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class EventPaymentFlowRegistrar {

    private final FlowRegistry registry;
    private final EventPaymentStartHandler startHandler;
    private final EventPaymentWaitPhotoHandler waitPhotoHandler;
    private final EventPaymentAdminHandler adminHandler;

    @Inject
    public EventPaymentFlowRegistrar(
            FlowRegistry registry,
            EventPaymentStartHandler startHandler,
            EventPaymentWaitPhotoHandler waitPhotoHandler,
            EventPaymentAdminHandler adminHandler
    ) {
        this.registry = registry;
        this.startHandler = startHandler;
        this.waitPhotoHandler = waitPhotoHandler;
        this.adminHandler = adminHandler;
    }

    public void register() {
        registry.registerStep(EventPaymentFlowDef.FLOW, EventPaymentFlowDef.STEP_START, startHandler);
        registry.registerStep(EventPaymentFlowDef.FLOW, EventPaymentFlowDef.STEP_WAIT_PHOTO, waitPhotoHandler);
        registry.registerStep(EventPaymentFlowDef.FLOW, EventPaymentFlowDef.STEP_ADMIN, adminHandler);
    }
}
