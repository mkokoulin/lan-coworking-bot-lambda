package com.lan.app.engine;

import com.lan.app.domain.UpdateContext;
import com.lan.app.session.Session;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlowRegistryTest {

    private static final StepHandler NOOP = (ctx, session) -> StepResult.finish();

    @Test
    void getStep_returnsRegisteredHandler() {
        FlowRegistry registry = new FlowRegistry();
        registry.registerStep("flow", "step", NOOP);

        assertThat(registry.getStep("flow", "step")).contains(NOOP);
    }

    @Test
    void getStep_isMissing_returnsEmpty() {
        FlowRegistry registry = new FlowRegistry();

        assertThat(registry.getStep("unknown", "step")).isEmpty();
    }

    @Test
    void getStep_distinguishesFlowAndStepIndependently() {
        FlowRegistry registry = new FlowRegistry();
        registry.registerStep("flowA", "step1", NOOP);

        assertThat(registry.getStep("flowA", "step2")).isEmpty();
        assertThat(registry.getStep("flowB", "step1")).isEmpty();
    }

    @Test
    void getCommand_isCaseInsensitive() {
        FlowRegistry registry = new FlowRegistry();
        registry.registerCommand("Start", new FlowEntry("start", "start:show"));

        assertThat(registry.getCommand("start")).contains(new FlowEntry("start", "start:show"));
        assertThat(registry.getCommand("START")).contains(new FlowEntry("start", "start:show"));
    }

    @Test
    void getCommand_nullCommand_returnsEmpty() {
        FlowRegistry registry = new FlowRegistry();

        assertThat(registry.getCommand(null)).isEmpty();
    }

    @Test
    void getCommand_unregistered_returnsEmpty() {
        FlowRegistry registry = new FlowRegistry();

        assertThat(registry.getCommand("nope")).isEmpty();
    }

    @Test
    void handlerActuallyRuns() {
        FlowRegistry registry = new FlowRegistry();
        StepHandler handler = (ctx, session) -> StepResult.stay("flow", "next");
        registry.registerStep("flow", "step", handler);

        StepHandler resolved = registry.getStep("flow", "step").orElseThrow();
        StepResult result = resolved.handle(null, new Session());

        assertThat(result).isEqualTo(StepResult.stay("flow", "next"));
    }
}
