package com.lan.app.flows.menu;

import com.lan.app.engine.FlowEntry;
import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MenuFlowRegistrar {

    private final FlowRegistry registry;
    private final MenuHandler menuHandler;

    @Inject
    public MenuFlowRegistrar(FlowRegistry registry, MenuHandler menuHandler) {
        this.registry = registry;
        this.menuHandler = menuHandler;
    }

    public void register() {
        registry.registerStep(MenuFlowDef.FLOW, MenuFlowDef.STEP_SHOW, menuHandler);
        registry.registerCommand("menu", new FlowEntry(MenuFlowDef.FLOW, MenuFlowDef.STEP_SHOW));
    }
}
