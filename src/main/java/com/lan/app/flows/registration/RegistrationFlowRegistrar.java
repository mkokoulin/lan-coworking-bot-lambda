package com.lan.app.flows.registration;

import com.lan.app.engine.FlowEntry;
import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RegistrationFlowRegistrar {

    private final FlowRegistry registry;
    private final RegistrationStartHandler       startHandler;
    private final RegistrationWaitNameHandler    waitNameHandler;
    private final RegistrationWaitPhoneHandler   waitPhoneHandler;
    private final RegistrationVerifyPhoneHandler verifyPhoneHandler;

    @Inject
    public RegistrationFlowRegistrar(
        FlowRegistry registry,
        RegistrationStartHandler startHandler,
        RegistrationWaitNameHandler waitNameHandler,
        RegistrationWaitPhoneHandler waitPhoneHandler,
        RegistrationVerifyPhoneHandler verifyPhoneHandler
    ) {
        this.registry = registry;
        this.startHandler = startHandler;
        this.waitNameHandler = waitNameHandler;
        this.waitPhoneHandler = waitPhoneHandler;
        this.verifyPhoneHandler = verifyPhoneHandler;   
    }

    public void register() {
        registry.registerStep(RegistrationFlowDef.FLOW, RegistrationFlowDef.STEP_START,        startHandler);
        registry.registerStep(RegistrationFlowDef.FLOW, RegistrationFlowDef.STEP_WAIT_NAME,    waitNameHandler);
        registry.registerStep(RegistrationFlowDef.FLOW, RegistrationFlowDef.STEP_WAIT_PHONE,   waitPhoneHandler);
        registry.registerStep(RegistrationFlowDef.FLOW, RegistrationFlowDef.STEP_VERIFY_PHONE, verifyPhoneHandler);

        FlowEntry start = new FlowEntry(RegistrationFlowDef.FLOW, RegistrationFlowDef.STEP_START);
        registry.registerCommand("registration", start);
        registry.registerCommand("register",     start);
        registry.registerCommand("reg",          start);
    }
}
