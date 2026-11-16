package com.lan.app.flows.birthday;

import com.lan.app.engine.FlowEntry;
import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BirthdayFlowRegistrar {

    private final FlowRegistry registry;
    private final BirthdayHomeHandler homeHandler;
    private final BirthdayTextHandler textHandler;

    @Inject
    public BirthdayFlowRegistrar(
        FlowRegistry registry,
        BirthdayHomeHandler homeHandler,
        BirthdayTextHandler textHandler
    ) {
        this.registry = registry;
        this.homeHandler = homeHandler;
        this.textHandler = textHandler;
    }

    public void register() {
        registry.registerStep(BirthdayFlowDef.FLOW, BirthdayFlowDef.STEP_HOME, homeHandler);
        registry.registerStep(BirthdayFlowDef.FLOW, BirthdayFlowDef.STEP_TEXT, textHandler);

        FlowEntry home = new FlowEntry(BirthdayFlowDef.FLOW, BirthdayFlowDef.STEP_HOME);
        registry.registerCommand("birthday", home);

        // callback payload регистрируем как команду — cbCmd добавит "/", CommandRouter снимет
        registry.registerCommand(BirthdayFlowDef.CB_WISH, home);
    }
}
