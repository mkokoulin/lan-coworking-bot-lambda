package com.lan.app.flows.review;

import com.lan.app.engine.FlowEntry;
import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ReviewFlowRegistrar {

    private final FlowRegistry registry;
    private final ReviewRatingHandler reviewRatingHandler;
    private final ReviewTextHandler reviewTextHandler;

    @Inject
    public ReviewFlowRegistrar(
        FlowRegistry registry,
        ReviewRatingHandler reviewRatingHandler,
        ReviewTextHandler reviewTextHandler
    ) {
        this.registry = registry;
        this.reviewRatingHandler = reviewRatingHandler;
        this.reviewTextHandler = reviewTextHandler;
    }

    public void register() {
        registry.registerStep(ReviewFlowDef.FLOW, ReviewFlowDef.STEP_RATING, reviewRatingHandler);
        registry.registerStep(ReviewFlowDef.FLOW, ReviewFlowDef.STEP_TEXT, reviewTextHandler);
        registry.registerCommand("review", new FlowEntry(ReviewFlowDef.FLOW, ReviewFlowDef.STEP_RATING));
    }
}
