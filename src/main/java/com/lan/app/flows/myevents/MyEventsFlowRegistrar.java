package com.lan.app.flows.myevents;

import com.lan.app.engine.FlowEntry;
import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MyEventsFlowRegistrar {

    private final FlowRegistry registry;
    private final MyEventsListHandler listHandler;
    private final MyEventsCancelActionHandler cancelActionHandler;
    private final MyEventsGuestsPromptHandler guestsPromptHandler;
    private final MyEventsGuestsWaitHandler guestsWaitHandler;

    @Inject
    public MyEventsFlowRegistrar(
        FlowRegistry registry,
        MyEventsListHandler listHandler,
        MyEventsCancelActionHandler cancelActionHandler,
        MyEventsGuestsPromptHandler guestsPromptHandler,
        MyEventsGuestsWaitHandler guestsWaitHandler
    ) {
        this.registry = registry;
        this.listHandler = listHandler;
        this.cancelActionHandler = cancelActionHandler;
        this.guestsPromptHandler = guestsPromptHandler;
        this.guestsWaitHandler = guestsWaitHandler;
    }

    public void register() {
        registry.registerStep(MyEventsFlowDef.FLOW, MyEventsFlowDef.STEP_LIST, listHandler);
        registry.registerStep(MyEventsFlowDef.FLOW, MyEventsFlowDef.STEP_CANCEL_ACTION, cancelActionHandler);
        registry.registerStep(MyEventsFlowDef.FLOW, MyEventsFlowDef.STEP_GUESTS_PROMPT, guestsPromptHandler);
        registry.registerStep(MyEventsFlowDef.FLOW, MyEventsFlowDef.STEP_GUESTS_WAIT, guestsWaitHandler);

        registry.registerCommand("myevents", new FlowEntry(MyEventsFlowDef.FLOW, MyEventsFlowDef.STEP_LIST));
        // "events" is handled by EventsListFlowRegistrar (browse + register flow)
    }
}
