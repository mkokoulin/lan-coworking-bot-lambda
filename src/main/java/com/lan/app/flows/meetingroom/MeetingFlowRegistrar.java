package com.lan.app.flows.meetingroom;

import com.lan.app.engine.FlowEntry;
import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MeetingFlowRegistrar {

    private final FlowRegistry registry;
    private final MeetingPromptHandler promptHandler;
    private final MeetingWaitDateHandler waitDateHandler;
    private final MeetingWaitStartHandler waitStartHandler;
    private final MeetingWaitEndHandler waitEndHandler;
    private final MeetingWaitContactHandler waitContactHandler;

    @Inject
    public MeetingFlowRegistrar(
        FlowRegistry registry,
        MeetingPromptHandler promptHandler,
        MeetingWaitDateHandler waitDateHandler,
        MeetingWaitStartHandler waitStartHandler,
        MeetingWaitEndHandler waitEndHandler,
        MeetingWaitContactHandler waitContactHandler
    ) {
        this.registry = registry;
        this.promptHandler = promptHandler;
        this.waitDateHandler = waitDateHandler;
        this.waitStartHandler = waitStartHandler;
        this.waitEndHandler = waitEndHandler;
        this.waitContactHandler = waitContactHandler;
    }

    public void register() {
        registry.registerStep(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_PROMPT,       promptHandler);
        registry.registerStep(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_DATE,    waitDateHandler);
        registry.registerStep(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_START,   waitStartHandler);
        registry.registerStep(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_END,     waitEndHandler);
        registry.registerStep(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_WAIT_CONTACT, waitContactHandler);

        registry.registerCommand("meetingroom",
                new FlowEntry(MeetingFlowDef.FLOW, MeetingFlowDef.STEP_PROMPT));
    }
}