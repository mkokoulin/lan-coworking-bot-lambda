package com.lan.app.flows.birthday;

import com.lan.app.engine.FlowEntry;
import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BirthdayFlowRegistrar {

    private final FlowRegistry registry;
    private final BirthdayHomeHandler homeHandler;
    private final BirthdayNameHandler nameHandler;
    private final BirthdayTextHandler textHandler;

    @Inject
    public BirthdayFlowRegistrar(
        FlowRegistry registry,
        BirthdayHomeHandler homeHandler,
        BirthdayNameHandler nameHandler,
        BirthdayTextHandler textHandler
    ) {
        this.registry = registry;
        this.homeHandler = homeHandler;
        this.nameHandler = nameHandler;
        this.textHandler = textHandler;
    }

    public void register() {
        registry.registerStep(BirthdayFlowDef.FLOW, BirthdayFlowDef.STEP_HOME, homeHandler);
        registry.registerStep(BirthdayFlowDef.FLOW, BirthdayFlowDef.STEP_NAME, nameHandler);
        registry.registerStep(BirthdayFlowDef.FLOW, BirthdayFlowDef.STEP_TEXT, textHandler);

        FlowEntry home = new FlowEntry(BirthdayFlowDef.FLOW, BirthdayFlowDef.STEP_HOME);
        registry.registerCommand("birthday", home);

        // callback payloads регистрируем как команды — cbCmd добавит "/", CommandRouter снимет
        registry.registerCommand(BirthdayFlowDef.CB_WISH, home);
        registry.registerCommand(BirthdayFlowDef.CB_ANONYMOUS,
            new FlowEntry(BirthdayFlowDef.FLOW, BirthdayFlowDef.STEP_NAME));
    }
}
