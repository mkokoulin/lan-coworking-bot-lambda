package com.lan.app.flows.cwlink;

import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CwLinkFlowRegistrar {

    private final FlowRegistry registry;
    private final CwLinkHandler cwLinkHandler;

    @Inject
    public CwLinkFlowRegistrar(FlowRegistry registry, CwLinkHandler cwLinkHandler) {
        this.registry = registry;
        this.cwLinkHandler = cwLinkHandler;
    }

    public void register() {
        registry.registerStep(CwLinkFlowDef.FLOW, CwLinkFlowDef.STEP_LINK, cwLinkHandler);
    }
}
