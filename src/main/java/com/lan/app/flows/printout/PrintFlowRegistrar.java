package com.lan.app.flows.printout;

import com.lan.app.engine.FlowEntry;
import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PrintFlowRegistrar {

    private final FlowRegistry registry;
    private final PrintPromptHandler promptHandler;
    private final PrintWaitDetailsHandler waitDetailsHandler;
    private final PrintWaitFileHandler waitFileHandler;

    @Inject
    public PrintFlowRegistrar(
            FlowRegistry registry,
            PrintPromptHandler promptHandler,
            PrintWaitDetailsHandler waitDetailsHandler,
            PrintWaitFileHandler waitFileHandler
    ) {
        this.registry = registry;
        this.promptHandler = promptHandler;
        this.waitDetailsHandler = waitDetailsHandler;
        this.waitFileHandler = waitFileHandler;
    }

    public void register() {
        registry.registerStep(PrintFlowDef.FLOW, PrintFlowDef.STEP_PROMPT,       promptHandler);
        registry.registerStep(PrintFlowDef.FLOW, PrintFlowDef.STEP_WAIT_DETAILS, waitDetailsHandler);
        registry.registerStep(PrintFlowDef.FLOW, PrintFlowDef.STEP_WAIT_FILE,    waitFileHandler);

        registry.registerCommand("printout",
                new FlowEntry(PrintFlowDef.FLOW, PrintFlowDef.STEP_PROMPT));
    }
}
