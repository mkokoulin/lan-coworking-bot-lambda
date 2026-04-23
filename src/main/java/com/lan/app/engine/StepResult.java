package com.lan.app.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record StepResult(String nextFlow, String nextStep) {

    private static final Logger logger = LoggerFactory.getLogger(StepResult.class);

    public static StepResult stay(String flow, String step) {
        logger.info("Staying in flow {}, step {}", flow, step);
        
        return new StepResult(flow, step);
    }

    public static StepResult finish() {
        logger.info("Finishing step");
        return new StepResult(null, null);
    }
}
