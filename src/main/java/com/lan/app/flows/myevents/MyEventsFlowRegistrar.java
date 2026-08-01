package com.lan.app.flows.myevents;

import com.lan.app.engine.FlowEntry;
import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MyEventsFlowRegistrar {

    private final FlowRegistry registry;
    private final MyEventsHandler handler;
    private final MyEventsCancelHandler cancelHandler;
    private final MyEventsGuestCountHandler guestCountHandler;

    @Inject
    public MyEventsFlowRegistrar(
        FlowRegistry registry,
        MyEventsHandler handler,
        MyEventsCancelHandler cancelHandler,
        MyEventsGuestCountHandler guestCountHandler
    ) {
        this.registry = registry;
        this.handler = handler;
        this.cancelHandler = cancelHandler;
        this.guestCountHandler = guestCountHandler;
    }

    public void register() {
        registry.registerStep(MyEventsFlowDef.FLOW, MyEventsFlowDef.STEP_SHOW, handler);
        registry.registerStep(MyEventsFlowDef.FLOW, MyEventsFlowDef.STEP_CANCEL_CONFIRM, cancelHandler);
        registry.registerStep(MyEventsFlowDef.FLOW, MyEventsFlowDef.STEP_GUEST_COUNT_WAIT, guestCountHandler);

        FlowEntry entry = new FlowEntry(MyEventsFlowDef.FLOW, MyEventsFlowDef.STEP_SHOW);
        registry.registerCommand("myevents", entry);
        // "events" is now handled by EventsListFlowRegistrar (browse + register flow)
    }
}
