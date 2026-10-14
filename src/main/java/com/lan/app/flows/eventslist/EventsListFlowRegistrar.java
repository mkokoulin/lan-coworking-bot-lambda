package com.lan.app.flows.eventslist;

import com.lan.app.engine.FlowEntry;
import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class EventsListFlowRegistrar {

    private final FlowRegistry registry;
    private final EventsListHandler listHandler;
    private final EventDetailHandler detailHandler;
    private final EventRegisterHandler registerHandler;
    private final FestivalDetailHandler festivalDetailHandler;

    @Inject
    public EventsListFlowRegistrar(
        FlowRegistry registry,
        EventsListHandler listHandler,
        EventDetailHandler detailHandler,
        EventRegisterHandler registerHandler,
        FestivalDetailHandler festivalDetailHandler
    ) {
        this.registry = registry;
        this.listHandler = listHandler;
        this.detailHandler = detailHandler;
        this.registerHandler = registerHandler;
        this.festivalDetailHandler = festivalDetailHandler;
    }

    public void register() {
        registry.registerStep(EventsListFlowDef.FLOW, EventsListFlowDef.STEP_LIST,     listHandler);
        registry.registerStep(EventsListFlowDef.FLOW, EventsListFlowDef.STEP_DETAIL,   detailHandler);
        registry.registerStep(EventsListFlowDef.FLOW, EventsListFlowDef.STEP_REGISTER, registerHandler);
        registry.registerStep(EventsListFlowDef.FLOW, EventsListFlowDef.STEP_FESTIVAL, festivalDetailHandler);

        // "events" command → show the events list (overrides MyEventsFlowRegistrar's "events" mapping)
        FlowEntry listEntry = new FlowEntry(EventsListFlowDef.FLOW, EventsListFlowDef.STEP_LIST);
        registry.registerCommand("events",     listEntry);
        registry.registerCommand("event_list", listEntry);
    }
}
