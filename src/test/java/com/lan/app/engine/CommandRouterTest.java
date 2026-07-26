package com.lan.app.engine;

import com.lan.app.domain.UpdateContext;
import com.lan.app.flows.eventconfirm.EventConfirmFlowDef;
import com.lan.app.flows.eventnotify.EventNotifyFlowDef;
import com.lan.app.flows.start.StartFlowDef;
import com.lan.app.session.Session;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The bug this guards against: routing a reminder callback ("en_y_.../en_n_...") by an exact
 * match against FlowRegistry#commands would never work, since the callback_data carries dynamic
 * notification/guest/registration ids. CommandRouter must special-case the prefix instead — the
 * same way it already special-cases "start reg_...".
 */
class CommandRouterTest {

    private final FlowRegistry registry = new FlowRegistry();
    private final CommandRouter router = new CommandRouter(registry);

    private UpdateContext callback(String data) {
        return new UpdateContext(111L, "private", 111L, null, null, "/" + data, true, null, null);
    }

    private UpdateContext textCommand(String text) {
        return new UpdateContext(111L, "private", 111L, null, text, null, false, null, null);
    }

    @Test
    void routesYesCallbackToEventNotifyFlow() {
        registry.registerStep(EventNotifyFlowDef.FLOW, EventNotifyFlowDef.STEP_ACTION,
                (ctx, session) -> StepResult.finish());

        Session session = Session.newDefault(111L, 111L);
        router.route(callback("en_y_10_20_30"), session);

        assertEquals(EventNotifyFlowDef.FLOW, session.getFlow());
        assertEquals(EventNotifyFlowDef.STEP_ACTION, session.getStep());
    }

    @Test
    void routesNoCallbackToEventNotifyFlow() {
        registry.registerStep(EventNotifyFlowDef.FLOW, EventNotifyFlowDef.STEP_ACTION,
                (ctx, session) -> StepResult.finish());

        Session session = Session.newDefault(111L, 111L);
        router.route(callback("en_n_10_20_30"), session);

        assertEquals(EventNotifyFlowDef.FLOW, session.getFlow());
        assertEquals(EventNotifyFlowDef.STEP_ACTION, session.getStep());
    }

    @Test
    void unregisteredEventNotifyStepFallsBackToStartInsteadOfCrashing() {
        Session session = Session.newDefault(111L, 111L);
        router.route(callback("en_y_10_20_30"), session);

        assertEquals(StartFlowDef.FLOW, session.getFlow());
    }

    @Test
    void deepLinkRegistrationStillRoutesToEventConfirmFlow() {
        registry.registerStep(EventConfirmFlowDef.FLOW, EventConfirmFlowDef.STEP_CONFIRM,
                (ctx, session) -> StepResult.finish());

        Session session = Session.newDefault(111L, 111L);
        router.route(textCommand("/start reg_abc123_ru"), session);

        assertEquals(EventConfirmFlowDef.FLOW, session.getFlow());
        assertEquals(EventConfirmFlowDef.STEP_CONFIRM, session.getStep());
    }

    @Test
    void plainRegisteredCallbackCommandsStillRouteNormally() {
        registry.registerCommand("help", new FlowEntry("help_flow", "help_flow:show"));
        registry.registerStep("help_flow", "help_flow:show", (ctx, session) -> StepResult.finish());

        Session session = Session.newDefault(111L, 111L);
        router.route(callback("help"), session);

        assertEquals("help_flow", session.getFlow());
    }
}
