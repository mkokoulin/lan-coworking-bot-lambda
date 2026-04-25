package com.lan.app.flows.donation;

import com.lan.app.engine.FlowEntry;
import com.lan.app.engine.FlowRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DonationFlowRegistrar {

        private final FlowRegistry registry;
        private final DonationHomeHandler    homeHandler;
        private final DonationContactHandler contactHandler;
        private final DonationDoneHandler    doneHandler;

        @Inject
        public DonationFlowRegistrar(
                FlowRegistry registry,
                DonationHomeHandler homeHandler,
                DonationContactHandler contactHandler,
                DonationDoneHandler doneHandler
        ) {
                this.registry = registry;
                this.homeHandler = homeHandler;
                this.contactHandler = contactHandler;
                this.doneHandler = doneHandler;
        }
 
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
