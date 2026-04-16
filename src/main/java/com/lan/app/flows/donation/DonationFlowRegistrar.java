package com.lan.app.flows.donation;

import com.lan.app.engine.FlowEntry;
import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DonationFlowRegistrar {

    @Inject FlowRegistry registry;
    @Inject DonationHomeHandler    homeHandler;
    @Inject DonationContactHandler contactHandler;
    @Inject DonationDoneHandler    doneHandler;

    public void register() {
        registry.registerStep(DonationFlowDef.FLOW, DonationFlowDef.STEP_HOME,    homeHandler);
        registry.registerStep(DonationFlowDef.FLOW, DonationFlowDef.STEP_CONTACT, contactHandler);
        registry.registerStep(DonationFlowDef.FLOW, DonationFlowDef.STEP_DONE,    doneHandler);

        FlowEntry home = new FlowEntry(DonationFlowDef.FLOW, DonationFlowDef.STEP_HOME);
        registry.registerCommand("donation", home);
        registry.registerCommand("support",  home);

        // callback payloads регистрируем как команды — cbCmd добавит "/", CommandRouter снимет
        registry.registerCommand(DonationFlowDef.CB_HOME,
                new FlowEntry(DonationFlowDef.FLOW, DonationFlowDef.STEP_HOME));
        registry.registerCommand(DonationFlowDef.CB_CONTACT,
                new FlowEntry(DonationFlowDef.FLOW, DonationFlowDef.STEP_CONTACT));
        registry.registerCommand(DonationFlowDef.CB_DONE,
                new FlowEntry(DonationFlowDef.FLOW, DonationFlowDef.STEP_DONE));
    }
}
