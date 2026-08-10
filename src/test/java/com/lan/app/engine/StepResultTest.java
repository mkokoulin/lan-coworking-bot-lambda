package com.lan.app.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StepResultTest {

    @Test
    void stay_capturesFlowAndStep() {
        StepResult result = StepResult.stay("flow", "step");

        assertThat(result.nextFlow()).isEqualTo("flow");
        assertThat(result.nextStep()).isEqualTo("step");
    }

    @Test
    void finish_hasNullFlowAndStep() {
        StepResult result = StepResult.finish();

        assertThat(result.nextFlow()).isNull();
        assertThat(result.nextStep()).isNull();
    }

    @Test
    void equality_isValueBased() {
        assertThat(StepResult.stay("a", "b")).isEqualTo(new StepResult("a", "b"));
        assertThat(StepResult.finish()).isEqualTo(new StepResult(null, null));
    }
}
