package com.lan.app.flows.tariffs;

import com.lan.app.engine.FlowEntry;
import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TariffsFlowRegistrar {

    private final FlowRegistry registry;
    private final TariffsHandler tariffsHandler;

    @Inject
    public TariffsFlowRegistrar(FlowRegistry registry, TariffsHandler tariffsHandler) {
        this.registry = registry;
        this.tariffsHandler = tariffsHandler;
    }

    public void register() {
        registry.registerStep(TariffsFlowDef.FLOW, TariffsFlowDef.STEP_LIST, tariffsHandler);
        registry.registerCommand("tariffs", new FlowEntry(TariffsFlowDef.FLOW, TariffsFlowDef.STEP_LIST));
    }
}
