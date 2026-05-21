package com.lan.app.flows.start;

import com.lan.app.engine.FlowEntry;
import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StartFlowRegistrar {

    private final FlowRegistry registry;
    private final StartShowHandler startShowHandler;
    private final StartProfileHandler startProfileHandler;
    private final StartLogoutHandler startLogoutHandler;
    private final StartLoginHandler startLoginHandler;
    private final StartLoginPhoneHandler startLoginPhoneHandler;
    private final StartDeductConfirmHandler startDeductConfirmHandler;
    private final StartDeductDoHandler startDeductDoHandler;

    @Inject
    public StartFlowRegistrar(
        FlowRegistry registry,
        StartShowHandler startShowHandler,
        StartProfileHandler startProfileHandler,
        StartLogoutHandler startLogoutHandler,
        StartLoginHandler startLoginHandler,
        StartLoginPhoneHandler startLoginPhoneHandler,
        StartDeductConfirmHandler startDeductConfirmHandler,
        StartDeductDoHandler startDeductDoHandler
    ) {
        this.registry = registry;
        this.startShowHandler = startShowHandler;
        this.startProfileHandler = startProfileHandler;
        this.startLogoutHandler = startLogoutHandler;
        this.startLoginHandler = startLoginHandler;
        this.startLoginPhoneHandler = startLoginPhoneHandler;
        this.startDeductConfirmHandler = startDeductConfirmHandler;
        this.startDeductDoHandler = startDeductDoHandler;
    }

    public void register() {
        registry.registerStep(StartFlowDef.FLOW, StartFlowDef.STEP_SHOW, startShowHandler);
        registry.registerStep(StartFlowDef.FLOW, StartFlowDef.STEP_PROFILE, startProfileHandler);
        registry.registerStep(StartFlowDef.FLOW, StartFlowDef.STEP_LOGOUT, startLogoutHandler);
        registry.registerStep(StartFlowDef.FLOW, StartFlowDef.STEP_LOGIN, startLoginHandler);
        registry.registerStep(StartFlowDef.FLOW, StartFlowDef.STEP_LOGIN_PHONE, startLoginPhoneHandler);
        registry.registerStep(StartFlowDef.FLOW, StartFlowDef.STEP_DEDUCT_CONFIRM, startDeductConfirmHandler);
        registry.registerStep(StartFlowDef.FLOW, StartFlowDef.STEP_DEDUCT_DO, startDeductDoHandler);
        registry.registerCommand("start", new FlowEntry(StartFlowDef.FLOW, StartFlowDef.STEP_SHOW));
        registry.registerCommand("profile", new FlowEntry(StartFlowDef.FLOW, StartFlowDef.STEP_PROFILE));
        registry.registerCommand("logout", new FlowEntry(StartFlowDef.FLOW, StartFlowDef.STEP_LOGOUT));
        registry.registerCommand("login", new FlowEntry(StartFlowDef.FLOW, StartFlowDef.STEP_LOGIN));
        registry.registerCommand("deduct_confirm", new FlowEntry(StartFlowDef.FLOW, StartFlowDef.STEP_DEDUCT_CONFIRM));
        registry.registerCommand("deduct_do", new FlowEntry(StartFlowDef.FLOW, StartFlowDef.STEP_DEDUCT_DO));
    }
}
