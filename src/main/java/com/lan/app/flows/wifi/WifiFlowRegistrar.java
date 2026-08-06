package com.lan.app.flows.wifi;

import com.lan.app.engine.FlowEntry;
import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class WifiFlowRegistrar {

    @Inject FlowRegistry registry;
    @Inject WifiHandler wifiHandler;

    public void register() {
        registry.registerStep(WifiFlowDef.FLOW, WifiFlowDef.STEP_SHOW, wifiHandler);
        registry.registerCommand("wifi", new FlowEntry(WifiFlowDef.FLOW, WifiFlowDef.STEP_SHOW));
    }
}
