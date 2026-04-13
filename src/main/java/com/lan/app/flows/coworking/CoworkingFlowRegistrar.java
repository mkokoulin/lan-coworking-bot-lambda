package com.lan.app.flows.coworking;

import com.lan.app.engine.FlowEntry;
import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CoworkingFlowRegistrar {

    private final FlowRegistry registry;
    private final CoworkingHomeHandler coworkingHomeHandler;

    @Inject
    public CoworkingFlowRegistrar(
        FlowRegistry registry,
        CoworkingHomeHandler coworkingHomeHandler
    ) {
        this.registry = registry;
        this.coworkingHomeHandler = coworkingHomeHandler;
    }

    public void register() {
        registry.registerStep(CoworkingFlowDef.FLOW, CoworkingFlowDef.STEP_HOME, coworkingHomeHandler);
        registry.registerCommand("coworking", new FlowEntry(CoworkingFlowDef.FLOW, CoworkingFlowDef.STEP_HOME));
    }
}