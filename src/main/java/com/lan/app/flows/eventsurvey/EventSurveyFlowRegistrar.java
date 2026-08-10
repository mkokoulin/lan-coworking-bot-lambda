package com.lan.app.flows.eventsurvey;

import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** No registerCommand — this flow is purely proactive, entered only via the survey_rate_ callback. */
@ApplicationScoped
public class EventSurveyFlowRegistrar {

    private final FlowRegistry registry;
    private final EventSurveyRatingHandler eventSurveyRatingHandler;
    private final EventSurveyTextHandler eventSurveyTextHandler;

    @Inject
    public EventSurveyFlowRegistrar(
        FlowRegistry registry,
        EventSurveyRatingHandler eventSurveyRatingHandler,
        EventSurveyTextHandler eventSurveyTextHandler
    ) {
        this.registry = registry;
        this.eventSurveyRatingHandler = eventSurveyRatingHandler;
        this.eventSurveyTextHandler = eventSurveyTextHandler;
    }

    public void register() {
        registry.registerStep(EventSurveyFlowDef.FLOW, EventSurveyFlowDef.STEP_RATING, eventSurveyRatingHandler);
        registry.registerStep(EventSurveyFlowDef.FLOW, EventSurveyFlowDef.STEP_TEXT, eventSurveyTextHandler);
    }
}
