package com.lan.app.flows.news;

import com.lan.app.engine.FlowEntry;
import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class NewsFlowRegistrar {

    @Inject FlowRegistry registry;
    @Inject NewsHandler newsHandler;

    public void register() {
        registry.registerStep(NewsFlowDef.FLOW, NewsFlowDef.STEP_LIST, newsHandler);
        registry.registerCommand("news", new FlowEntry(NewsFlowDef.FLOW, NewsFlowDef.STEP_LIST));
    }
}
